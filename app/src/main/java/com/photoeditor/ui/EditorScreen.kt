package com.photoeditor.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun EditorScreen(
    bitmap: Bitmap?,
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
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF111318)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PhotoEditor", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    OutlinedButton(onClick = onToggleLanguage) {
                        Text(if (isArabic) "English" else "العربية")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .background(Color(0xFF20242D), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap == null) {
                        Text(
                            if (isArabic) "مساحة العمل\n\nافتح صورة من جهازك للبدء"
                            else "Workspace\n\nOpen an image from your device to begin",
                            color = Color(0xFFCBD2DC)
                        )
                    } else {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = if (isArabic) "معاينة الصورة" else "Image preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(status, color = Color(0xFFCBD2DC))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = onOpen, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "فتح" else "Open")
                    }
                    Button(onClick = onSave, enabled = bitmap != null, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "حفظ" else "Save")
                    }
                    OutlinedButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "تراجع" else "Undo")
                    }
                    OutlinedButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "إعادة" else "Redo")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onGrayscale, enabled = bitmap != null, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "أبيض وأسود" else "B&W")
                    }
                    OutlinedButton(onClick = onRotate, enabled = bitmap != null, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "تدوير 90°" else "Rotate 90°")
                    }
                    OutlinedButton(onClick = onReset, enabled = bitmap != null, modifier = Modifier.weight(1f)) {
                        Text(if (isArabic) "الأصل" else "Original")
                    }
                }
                Spacer(Modifier.height(4.dp))
                AdjustmentRow(
                    if (isArabic) "الإضاءة" else "Brightness", brightness, -1f, 1f,
                    onBrightnessChange, bitmap != null, onAdjustmentStart, onAdjustmentFinish
                )
                AdjustmentRow(
                    if (isArabic) "التباين" else "Contrast", contrast, 0f, 2f,
                    onContrastChange, bitmap != null, onAdjustmentStart, onAdjustmentFinish
                )
                AdjustmentRow(
                    if (isArabic) "التشبع" else "Saturation", saturation, 0f, 2f,
                    onSaturationChange, bitmap != null, onAdjustmentStart, onAdjustmentFinish
                )
            }
        }
    }
}

@Composable
private fun AdjustmentRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
    enabled: Boolean,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFE2E7EF), modifier = Modifier.weight(1f))
        Slider(
            value = value,
            onValueChange = { next -> onStart(); onChange(next) },
            onValueChangeFinished = onFinish,
            valueRange = min..max,
            enabled = enabled,
            modifier = Modifier.weight(3f)
        )
    }
}
