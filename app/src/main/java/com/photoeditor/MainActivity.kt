package com.photoeditor

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.photoeditor.data.ImageExporter
import com.photoeditor.engine.BitmapEffects
import com.photoeditor.engine.document.BitmapLayerCompositor
import com.photoeditor.engine.document.BitmapOwnership
import com.photoeditor.engine.document.DocumentManager
import com.photoeditor.engine.document.DocumentSpec
import com.photoeditor.engine.document.EditorSession
import com.photoeditor.engine.document.EditorSessionHistory
import com.photoeditor.engine.document.LayerTransform
import com.photoeditor.engine.canvas.LayerHitTester
import com.photoeditor.engine.document.ImageBitmapDecoder
import com.photoeditor.ui.EditorScreen
import java.util.UUID
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PhotoEditorApp() }
    }
}

@Composable
private fun PhotoEditorApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("photoeditor_preferences", android.content.Context.MODE_PRIVATE)
    }
    var isArabic by remember {
        mutableStateOf(preferences.getString("language", "ar") != "en")
    }
    LaunchedEffect(isArabic) {
        preferences.edit().putString("language", if (isArabic) "ar" else "en").apply()
    }
    fun msg(ar: String, en: String) = if (isArabic) ar else en

    var session by remember { mutableStateOf<EditorSession?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalBitmaps by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    var adjustmentBase by remember { mutableStateOf<Bitmap?>(null) }
    var adjustmentLayerId by remember { mutableStateOf<String?>(null) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var status by remember { mutableStateOf(msg("افتح صورة للبدء في التحرير", "Open an image to start editing")) }
    val history = remember { EditorSessionHistory(12) }
    var adjustmentInProgress by remember { mutableStateOf(false) }
    var opacityAdjustmentLayerId by remember { mutableStateOf<String?>(null) }
    var layerMoveHistoryLayerId by remember { mutableStateOf<String?>(null) }

    // Keep disposal attached to the latest Compose state, not the values
    // captured when this screen was first composed.
    val latestOwnedBitmaps by rememberUpdatedState(
        buildList {
            addAll(session?.bitmapLayers?.values.orEmpty())
            addAll(originalBitmaps.values)
            adjustmentBase?.let(::add)
        }
    )

    DisposableEffect(history) {
        onDispose {
            history.clear()
            BitmapOwnership.releaseUnretained(latestOwnedBitmaps, emptyList()) { bitmap ->
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    // Each preview is a separately allocated flattened bitmap.
    DisposableEffect(previewBitmap) {
        val ownedPreview = previewBitmap
        onDispose {
            if (ownedPreview != null && !ownedPreview.isRecycled) {
                ownedPreview.recycle()
            }
        }
    }

    fun recordHistory(active: EditorSession): Boolean {
        val recorded = history.record(active)
        if (!recorded) {
            // Do not keep older snapshots across an unrecorded edit: doing so
            // would make the next Undo jump over a state the user cannot restore.
            history.clear()
            status = msg(
                "تم تنفيذ العملية، لكن لا يمكن حفظ لقطة تراجع لهذه الصورة؛ مُسح السجل القديم لمنع تراجع غير صحيح",
                "Edit applied, but this image cannot be snapshotted; older history was cleared to prevent an incorrect undo"
            )
        }
        return recorded
    }

    fun renderSession(next: EditorSession?): Boolean {
        val previous = session
        val nextPreview = try {
            next?.let { BitmapLayerCompositor.render(it.document, it.bitmapLayers) }
        } catch (_: OutOfMemoryError) {
            status = msg(
                "لا توجد ذاكرة كافية لتحديث معاينة الطبقات",
                "Not enough memory to refresh the layer preview"
            )
            null
        } catch (error: Exception) {
            status = msg(
                "تعذر تركيب الطبقات: ${error.localizedMessage ?: "خطأ"}",
                "Could not composite layers: ${error.localizedMessage ?: "Error"}"
            )
            null
        }

        // Rendering is transactional: do not replace the working session with a
        // state whose preview failed. Dispose only payloads unique to the failed
        // candidate, preserving bitmaps shared with the current session/originals.
        if (next != null && nextPreview == null) {
            BitmapOwnership.releaseUnretained(
                previous = next.bitmapLayers.values,
                retained = buildList {
                    addAll(previous?.bitmapLayers?.values.orEmpty())
                    addAll(originalBitmaps.values)
                    adjustmentBase?.let(::add)
                }
            ) { bitmap ->
                if (!bitmap.isRecycled) bitmap.recycle()
            }
            return false
        }

        session = next
        previewBitmap = nextPreview

        // Session snapshots in history are deep copies. Reclaim only payloads
        // that are no longer referenced by the next session, originals, or an
        // in-progress adjustment's immutable base image.
        if (previous != null) {
            BitmapOwnership.releaseUnretained(
                previous = previous.bitmapLayers.values,
                retained = buildList {
                    addAll(next?.bitmapLayers?.values.orEmpty())
                    addAll(originalBitmaps.values)
                    adjustmentBase?.let(::add)
                }
            ) { bitmap ->
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
        return true
    }

    fun finishAdjustment() {
        val base = adjustmentBase
        adjustmentInProgress = false
        adjustmentBase = null
        adjustmentLayerId = null
        brightness = 0f
        contrast = 1f
        saturation = 1f
        if (base != null) {
            val stillInSession = session?.bitmapLayers?.values?.any { it === base } == true
            val retainedAsOriginal = originalBitmaps.values.any { it === base }
            if (!stillInSession && !retainedAsOriginal && !base.isRecycled) {
                base.recycle()
            }
        }
    }

    fun beginAdjustment() {
        if (adjustmentInProgress) return
        val active = session ?: return
        val layerId = active.document.selectedLayerId ?: run {
            status = msg("حدد طبقة صورة أولاً", "Select an image layer first")
            return
        }
        val selectedLayer = active.document.layers.firstOrNull { it.id == layerId } ?: run {
            status = msg("الطبقة المحددة غير موجودة", "Selected layer no longer exists")
            return
        }
        if (selectedLayer.locked) {
            status = msg("الطبقة مقفلة؛ أزل القفل قبل التعديل", "Layer is locked; unlock it before editing")
            return
        }
        val bitmap = active.bitmapLayers[layerId] ?: run {
            status = msg("هذه الطبقة لا تحتوي على صورة قابلة للتعديل", "This layer has no editable bitmap")
            return
        }
        recordHistory(active)
        adjustmentBase = bitmap
        adjustmentLayerId = layerId
        adjustmentInProgress = true
    }

    fun updateAdjustment() {
        val active = session ?: return
        val layerId = adjustmentLayerId ?: active.document.selectedLayerId ?: return
        val layer = active.document.layers.firstOrNull { it.id == layerId } ?: return
        if (layer.locked) {
            status = msg("الطبقة مقفلة؛ أُلغي التعديل", "Layer is locked; adjustment cancelled")
            finishAdjustment()
            return
        }
        val base = adjustmentBase ?: active.bitmapLayers[layerId] ?: return
        val adjusted = try {
            BitmapEffects.adjust(base, brightness, contrast, saturation)
        } catch (_: OutOfMemoryError) {
            status = msg("لا توجد ذاكرة كافية لمعاينة التعديلات", "Not enough memory to preview adjustments")
            return
        } catch (error: Exception) {
            status = msg(
                "فشل تعديل الصورة: ${error.localizedMessage ?: "خطأ"}",
            "Adjustment failed: ${error.localizedMessage ?: "Error"}"
            )
            return
        }
        if (renderSession(active.replaceBitmapPayload(layerId, adjusted))) {
            status = msg("معاينة تعديلات الطبقة المحددة", "Previewing selected-layer adjustments")
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var importedBitmap: Bitmap? = null
            var originalCopy: Bitmap? = null
            var ownershipTransferred = false
            try {
                importedBitmap = ImageBitmapDecoder.decode(context.contentResolver, uri)
                val safeBitmap = importedBitmap
                if (safeBitmap == null) {
                    status = msg("تعذر قراءة الصورة أو أن أبعادها غير صالحة", "Could not decode the selected image or its dimensions are invalid")
                } else {
                    val active = session
                    val layerId = UUID.randomUUID().toString()
                    val originalSnapshot = safeBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        ?: error("Could not create the original-image snapshot")
                    originalCopy = originalSnapshot

                    if (active == null) {
                        val first = EditorSession.fromBitmap(
                            safeBitmap,
                            name = "Background",
                            id = layerId
                        )
                        history.clear()
                        finishAdjustment()
                        renderSession(first)
                        originalBitmaps = mapOf(layerId to originalSnapshot)
                        originalCopy = null
                        ownershipTransferred = true
                        status = msg(
                            "تم إنشاء المستند وفتح الصورة كطبقة خلفية",
                            "Document created with the image as its background layer"
                        )
                    } else {
                        recordHistory(active)
                        val scale = min(
                            1f,
                            min(
                                active.document.width.toFloat() / safeBitmap.width,
                                active.document.height.toFloat() / safeBitmap.height
                            )
                        )
                        val x = (active.document.width - safeBitmap.width * scale) / 2f
                        val y = (active.document.height - safeBitmap.height * scale) / 2f
                        val next = active.addBitmapLayer(
                            bitmap = safeBitmap,
                            name = "Image ${active.document.layers.size + 1}",
                            id = layerId,
                            transform = LayerTransform(
                                x = x,
                                y = y,
                                scaleX = scale,
                                scaleY = scale
                            )
                        )
                        finishAdjustment()
                        renderSession(next)
                        originalBitmaps = originalBitmaps + (layerId to originalSnapshot)
                        originalCopy = null
                        ownershipTransferred = true
                        status = msg(
                            "تمت إضافة الصورة كطبقة مستقلة",
                            "Image added as an independent layer"
                        )
                    }
                }
            } catch (error: OutOfMemoryError) {
                status = msg(
                    "لا توجد ذاكرة كافية لفتح الصورة؛ جرّب صورة أصغر",
                    "Not enough memory to open this image; try a smaller image"
                )
            } catch (error: Exception) {
                status = msg(
                    "تعذر فتح الصورة: ${error.localizedMessage ?: "خطأ غير معروف"}",
                    "Could not open image: ${error.localizedMessage ?: "Unknown error"}"
                )
            } finally {
                originalCopy?.let { if (!it.isRecycled) it.recycle() }
                if (!ownershipTransferred) {
                    importedBitmap?.let { if (!it.isRecycled) it.recycle() }
                }
            }
        }
    }

    fun applyEffect(effect: (Bitmap) -> Bitmap) {
        finishAdjustment()
        val active = session ?: run {
            status = msg("افتح صورة أولاً", "Open an image first")
            return
        }
        val layerId = active.document.selectedLayerId ?: run {
            status = msg("حدد طبقة صورة أولاً", "Select an image layer first")
            return
        }
        val layer = active.document.layers.firstOrNull { it.id == layerId } ?: return
        if (layer.locked) {
            status = msg("الطبقة مقفلة؛ أزل القفل قبل تطبيق التأثير", "Layer is locked; unlock it before applying effects")
            return
        }
        val source = active.bitmapLayers[layerId] ?: run {
            status = msg("الطبقة المحددة ليست طبقة صورة قابلة للتأثير", "Selected layer is not an editable image layer")
            return
        }

        // Generate the candidate before recording history; failed effects must not
        // create a phantom undo entry or disturb the current session.
        val effected = try {
            effect(source)
        } catch (_: OutOfMemoryError) {
            status = msg("لا توجد ذاكرة كافية لتنفيذ التأثير", "Not enough memory to apply the effect")
            return
        } catch (error: Exception) {
            status = msg(
                "فشل التأثير: ${error.localizedMessage ?: "خطأ"}",
                "Effect failed: ${error.localizedMessage ?: "Error"}"
            )
            return
        }
        if (effected === source || effected.isRecycled) {
            status = msg("أعاد التأثير صورة غير صالحة", "Effect returned an invalid bitmap")
            return
        }

        recordHistory(active)
        if (renderSession(active.replaceBitmapPayload(layerId, effected))) {
            status = msg("تم تطبيق التأثير على الطبقة المحددة", "Effect applied to selected layer")
        }
    }

    fun undo() {
        finishAdjustment()
        val active = session ?: return
        val previous = history.undo(active)
        if (previous != null) {
            renderSession(previous)
            status = if (history.lastTransitionSkipped) {
                msg("تم التراجع، لكن تعذر الاحتفاظ بنسخة للإعادة بسبب حد الذاكرة؛ مُسح سجل الإعادة", "Undo completed, but redo could not be retained within the memory budget; redo history was cleared")
            } else msg("تم التراجع", "Undo completed")
        }
    }

    fun redo() {
        finishAdjustment()
        val active = session ?: return
        val next = history.redo(active)
        if (next != null) {
            renderSession(next)
            status = if (history.lastTransitionSkipped) {
                msg("تمت الإعادة، لكن تعذر الاحتفاظ بنسخة للتراجع بسبب حد الذاكرة؛ مُسح سجل التراجع", "Redo completed, but undo could not be retained within the memory budget; undo history was cleared")
            } else msg("تمت إعادة العملية", "Redo completed")
        }
    }

    fun resetImage() {
        finishAdjustment()
        val active = session ?: return
        val layerId = active.document.selectedLayerId ?: return
        val original = originalBitmaps[layerId] ?: return
        val restored = try {
            original.copy(Bitmap.Config.ARGB_8888, false)
        } catch (_: OutOfMemoryError) {
            null
        } ?: run {
            status = msg("لا توجد ذاكرة كافية لاستعادة الصورة الأصلية", "Not enough memory to restore the original image")
            return
        }
        recordHistory(active)
        if (renderSession(active.replaceBitmapPayload(layerId, restored))) {
            status = msg("تمت استعادة الصورة الأصلية للطبقة", "Selected layer restored to its original image")
        }
    }

    fun updateLayer(id: String, change: (com.photoeditor.engine.document.EditorDocument) -> com.photoeditor.engine.document.EditorDocument) {
        val active = session ?: return
        renderSession(active.updateDocument(change))
    }

    val selectedLayerForEditing = session?.document?.let { document ->
        document.layers.firstOrNull { it.id == document.selectedLayerId }
    }
    val canAdjustSelectedLayer = selectedLayerForEditing?.let { layer ->
        layer.kind == com.photoeditor.engine.document.LayerKind.BITMAP &&
            !layer.locked &&
            session?.bitmapLayers?.containsKey(layer.id) == true
    } == true

    EditorScreen(
        bitmap = previewBitmap,
        canAdjustSelectedLayer = canAdjustSelectedLayer,
        status = status,
        canUndo = history.canUndo,
        canRedo = history.canRedo,
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        isArabic = isArabic,
        layers = session?.document?.layers.orEmpty(),
        selectedLayerId = session?.document?.selectedLayerId,
        canMoveSelectedLayer = canAdjustSelectedLayer,
        selectedLayerBitmapSize = session?.document?.selectedLayerId?.let { id ->
            session?.bitmapLayers?.get(id)?.let { it.width to it.height }
        },
        onLayerMoveStart = { id ->
            val active = session
            val layer = active?.document?.layers?.firstOrNull { it.id == id }
            if (active != null && layer != null && !layer.locked && active.bitmapLayers.containsKey(id)) {
                finishAdjustment()
                recordHistory(active)
                layerMoveHistoryLayerId = id
            }
        },
        onLayerMoveBy = { id, deltaX, deltaY ->
            val active = session
            if (active != null && layerMoveHistoryLayerId == id) {
                val layer = active.document.layers.firstOrNull { it.id == id }
                if (layer != null && !layer.locked) {
                    updateLayer(id) { document ->
                        document.updateLayer(id) { current ->
                            current.copy(transform = current.transform.copy(
                                x = current.transform.x + deltaX,
                                y = current.transform.y + deltaY
                            ))
                        }
                    }
                }
            }
        },
        onLayerMoveFinish = {
            layerMoveHistoryLayerId = null
            status = msg("تم تحريك الطبقة المحددة", "Selected layer moved")
        },
        onLayerTransformStart = { id ->
            if (layerMoveHistoryLayerId != id) {
                val active = session
                val layer = active?.document?.layers?.firstOrNull { it.id == id }
                if (active != null && layer != null && !layer.locked && active.bitmapLayers.containsKey(id)) {
                    finishAdjustment()
                    recordHistory(active)
                    layerMoveHistoryLayerId = id
                }
            }
        },
        onLayerTransformChange = { id, transform ->
            val active = session
            if (active != null && layerMoveHistoryLayerId == id) {
                updateLayer(id) { document ->
                    document.updateLayer(id) { current -> current.copy(transform = transform) }
                }
            }
        },
        onLayerTransformFinish = { layerMoveHistoryLayerId = null },
        onCanvasSelectLayer = { x, y ->
            val active = session
            if (active != null) {
                val sizes = active.bitmapLayers.mapValues { (_, layerBitmap) ->
                    layerBitmap.width to layerBitmap.height
                }
                val hitId = LayerHitTester.findTopmost(active.document, sizes, x, y)
                if (hitId != null && hitId != active.document.selectedLayerId) {
                    finishAdjustment()
                    updateLayer(hitId) { it.selectLayer(hitId) }
                    status = msg("تم تحديد الطبقة من مساحة العمل", "Layer selected from canvas")
                }
                val hitLayer = active.document.layers.firstOrNull { it.id == hitId }
                if (hitLayer != null && !hitLayer.locked && active.bitmapLayers.containsKey(hitId)) hitId else null
            } else null
        },
        onToggleLanguage = { isArabic = !isArabic },
        onOpen = { imagePicker.launch("image/*") },
        onCreateDocument = { spec ->
            finishAdjustment()
            history.clear()
            opacityAdjustmentLayerId = null
            val oldOriginals = originalBitmaps.values.toList()
            originalBitmaps = emptyMap()
            renderSession(DocumentManager.create(spec))
            BitmapOwnership.releaseUnretained(oldOriginals, emptyList()) { bitmap ->
                if (!bitmap.isRecycled) bitmap.recycle()
            }
            status = msg("تم إنشاء مستند جديد بنجاح", "New document created successfully")
        },
        onSave = {
            finishAdjustment()
            val bitmap = previewBitmap
            if (bitmap != null) {
                val result = ImageExporter.savePng(context, bitmap)
                status = result.getOrElse {
                    msg("فشل الحفظ: ${it.localizedMessage ?: "خطأ"}", "Save failed: ${it.localizedMessage ?: "Error"}")
                }
            }
        },
        onUndo = { undo() },
        onRedo = { redo() },
        onReset = { resetImage() },
        onGrayscale = { applyEffect { BitmapEffects.grayscale(it) } },
        onRotate = { applyEffect { BitmapEffects.rotate(it, 90f) } },
        onBrightnessChange = { brightness = it; updateAdjustment() },
        onContrastChange = { contrast = it; updateAdjustment() },
        onSaturationChange = { saturation = it; updateAdjustment() },
        onAdjustmentStart = { beginAdjustment() },
        onAdjustmentFinish = { finishAdjustment(); status = msg("تم تثبيت التعديلات", "Adjustments committed") },
        onSelectLayer = { id ->
            finishAdjustment()
            updateLayer(id) { it.selectLayer(id) }
        },
        onToggleLayerVisibility = { id, visible ->
            recordHistory(session ?: return@EditorScreen)
            updateLayer(id) { it.setLayerVisibility(id, visible) }
            status = msg("تم تحديث ظهور الطبقة", "Layer visibility updated")
        },
        onToggleLayerLock = { id, locked ->
            val active = session ?: return@EditorScreen
            finishAdjustment()
            recordHistory(active)
            updateLayer(id) { it.setLayerLocked(id, locked) }
            status = if (locked) {
                msg("تم قفل الطبقة؛ لن تقبل تعديلات الصورة", "Layer locked; image edits are disabled")
            } else {
                msg("تم فتح قفل الطبقة", "Layer unlocked")
            }
        },
        onLayerOpacityChange = { id, opacity ->
            if (!adjustmentInProgress) {
                val active = session
                val target = active?.document?.layers?.firstOrNull { it.id == id }
                when {
                    active == null || target == null -> Unit
                    target.locked -> {
                        opacityAdjustmentLayerId = null
                        status = msg("الطبقة مقفلة؛ أزل القفل قبل تغيير العتامة", "Layer is locked; unlock it before changing opacity")
                    }
                    else -> {
                        if (opacityAdjustmentLayerId != id) {
                            recordHistory(active)
                            opacityAdjustmentLayerId = id
                        }
                        updateLayer(id) { it.setLayerOpacity(id, opacity) }
                    }
                }
            }
        },
        onLayerOpacityFinish = { opacityAdjustmentLayerId = null },
        onMoveLayer = { id, direction ->
            val active = session
            if (active != null) {
                val index = active.document.layers.indexOfFirst { it.id == id }
                val target = index + direction
                if (index >= 0 && target in active.document.layers.indices) {
                    recordHistory(active)
                    updateLayer(id) { it.moveLayer(index, target) }
                    status = msg("تم تغيير ترتيب الطبقة", "Layer order updated")
                }
            }
        },
        onDeleteLayer = { id ->
            val active = session
            if (active != null) {
                recordHistory(active)
                finishAdjustment()
                renderSession(active.removeLayer(id))
                val removedOriginal = originalBitmaps[id]
                originalBitmaps = originalBitmaps - id
                if (removedOriginal != null && !removedOriginal.isRecycled) removedOriginal.recycle()
                status = msg("تم حذف الطبقة", "Layer deleted")
            }
        }
    )
}
