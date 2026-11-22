package com.photoeditor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.photoeditor.data.ImageExporter
import com.photoeditor.engine.BitmapEffects
import com.photoeditor.engine.EditorHistory
import com.photoeditor.engine.document.EditorDocument
import com.photoeditor.engine.document.EditorLayer
import com.photoeditor.engine.document.LayerKind
import java.util.UUID
import com.photoeditor.ui.EditorScreen

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

    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var document by remember { mutableStateOf<EditorDocument?>(null) }
    var importedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var adjustmentBase by remember { mutableStateOf<Bitmap?>(null) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var status by remember { mutableStateOf(msg("افتح صورة للبدء في التحرير", "Open an image to start editing")) }
    val history = remember { EditorHistory(20) }
    var adjustmentInProgress by remember { mutableStateOf(false) }

    fun finishAdjustment() {
        adjustmentInProgress = false
        adjustmentBase = null
        brightness = 0f
        contrast = 1f
        saturation = 1f
    }

    fun beginAdjustment() {
        if (!adjustmentInProgress) {
            val bitmap = currentBitmap ?: return
            history.record(bitmap)
            adjustmentBase = bitmap
            adjustmentInProgress = true
        }
    }

    fun updateAdjustment() {
        val base = adjustmentBase ?: currentBitmap ?: return
        currentBitmap = BitmapEffects.adjust(base, brightness, contrast, saturation)
        status = msg("معاينة التعديلات", "Previewing adjustments")
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) {
                        val safeBitmap = decoded.copy(Bitmap.Config.ARGB_8888, false)
                        importedBitmap = safeBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        currentBitmap = safeBitmap
                        document = EditorDocument(safeBitmap.width, safeBitmap.height).addLayer(
                            EditorLayer(UUID.randomUUID().toString(), msg("الصورة الأصلية", "Original image"), LayerKind.BITMAP)
                        )
                        history.clear()
                        finishAdjustment()
                        status = msg("تم فتح الصورة بنجاح", "Image opened successfully")
                    } else {
                        status = msg("تعذر قراءة الصورة المختارة", "Could not read the selected image")
                    }
                }
            } catch (error: Exception) {
                status = msg(
                    "تعذر فتح الصورة: ${error.localizedMessage ?: "خطأ غير معروف"}",
                    "Could not open image: ${error.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun applyEffect(effect: () -> Bitmap) {
        finishAdjustment()
        val source = currentBitmap ?: run {
            status = msg("افتح صورة أولاً", "Open an image first")
            return
        }
        history.record(source)
        currentBitmap = effect()
        status = msg("تم تطبيق التأثير", "Effect applied")
    }

    fun undo() {
        finishAdjustment()
        val source = currentBitmap ?: return
        val previous = history.undo(source)
        if (previous != null) {
            currentBitmap = previous
            status = msg("تم التراجع", "Undo completed")
        }
    }

    fun redo() {
        finishAdjustment()
        val source = currentBitmap ?: return
        val next = history.redo(source)
        if (next != null) {
            currentBitmap = next
            status = msg("تمت إعادة العملية", "Redo completed")
        }
    }

    fun resetImage() {
        finishAdjustment()
        val source = currentBitmap ?: return
        val original = importedBitmap ?: return
        history.record(source)
        currentBitmap = original.copy(Bitmap.Config.ARGB_8888, false)
        status = msg("تمت استعادة الصورة الأصلية", "Original image restored")
    }

    EditorScreen(
        bitmap = currentBitmap,
        layers = document?.layers ?: emptyList(),
        selectedLayerId = document?.selectedLayerId,
        onSelectLayer = { id -> document = document?.selectLayer(id) },
        onSetLayerVisibility = { id, visible -> document = document?.setLayerVisibility(id, visible) },
        status = status,
        canUndo = history.canUndo,
        canRedo = history.canRedo,
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        isArabic = isArabic,
        onToggleLanguage = { isArabic = !isArabic },
        onOpen = { imagePicker.launch("image/*") },
        onSave = {
            finishAdjustment()
            val bitmap = currentBitmap
            if (bitmap != null && document?.layers?.firstOrNull()?.visible != false) {
                val result = ImageExporter.savePng(context, bitmap)
                status = result.getOrElse {
                    msg("فشل الحفظ: ${it.localizedMessage ?: "خطأ"}", "Save failed: ${it.localizedMessage ?: "Error"}")
                }
            } else if (bitmap != null) {
                status = msg("أظهر طبقة الصورة قبل التصدير", "Show the image layer before exporting")
            }
        },
        onUndo = { undo() },
        onRedo = { redo() },
        onReset = { resetImage() },
        onGrayscale = { applyEffect { BitmapEffects.grayscale(currentBitmap!!) } },
        onRotate = { applyEffect { BitmapEffects.rotate(currentBitmap!!, 90f) } },
        onBrightnessChange = { brightness = it; updateAdjustment() },
        onContrastChange = { contrast = it; updateAdjustment() },
        onSaturationChange = { saturation = it; updateAdjustment() },
        onAdjustmentStart = { beginAdjustment() },
        onAdjustmentFinish = { finishAdjustment(); status = msg("تم تثبيت التعديلات", "Adjustments committed") }
    )
}
