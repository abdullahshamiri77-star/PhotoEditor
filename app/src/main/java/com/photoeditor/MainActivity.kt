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
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var importedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var adjustmentBase by remember { mutableStateOf<Bitmap?>(null) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var status by remember { mutableStateOf("افتح صورة للبدء في التحرير") }
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
        status = "معاينة التعديلات"
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
                        history.clear()
                        finishAdjustment()
                        status = "تم فتح الصورة بنجاح"
                    } else {
                        status = "تعذر قراءة الصورة المختارة"
                    }
                }
            } catch (error: Exception) {
                status = "تعذر فتح الصورة: ${error.localizedMessage ?: "خطأ غير معروف"}"
            }
        }
    }

    fun applyEffect(effect: () -> Bitmap) {
        finishAdjustment()
        val source = currentBitmap ?: run {
            status = "افتح صورة أولاً"
            return
        }
        history.record(source)
        currentBitmap = effect()
        status = "تم تطبيق التأثير"
    }

    fun undo() {
        finishAdjustment()
        val source = currentBitmap ?: return
        val previous = history.undo(source)
        if (previous != null) {
            currentBitmap = previous
            status = "تم التراجع"
        }
    }

    fun redo() {
        finishAdjustment()
        val source = currentBitmap ?: return
        val next = history.redo(source)
        if (next != null) {
            currentBitmap = next
            status = "تمت إعادة العملية"
        }
    }

    fun resetImage() {
        finishAdjustment()
        val source = currentBitmap ?: return
        val original = importedBitmap ?: return
        history.record(source)
        currentBitmap = original.copy(Bitmap.Config.ARGB_8888, false)
        status = "تمت استعادة الصورة الأصلية"
    }

    EditorScreen(
        bitmap = currentBitmap,
        status = status,
        canUndo = history.canUndo,
        canRedo = history.canRedo,
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        onOpen = { imagePicker.launch("image/*") },
        onSave = {
            finishAdjustment()
            val bitmap = currentBitmap
            if (bitmap != null) {
                val result = ImageExporter.savePng(context, bitmap)
                status = result.getOrElse { "فشل الحفظ: ${it.localizedMessage ?: "خطأ"}" }
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
        onAdjustmentFinish = { finishAdjustment(); status = "تم تثبيت التعديلات" }
    )
}
