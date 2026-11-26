package com.photoeditor.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.abs
import kotlin.math.sign
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.photoeditor.engine.document.EditorLayer
import com.photoeditor.engine.document.LayerTransform
import com.photoeditor.engine.document.DocumentPreset
import com.photoeditor.engine.document.DocumentSizeUnit
import com.photoeditor.engine.document.DocumentSpec
import com.photoeditor.engine.canvas.CanvasViewport
import com.photoeditor.engine.canvas.CanvasCoordinateMapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val AppBg = Color(0xFF17191E)
private val PanelBg = Color(0xFF22252C)
private val PanelEdge = Color(0xFF353A44)
private val Accent = Color(0xFF2685E8)
private val MainText = Color(0xFFE7EAF0)
private val MutedText = Color(0xFFAAB2C0)

private fun centerForCanvas(value: Float): Float = value / 2f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    bitmap: Bitmap?,
    imageDocumentPreview: Bitmap?,
    onAddImageDocumentToCanvas: () -> Unit,
    onCloseImageDocument: () -> Unit,
    canAdjustSelectedLayer: Boolean,
    status: String,
    canUndo: Boolean,
    canRedo: Boolean,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    isArabic: Boolean,
    layers: List<EditorLayer>,
    selectedLayerId: String?,
    canMoveSelectedLayer: Boolean,
    selectedLayerBitmapSize: Pair<Int, Int>?,
    onLayerMoveStart: (String) -> Unit,
    onLayerMoveBy: (String, Float, Float) -> Unit,
    onLayerMoveFinish: () -> Unit,
    onLayerTransformStart: (String) -> Unit,
    onLayerTransformChange: (String, LayerTransform) -> Unit,
    onLayerTransformFinish: () -> Unit,
    onCanvasSelectLayer: (Float, Float) -> String?,
    onToggleLanguage: () -> Unit,
    onOpen: () -> Unit,
    onCreateDocument: (DocumentSpec) -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onGrayscale: () -> Unit,
    onRotate: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onAdjustmentStart: () -> Unit,
    onAdjustmentFinish: () -> Unit,
    onSelectLayer: (String) -> Unit,
    onToggleLayerVisibility: (String, Boolean) -> Unit,
    onToggleLayerLock: (String, Boolean) -> Unit,
    onLayerOpacityChange: (String, Float) -> Unit,
    onLayerOpacityFinish: () -> Unit,
    onMoveLayer: (String, Int) -> Unit,
    onDeleteLayer: (String) -> Unit
) {
    val direction = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    var selectedTool by remember { mutableStateOf("Move") }
    var showLayers by remember { mutableStateOf(true) }
    var showProperties by remember { mutableStateOf(true) }
    var showColorPanel by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showShapes by remember { mutableStateOf(false) }
    var showBrushes by remember { mutableStateOf(false) }
    var transformControlsLayerId by remember { mutableStateOf<String?>(null) }
    var imageDocumentZoom by remember(imageDocumentPreview) { mutableFloatStateOf(1f) }
    var activeMenu by remember { mutableStateOf<String?>(null) }
    var showNewDocumentDialog by remember { mutableStateOf(false) }
    var presetMenuExpanded by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(DocumentPreset.SQUARE_1080) }
    var widthText by remember { mutableStateOf("1080") }
    var heightText by remember { mutableStateOf("1080") }
    var dpiText by remember { mutableStateOf("300") }
    var sizeUnit by remember { mutableStateOf(DocumentSizeUnit.PIXELS) }
    var backgroundArgb by remember { mutableStateOf(0xFFFFFFFF.toInt()) }
    var documentError by remember { mutableStateOf<String?>(null) }
    var viewport by remember { mutableStateOf(CanvasViewport()) }
    val latestViewport by rememberUpdatedState(viewport)

    LaunchedEffect(bitmap?.width, bitmap?.height) {
        viewport = CanvasViewport()
    }

    fun label(ar: String, en: String) = if (isArabic) ar else en

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Surface(Modifier.fillMaxSize(), color = AppBg) {
            Column(Modifier.fillMaxSize()) {
                // Application title and compact desktop-inspired menu bar.
                Row(
                    Modifier.fillMaxWidth().height(34.dp).background(Color(0xFF111318)).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PhotoEditor", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Text(if (layers.isEmpty()) "Untitled-1" else "Image-1", color = MutedText, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onToggleLanguage, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)) {
                        Text(if (isArabic) "EN" else "ع", color = MainText)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(34.dp).background(PanelBg).horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("File", "Edit", "Image", "Layer", "Type", "Select", "Filter", "View", "Window", "Help").forEach { menu ->
                        Box {
                            TextButton(
                                onClick = { activeMenu = if (activeMenu == menu) null else menu },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) { Text(label(
                                when(menu) {
                                    "File" -> "ملف"; "Edit" -> "تحرير"; "Image" -> "صورة"; "Layer" -> "طبقة"
                                    "Type" -> "نص"; "Select" -> "تحديد"; "Filter" -> "مرشح"; "View" -> "عرض"
                                    "Window" -> "نافذة"; else -> "مساعدة"
                                }, menu), color = MainText, style = MaterialTheme.typography.labelMedium) }
                            DropdownMenu(expanded = activeMenu == menu, onDismissRequest = { activeMenu = null }) {
                                when (menu) {
                                    "File" -> {
                                        DropdownMenuItem(text = { Text(label("مستند جديد", "New Document")) }, onClick = {
                                            activeMenu = null
                                            selectedPreset = DocumentPreset.SQUARE_1080
                                            widthText = "1080"
                                            heightText = "1080"
                                            dpiText = "300"
                                            sizeUnit = DocumentSizeUnit.PIXELS
                                            backgroundArgb = 0xFFFFFFFF.toInt()
                                            documentError = null
                                            showNewDocumentDialog = true
                                        })
                                        DropdownMenuItem(text = { Text(label("فتح صورة", "Open Image")) }, onClick = { activeMenu = null; onOpen() })
                                        DropdownMenuItem(text = { Text(label("تصدير PNG", "Export PNG")) }, enabled = bitmap != null, onClick = { activeMenu = null; onSave() })
                                    }
                                    "Edit" -> {
                                        DropdownMenuItem(text = { Text(label("تراجع", "Undo")) }, enabled = canUndo, onClick = { activeMenu = null; onUndo() })
                                        DropdownMenuItem(text = { Text(label("إعادة", "Redo")) }, enabled = canRedo, onClick = { activeMenu = null; onRedo() })
                                    }
                                    "Image" -> {
                                        DropdownMenuItem(text = { Text(label("أبيض وأسود", "Grayscale")) }, enabled = canAdjustSelectedLayer, onClick = { activeMenu = null; onGrayscale() })
                                        DropdownMenuItem(text = { Text(label("تدوير 90°", "Rotate 90°")) }, enabled = canAdjustSelectedLayer, onClick = { activeMenu = null; onRotate() })
                                        DropdownMenuItem(text = { Text(label("استعادة الأصل", "Restore Original")) }, enabled = canAdjustSelectedLayer, onClick = { activeMenu = null; onReset() })
                                    }
                                    "Layer" -> {
                                        DropdownMenuItem(text = { Text(label("حذف الطبقة المحددة", "Delete selected layer")) }, enabled = selectedLayerId != null, onClick = { activeMenu = null; selectedLayerId?.let(onDeleteLayer) })
                                    }
                                    "Window" -> {
                                        DropdownMenuItem(text = { Text(label("الطبقات", "Layers") + if (showLayers) " ✓" else "") }, onClick = { showLayers = !showLayers; activeMenu = null })
                                        DropdownMenuItem(text = { Text(label("الخصائص", "Properties") + if (showProperties) " ✓" else "") }, onClick = { showProperties = !showProperties; activeMenu = null })
                                        DropdownMenuItem(text = { Text(label("الألوان", "Colors") + if (showColorPanel) " ✓" else "") }, onClick = { showColorPanel = !showColorPanel; activeMenu = null })
                                        DropdownMenuItem(text = { Text(label("السجل", "History") + if (showHistory) " ✓" else "") }, onClick = { showHistory = !showHistory; activeMenu = null })
                                        DropdownMenuItem(text = { Text(label("الأشكال", "Shapes") + if (showShapes) " ✓" else "") }, onClick = { showShapes = !showShapes; activeMenu = null })
                                        DropdownMenuItem(text = { Text(label("الفرش", "Brushes") + if (showBrushes) " ✓" else "") }, onClick = { showBrushes = !showBrushes; activeMenu = null })
                                    }
                                    else -> DropdownMenuItem(text = { Text(label("سيتم تفعيل أدوات هذه القائمة ضمن مراحل التنفيذ", "Tools in this menu are scheduled for implementation")) }, onClick = { activeMenu = null })
                                }
                            }
                        }
                    }
                }
                // Contextual tool options strip.
                Row(
                    Modifier.fillMaxWidth().height(36.dp).background(Color(0xFF292D35)).padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label("الأداة:", "Tool:"), color = MutedText, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(6.dp))
                    Text(selectedTool, color = Color(0xFF8FC5FF), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { selectedTool = "Move" },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTool == "Move") Accent else Color(0xFF3A414D),
                            contentColor = Color.White
                        )
                    ) {
                        Text(label("تحريك الطبقة", "Move Layer"), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.weight(1f))
                }
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    // Left tool rail.
                    Column(
                        Modifier.width(48.dp).fillMaxHeight().background(Color(0xFF202329)).padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val tools = listOf(
                            "Move" to "تحريك", "Select" to "تحديد", "Crop" to "قص",
                            "Brush" to "فرشاة", "Eraser" to "ممحاة", "Text" to "نص",
                            "Shape" to "شكل", "Gradient" to "تدرج", "Hand" to "يد", "Zoom" to "تكبير"
                        )
                        tools.forEach { (tool, ar) ->
                            val chosen = selectedTool == tool
                            TextButton(
                                onClick = { selectedTool = tool },
                                modifier = Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 3.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (chosen) Accent.copy(alpha = 0.30f) else Color.Transparent,
                                    contentColor = if (chosen) Color.White else MutedText
                                )
                            ) { Text(tool.take(2), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                        }
                    }
                    // Central canvas stage.
                    Box(
                        Modifier.weight(1f).fillMaxHeight().background(Color(0xFF30343C)).padding(10.dp).clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap == null) {
                            Spacer(Modifier.fillMaxSize())
                        } else {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = label("معاينة مساحة العمل", "Canvas preview"),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = viewport.scale
                                        scaleY = viewport.scale
                                        translationX = viewport.offsetX
                                        translationY = viewport.offsetY
                                    }
                                    .pointerInput(selectedTool, selectedLayerId, canMoveSelectedLayer, bitmap?.width, bitmap?.height) {
                                        if (selectedTool == "Move") {
                                            var movingLayerId: String? = null
                                            detectDragGestures(
                                                onDragStart = { start ->
                                                    val documentWidth = bitmap?.width ?: 0
                                                    val documentHeight = bitmap?.height ?: 0
                                                    val documentPoint = if (documentWidth > 0 && documentHeight > 0) {
                                                        CanvasCoordinateMapper.screenToDocument(
                                                            canvasWidth = size.width.toFloat(),
                                                            canvasHeight = size.height.toFloat(),
                                                            documentWidth = documentWidth,
                                                            documentHeight = documentHeight,
                                                            screenX = start.x,
                                                            screenY = start.y,
                                                            viewportScale = latestViewport.scale,
                                                            viewportOffsetX = latestViewport.offsetX,
                                                            viewportOffsetY = latestViewport.offsetY
                                                        )
                                                    } else null
                                                    movingLayerId = documentPoint?.let { point ->
                                                        onCanvasSelectLayer(point.first, point.second)
                                                    }
                                                    movingLayerId?.let(onLayerMoveStart)
                                                },
                                                onDragEnd = {
                                                    if (movingLayerId != null) onLayerMoveFinish()
                                                    movingLayerId = null
                                                },
                                                onDragCancel = {
                                                    if (movingLayerId != null) onLayerMoveFinish()
                                                    movingLayerId = null
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                val layerId = movingLayerId
                                                if (layerId != null) {
                                                    val documentWidth = bitmap?.width ?: return@detectDragGestures
                                                    val documentHeight = bitmap?.height ?: return@detectDragGestures
                                                    val fitScale = minOf(
                                                        size.width / documentWidth.toFloat(),
                                                        size.height / documentHeight.toFloat()
                                                    )
                                                    val divisor = fitScale * latestViewport.scale
                                                    if (divisor > 0f) {
                                                        onLayerMoveBy(layerId, dragAmount.x / divisor, dragAmount.y / divisor)
                                                    }
                                                } else {
                                                    viewport = latestViewport.panBy(dragAmount.x, dragAmount.y)
                                                }
                                            }
                                        } else {
                                            detectTransformGestures { centroid, pan, zoom, _ ->
                                                val centerX = size.width / 2f
                                                val centerY = size.height / 2f
                                                viewport = latestViewport.transform(
                                                    zoomFactor = zoom,
                                                    panX = pan.x,
                                                    panY = pan.y,
                                                    pivotX = centroid.x - centerX,
                                                    pivotY = centroid.y - centerY
                                                )
                                            }
                                        }
                                    }
                                    .pointerInput(bitmap?.width, bitmap?.height, viewport) {
                                        detectTapGestures { position ->
                                            val documentWidth = bitmap?.width ?: return@detectTapGestures
                                            val documentHeight = bitmap?.height ?: return@detectTapGestures
                                            val documentPoint = CanvasCoordinateMapper.screenToDocument(
                                                canvasWidth = size.width.toFloat(),
                                                canvasHeight = size.height.toFloat(),
                                                documentWidth = documentWidth,
                                                documentHeight = documentHeight,
                                                screenX = position.x,
                                                screenY = position.y,
                                                viewportScale = viewport.scale,
                                                viewportOffsetX = viewport.offsetX,
                                                viewportOffsetY = viewport.offsetY
                                            ) ?: return@detectTapGestures
                                            onCanvasSelectLayer(documentPoint.first, documentPoint.second)
                                        }
                                    }
                                    .border(1.dp, Color(0xFF596170)),
                                contentScale = ContentScale.Fit
                            )
                            val selectedLayer = layers.firstOrNull { it.id == selectedLayerId }
                            val selectedSize = selectedLayerBitmapSize
                            if (selectedLayer != null && selectedSize != null && selectedLayer.visible) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val docWidth = bitmap.width.toFloat()
                                    val docHeight = bitmap.height.toFloat()
                                    val fitScale = minOf(size.width / docWidth, size.height / docHeight)
                                    val transform = selectedLayer.transform
                                    val radians = Math.toRadians(transform.rotationDegrees.toDouble())
                                    val cosAngle = kotlin.math.cos(radians).toFloat()
                                    val sinAngle = kotlin.math.sin(radians).toFloat()
                                    fun toScreen(localX: Float, localY: Float): Offset {
                                        val scaledX = localX * transform.scaleX
                                        val scaledY = localY * transform.scaleY
                                        val docX = transform.x + scaledX * cosAngle - scaledY * sinAngle
                                        val docY = transform.y + scaledX * sinAngle + scaledY * cosAngle
                                        val fitX = (size.width - docWidth * fitScale) / 2f + docX * fitScale
                                        val fitY = (size.height - docHeight * fitScale) / 2f + docY * fitScale
                                        return Offset(
                                            size.width / 2f + (fitX - size.width / 2f) * viewport.scale + viewport.offsetX,
                                            size.height / 2f + (fitY - size.height / 2f) * viewport.scale + viewport.offsetY
                                        )
                                    }
                                    val corners = listOf(
                                        toScreen(0f, 0f),
                                        toScreen(selectedSize.first.toFloat(), 0f),
                                        toScreen(selectedSize.first.toFloat(), selectedSize.second.toFloat()),
                                        toScreen(0f, selectedSize.second.toFloat())
                                    )
                                    corners.indices.forEach { index ->
                                        drawLine(Color(0xFF70C3FF), corners[index], corners[(index + 1) % corners.size], strokeWidth = 2.dp.toPx())
                                        drawCircle(Color.White, radius = 5.dp.toPx(), center = corners[index])
                                        drawCircle(Color(0xFF167BD1), radius = 5.dp.toPx(), center = corners[index], style = Stroke(width = 1.5.dp.toPx()))
                                    }
                                }
                            }
                            Row(
                                Modifier.align(Alignment.TopStart)
                                    .padding(start = 6.dp)
                                    .background(Color(0xDD17191E), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewport = viewport.zoomBy(0.8f) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) { Text("−", color = MainText) }
                                Text(
                                    "${(viewport.scale * 100).toInt()}%",
                                    color = MainText,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                TextButton(
                                    onClick = { viewport = viewport.zoomBy(1.25f) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) { Text("+", color = MainText) }
                                TextButton(
                                    onClick = { viewport = viewport.reset() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) { Text(label("ملاءمة", "Fit"), color = Color(0xFF8FC5FF), style = MaterialTheme.typography.labelSmall) }
                            }
                        Box(Modifier.fillMaxSize()) {
                    // Floating workspace panels overlay the canvas.
                    if (showLayers || showProperties || showColorPanel || showHistory || showShapes || showBrushes) {
                        Box(Modifier.fillMaxSize()) {
                            if (showLayers) {
                                PanelCard(label("الطبقات", "Layers")) {
                                    if (layers.isEmpty()) {
                                        Text(label("لا توجد طبقات بعد", "No layers yet"), color = MutedText, style = MaterialTheme.typography.labelSmall)
                                    } else {
                                        Column(
                                            Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            layers.asReversed().forEach { layer ->
                                                val layerIndex = layers.indexOfFirst { it.id == layer.id }
                                                val selected = layer.id == selectedLayerId
                                                Column(
                                                    Modifier.fillMaxWidth()
                                                        .combinedClickable(
                                                            onClick = { onSelectLayer(layer.id) },
                                                            onDoubleClick = {
                                                                onSelectLayer(layer.id)
                                                                transformControlsLayerId = if (transformControlsLayerId == layer.id) null else layer.id
                                                            }
                                                        )
                                                        .pointerInput(layer.id) {
                                                            var accumulatedDrag = 0f
                                                            val reorderThreshold = 48.dp.toPx()
                                                            detectDragGestures(
                                                                onDragEnd = { accumulatedDrag = 0f },
                                                                onDragCancel = { accumulatedDrag = 0f }
                                                            ) { change, dragAmount ->
                                                                change.consume()
                                                                accumulatedDrag += dragAmount.y
                                                                while (abs(accumulatedDrag) >= reorderThreshold) {
                                                                    // The visible list is reversed (top layer first), while the
                                                                    // document model is bottom-to-top.
                                                                    val direction = if (accumulatedDrag < 0f) 1 else -1
                                                                    onMoveLayer(layer.id, direction)
                                                                    accumulatedDrag -= sign(accumulatedDrag) * reorderThreshold
                                                                }
                                                            }
                                                        }
                                                        .background(
                                                            if (selected) Accent.copy(alpha = 0.24f) else Color(0xFF202329),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .border(
                                                            if (selected) 2.dp else 1.dp,
                                                            if (selected) Color(0xFF62B5FF) else PanelEdge,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(2.dp),
                                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            Modifier.weight(1f).height(26.dp)
                                                                .padding(horizontal = 4.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Text(
                                                                (if (selected) "● " else "") + layer.name,
                                                                color = if (selected) Color(0xFF9BD0FF) else MainText,
                                                                maxLines = 1,
                                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                        TextButton(
                                                            onClick = { onToggleLayerVisibility(layer.id, !layer.visible) },
                                                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)
                                                        ) {
                                                            Text(if (layer.visible) "◉" else "○", color = MutedText)
                                                        }
                                                        TextButton(
                                                            onClick = { onToggleLayerLock(layer.id, !layer.locked) },
                                                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)
                                                        ) {
                                                            Text(if (layer.locked) "🔒" else "🔓", color = if (layer.locked) Color(0xFFFFC66D) else MutedText)
                                                        }
                                                    }
                                                    Row(
                                                        Modifier.fillMaxWidth().height(24.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                                                    ) {
                                                        TextButton(
                                                            onClick = { onMoveLayer(layer.id, 1) },
                                                            enabled = layerIndex < layers.lastIndex,
                                                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)
                                                        ) { Text("↑", color = MutedText) }
                                                        TextButton(
                                                            onClick = { onMoveLayer(layer.id, -1) },
                                                            enabled = layerIndex > 0,
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                                        ) { Text("↓", color = MutedText) }
                                                        TextButton(
                                                            onClick = { onDeleteLayer(layer.id) },
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                                        ) { Text("×", color = Color(0xFFFF8585)) }
                                                        Spacer(Modifier.weight(1f))
                                                        Text(
                                                            layer.kind.name,
                                                            color = MutedText,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        val selectedLayer = layers.firstOrNull { it.id == selectedLayerId }
                                        if (selectedLayer != null && transformControlsLayerId == selectedLayer.id) {
                                            PanelCard(label("تحويل الطبقة", "Layer Transform")) {
                                                @Composable
                                                fun transformSlider(caption: String, value: Float, range: ClosedFloatingPointRange<Float>, update: (Float) -> LayerTransform) {
                                                    Text("$caption: ${"%.2f".format(value)}", color = MutedText, style = MaterialTheme.typography.labelSmall)
                                                    Slider(
                                                        value = value.coerceIn(range.start, range.endInclusive),
                                                        onValueChange = {
                                                            onLayerTransformStart(selectedLayer.id)
                                                            onLayerTransformChange(selectedLayer.id, update(it))
                                                        },
                                                        onValueChangeFinished = onLayerTransformFinish,
                                                        valueRange = range,
                                                        enabled = !selectedLayer.locked
                                                    )
                                                }
                                                transformSlider("X", selectedLayer.transform.x, -(bitmap?.width?.toFloat() ?: 1f)..(bitmap?.width?.toFloat() ?: 1f)) {
                                                    selectedLayer.transform.copy(x = it)
                                                }
                                                transformSlider("Y", selectedLayer.transform.y, -(bitmap?.height?.toFloat() ?: 1f)..(bitmap?.height?.toFloat() ?: 1f)) {
                                                    selectedLayer.transform.copy(y = it)
                                                }
                                                transformSlider("Scale X", selectedLayer.transform.scaleX, 0.1f..5f) {
                                                    selectedLayer.transform.copy(scaleX = it)
                                                }
                                                transformSlider("Scale Y", selectedLayer.transform.scaleY, 0.1f..5f) {
                                                    selectedLayer.transform.copy(scaleY = it)
                                                }
                                                transformSlider("Angle", selectedLayer.transform.rotationDegrees, -180f..180f) {
                                                    selectedLayer.transform.copy(rotationDegrees = it)
                                                }
                                            }
                                        }
                                        if (selectedLayer != null) {
                                            var opacityPercentText by remember(selectedLayer.id) { mutableStateOf((selectedLayer.opacity * 100).toInt().toString()) }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(label("العتامة", "Opacity"), color = MainText, style = MaterialTheme.typography.labelSmall)
                                                Spacer(Modifier.weight(1f))
                                                OutlinedTextField(
                                                    value = opacityPercentText,
                                                    onValueChange = { input ->
                                                        opacityPercentText = input.filter { it.isDigit() }.take(3)
                                                        val percent = opacityPercentText.toIntOrNull()
                                                        if (percent != null && percent in 0..100) onLayerOpacityChange(selectedLayer.id, percent / 100f)
                                                    },
                                                    modifier = Modifier.width(62.dp).height(40.dp),
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.labelSmall,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    enabled = !selectedLayer.locked
                                                )
                                                Text("%", color = MutedText, style = MaterialTheme.typography.labelSmall)
                                            }
                                            Slider(
                                                value = selectedLayer.opacity,
                                                onValueChange = {
                                                    opacityPercentText = (it * 100).toInt().toString()
                                                    onLayerOpacityChange(selectedLayer.id, it)
                                                },
                                                onValueChangeFinished = onLayerOpacityFinish,
                                                enabled = !selectedLayer.locked
                                            )
                                        }
                                    }
                                }
                            }
                            if (showProperties) {
                                PanelCard(label("الخصائص", "Properties")) {
                                    AdjustmentControl(label("الإضاءة", "Brightness"), brightness, -1f..1f, canAdjustSelectedLayer, onBrightnessChange, onAdjustmentStart, onAdjustmentFinish)
                                    AdjustmentControl(label("التباين", "Contrast"), contrast, 0f..2f, canAdjustSelectedLayer, onContrastChange, onAdjustmentStart, onAdjustmentFinish)
                                    AdjustmentControl(label("التشبع", "Saturation"), saturation, 0f..2f, canAdjustSelectedLayer, onSaturationChange, onAdjustmentStart, onAdjustmentFinish)
                                }
                            }
                            if (showColorPanel) {
                                PanelCard(label("الألوان", "Colors")) {
                                    Text(label("محرك الألوان الأساسي جاهز؛ واجهة التحكم قيد التطوير", "Color engine foundation ready; controls are being developed"), color = MutedText, style = MaterialTheme.typography.labelSmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta).forEach {
                                            Box(Modifier.size(17.dp).background(it, RoundedCornerShape(3.dp)))
                                        }
                                    }
                                }
                            }
                            if (showHistory) {
                                PanelCard(label("السجل", "History")) {
                                    Text(status, color = MutedText, style = MaterialTheme.typography.labelSmall)
                                    Text(label("تراجع وإعادة متاحان من قائمة Edit", "Undo and redo are available in Edit"), color = MutedText, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (showShapes) {
                                PanelCard(label("الأشكال", "Shapes")) {
                                    Text(label("اختر أداة الشكل من شريط الأدوات؛ لا توجد أدوات شكل إضافية مفعّلة بعد", "Choose a shape tool from the tool rail; additional shape controls are not active yet"), color = MutedText, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (showBrushes) {
                                PanelCard(label("الفرش", "Brushes")) {
                                    Text(label("لوحة الفرش جاهزة كمكان عمل؛ إعدادات الفرش المتقدمة لم تُفعّل بعد", "Brush workspace is available; advanced brush settings are not active yet"), color = MutedText, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                        }

                        }
                    }

                }
                // Status and quick actions.
                Row(
                    Modifier.fillMaxWidth().height(30.dp).background(Color(0xFF111318)).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(status, color = MutedText, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.weight(1f))
                    Text(if (bitmap == null) "—" else "${(viewport.scale * 100).toInt()}%", color = Color(0xFF8FC5FF), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (imageDocumentPreview != null) {
        AlertDialog(
            onDismissRequest = onCloseImageDocument,
            title = { Text(label("مستند صورة مفتوح", "Opened Image Document")) },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(
                        bitmap = imageDocumentPreview.asImageBitmap(),
                        contentDescription = label("معاينة الصورة المستوردة", "Imported image preview"),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).graphicsLayer { scaleX = imageDocumentZoom; scaleY = imageDocumentZoom },
                        contentScale = ContentScale.Fit
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${imageDocumentPreview.width} × ${imageDocumentPreview.height} px", modifier = Modifier.weight(1f), color = MutedText, style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = { imageDocumentZoom = (imageDocumentZoom / 1.25f).coerceAtLeast(0.25f) }) { Text("−") }
                        Text("${(imageDocumentZoom * 100).toInt()}%", color = MainText, style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = { imageDocumentZoom = (imageDocumentZoom * 1.25f).coerceAtMost(4f) }) { Text("+") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onAddImageDocumentToCanvas) { Text(label("إضافة إلى مساحة التصميم", "Add to Design")) }
            },
            dismissButton = {
                TextButton(onClick = onCloseImageDocument) { Text(label("إغلاق", "Close")) }
            }
        )
    }

    if (showNewDocumentDialog) {
        AlertDialog(
            onDismissRequest = { showNewDocumentDialog = false },
            title = { Text(label("مستند جديد", "New Document")) },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(label("قالب البداية", "Starting preset"), color = MainText, style = MaterialTheme.typography.labelMedium)
                    Box {
                        OutlinedButton(onClick = { presetMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedPreset.name)
                        }
                        DropdownMenu(
                            expanded = presetMenuExpanded,
                            onDismissRequest = { presetMenuExpanded = false }
                        ) {
                            DocumentPreset.values().forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.name.replace('_', ' ')) },
                                    onClick = {
                                        selectedPreset = preset
                                        widthText = preset.widthPx.toString()
                                        heightText = preset.heightPx.toString()
                                        dpiText = preset.dpi.toString()
                                        sizeUnit = DocumentSizeUnit.PIXELS
                                        presetMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = widthText,
                            onValueChange = { widthText = it; documentError = null },
                            modifier = Modifier.weight(1f),
                            label = { Text(label("العرض", "Width")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it; documentError = null },
                            modifier = Modifier.weight(1f),
                            label = { Text(label("الارتفاع", "Height")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    Text(label("وحدة القياس", "Measurement unit"), color = MainText, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            DocumentSizeUnit.PIXELS to "px",
                            DocumentSizeUnit.INCHES to "in",
                            DocumentSizeUnit.MILLIMETERS to "mm"
                        ).forEach { (unit, caption) ->
                            FilterChip(
                                selected = sizeUnit == unit,
                                onClick = { sizeUnit = unit },
                                label = { Text(caption) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = dpiText,
                        onValueChange = { dpiText = it; documentError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("DPI") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(label("خلفية المستند", "Document background"), color = MainText, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = backgroundArgb == 0xFFFFFFFF.toInt(),
                            onClick = { backgroundArgb = 0xFFFFFFFF.toInt() },
                            label = { Text(label("أبيض", "White")) }
                        )
                        FilterChip(
                            selected = backgroundArgb == 0xFF000000.toInt(),
                            onClick = { backgroundArgb = 0xFF000000.toInt() },
                            label = { Text(label("أسود", "Black")) }
                        )
                        FilterChip(
                            selected = backgroundArgb == 0x00000000,
                            onClick = { backgroundArgb = 0x00000000 },
                            label = { Text(label("شفاف", "Transparent")) }
                        )
                    }
                    documentError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        label(
                            "الحد الآمن الحالي: 32 مليون بكسل كحد أقصى",
                            "Current safety limit: 32 million pixels maximum"
                        ),
                        color = MutedText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val width = widthText.toDoubleOrNull()
                    val height = heightText.toDoubleOrNull()
                    val dpi = dpiText.toIntOrNull()
                    if (width == null || height == null || dpi == null) {
                        documentError = label("أدخل أرقامًا صحيحة للأبعاد وDPI", "Enter valid numeric dimensions and DPI")
                    } else {
                        try {
                            val spec = DocumentSpec.fromPhysicalSize(
                                width = width,
                                height = height,
                                unit = sizeUnit,
                                dpi = dpi,
                                backgroundArgb = backgroundArgb
                            )
                            viewport = CanvasViewport()
                            onCreateDocument(spec)
                            showNewDocumentDialog = false
                        } catch (error: IllegalArgumentException) {
                            documentError = error.message ?: label("إعدادات المستند غير صالحة", "Invalid document settings")
                        }
                    }
                }) { Text(label("إنشاء", "Create")) }
            },
            dismissButton = {
                TextButton(onClick = { showNewDocumentDialog = false }) { Text(label("إلغاء", "Cancel")) }
            }
        )
    }
}

@Composable
private fun PanelCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    var dragOffset by remember(title) { mutableStateOf(Offset(0f, 0f)) }
    val startY = when (title) {
        "Layers", "الطبقات" -> 44
        "Properties", "الخصائص" -> 310
        "Colors", "الألوان" -> 520
        "History", "السجل" -> 650
        "Shapes", "الأشكال" -> 164
        else -> 240
    }
    val startX = when (title) { "Colors", "الألوان" -> 150; "Shapes", "الأشكال" -> 150; else -> 0 }
    Column(
        Modifier.width(156.dp).offset { IntOffset(startX.dp.roundToPx() + dragOffset.x.roundToInt(), startY.dp.roundToPx() + dragOffset.y.roundToInt()) }
            .background(Color(0xFF292D35), RoundedCornerShape(5.dp)).border(1.dp, PanelEdge, RoundedCornerShape(5.dp)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = {
            Row(Modifier.fillMaxWidth().pointerInput(title) {
                detectDragGestures { change, amount -> change.consume(); dragOffset += amount }
            }, verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), color = MainText, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                Text("⠿", color = MutedText, style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider(color = PanelEdge)
            content()
        }
    )
}

@Composable
private fun AdjustmentControl(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onChange: (Float) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MainText, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(String.format("%.2f", value), color = MutedText, style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = value,
            onValueChange = { onStart(); onChange(it) },
            onValueChangeFinished = onFinish,
            valueRange = range,
            enabled = enabled
        )
    }
}
