package com.photoeditor.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.photoeditor.engine.document.EditorLayer

private val AppBg = Color(0xFF17191E)
private val PanelBg = Color(0xFF22252C)
private val PanelEdge = Color(0xFF353A44)
private val Accent = Color(0xFF2685E8)
private val MainText = Color(0xFFE7EAF0)
private val MutedText = Color(0xFFAAB2C0)

@Composable
fun EditorScreen(
    bitmap: Bitmap?,
    layers: List<EditorLayer>,
    selectedLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onSetLayerVisibility: (String, Boolean) -> Unit,
    status: String,
    canUndo: Boolean,
    canRedo: Boolean,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    isArabic: Boolean,
    onToggleLanguage: () -> Unit,
    onOpen: () -> Unit,
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
    onAdjustmentFinish: () -> Unit
) {
    val direction = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    var selectedTool by remember { mutableStateOf("Move") }
    var showLayers by remember { mutableStateOf(true) }
    var showProperties by remember { mutableStateOf(true) }
    var showColorPanel by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var activeMenu by remember { mutableStateOf<String?>(null) }

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
                    Text(if (bitmap == null) "Untitled-1" else "Image-1", color = MutedText, style = MaterialTheme.typography.labelSmall)
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
                                        DropdownMenuItem(text = { Text(label("فتح صورة", "Open Image")) }, onClick = { activeMenu = null; onOpen() })
                                        DropdownMenuItem(text = { Text(label("تصدير PNG", "Export PNG")) }, enabled = bitmap != null, onClick = { activeMenu = null; onSave() })
                                    }
                                    "Edit" -> {
                                        DropdownMenuItem(text = { Text(label("تراجع", "Undo")) }, enabled = canUndo, onClick = { activeMenu = null; onUndo() })
                                        DropdownMenuItem(text = { Text(label("إعادة", "Redo")) }, enabled = canRedo, onClick = { activeMenu = null; onRedo() })
                                    }
                                    "Image" -> {
                                        DropdownMenuItem(text = { Text(label("أبيض وأسود", "Grayscale")) }, enabled = bitmap != null, onClick = { activeMenu = null; onGrayscale() })
                                        DropdownMenuItem(text = { Text(label("تدوير 90°", "Rotate 90°")) }, enabled = bitmap != null, onClick = { activeMenu = null; onRotate() })
                                        DropdownMenuItem(text = { Text(label("استعادة الأصل", "Restore Original")) }, enabled = bitmap != null, onClick = { activeMenu = null; onReset() })
                                    }
                                    "Window" -> {
                                        DropdownMenuItem(text = { Text(label("الطبقات", "Layers") + if (showLayers) " ✓" else "") }, onClick = { showLayers = !showLayers; activeMenu = null })
                                        DropdownMenuItem(text = { Text(label("الخصائص", "Properties") + if (showProperties) " ✓" else "") }, onClick = { showProperties = !showProperties; activeMenu = null })
                                        DropdownMenuItem(text = { Text(label("الألوان", "Colors") + if (showColorPanel) " ✓" else "") }, onClick = { showColorPanel = !showColorPanel; activeMenu = null })
                                        DropdownMenuItem(text = { Text(label("السجل", "History") + if (showHistory) " ✓" else "") }, onClick = { showHistory = !showHistory; activeMenu = null })
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
                    Spacer(Modifier.weight(1f))
                    Text(if (bitmap == null) label("لا يوجد مستند", "No document") else "${bitmap.width} × ${bitmap.height} px", color = MutedText, style = MaterialTheme.typography.labelSmall)
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
                        Modifier.weight(1f).fillMaxHeight().background(Color(0xFF30343C)).padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("＋", color = Color(0xFF8B96A7), style = MaterialTheme.typography.displayMedium)
                                Text(label("مساحة العمل", "Workspace"), color = MainText, fontWeight = FontWeight.SemiBold)
                                Text(label("افتح صورة للبدء أو أنشئ مستندًا جديدًا", "Open an image or create a new document"), color = MutedText, style = MaterialTheme.typography.bodySmall)
                                Button(onClick = onOpen) { Text(label("فتح صورة", "Open Image")) }
                            }
                        } else if (layers.firstOrNull()?.visible != false) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = label("معاينة مساحة العمل", "Canvas preview"),
                                modifier = Modifier.fillMaxSize().border(1.dp, Color(0xFF596170)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    // Dockable right-side panels.
                    if (showLayers || showProperties || showColorPanel || showHistory) {
                        Column(
                            Modifier.width(176.dp).fillMaxHeight().background(PanelBg).padding(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            if (showLayers) {
                                PanelCard(label("الطبقات", "Layers")) {
                                    if (layers.isEmpty()) {
                                        Text(label("افتح صورة لإضافة طبقة", "Open an image to add a layer"), color = MutedText, style = MaterialTheme.typography.labelSmall)
                                    } else {
                                        layers.asReversed().forEach { layer ->
                                            Row(
                                                Modifier.fillMaxWidth()
                                                    .background(
                                                        if (layer.id == selectedLayerId) Accent.copy(alpha = 0.22f) else Color.Transparent,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (layer.id == selectedLayerId) Accent else PanelEdge,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = { onSetLayerVisibility(layer.id, !layer.visible) },
                                                    modifier = Modifier.size(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text(if (layer.visible) "◉" else "○", color = if (layer.visible) Color(0xFF8FC5FF) else MutedText)
                                                }
                                                Text(
                                                    layer.name,
                                                    color = MainText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                TextButton(
                                                    onClick = { onSelectLayer(layer.id) },
                                                    modifier = Modifier.size(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text(if (layer.id == selectedLayerId) "●" else "○", color = Accent)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (showProperties) {
                                PanelCard(label("الخصائص", "Properties")) {
                                    AdjustmentControl(label("الإضاءة", "Brightness"), brightness, -1f..1f, bitmap != null, onBrightnessChange, onAdjustmentStart, onAdjustmentFinish)
                                    AdjustmentControl(label("التباين", "Contrast"), contrast, 0f..2f, bitmap != null, onContrastChange, onAdjustmentStart, onAdjustmentFinish)
                                    AdjustmentControl(label("التشبع", "Saturation"), saturation, 0f..2f, bitmap != null, onSaturationChange, onAdjustmentStart, onAdjustmentFinish)
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
                        }
                    }
                }
                // Status and quick actions.
                Row(
                    Modifier.fillMaxWidth().height(30.dp).background(Color(0xFF111318)).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(status, color = MutedText, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.weight(1f))
                    Text(if (bitmap == null) "—" else "100%", color = Color(0xFF8FC5FF), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun PanelCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF292D35), RoundedCornerShape(5.dp)).border(1.dp, PanelEdge, RoundedCornerShape(5.dp)).padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        content = {
            Text(title, color = MainText, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
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
