package com.photoeditor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Canvas as AndroidCanvas
import android.os.Bundle
import android.provider.MediaStore
import android.content.ContentValues
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PhotoEditorApp()
            }
        }
    }
}

@Composable
private fun PhotoEditorApp() {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var grayscale by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("افتح صورة للبدء في التحرير") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        originalBitmap = bitmap
                        editedBitmap = bitmap
                        grayscale = false
                        status = "تم تحميل الصورة بنجاح"
                    } else {
                        status = "تعذر قراءة الصورة"
                    }
                }
            } catch (error: Exception) {
                status = "حدث خطأ أثناء فتح الصورة"
            }
        }
    }

    fun applyCurrentEffect() {
        val source = originalBitmap ?: return
        if (!grayscale) {
            editedBitmap = source
            status = "تمت إعادة الصورة إلى الأصل"
            return
        }
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        editedBitmap = output
        status = "تم تطبيق الأبيض والأسود"
    }

    fun exportImage() {
        val bitmap = editedBitmap ?: return
        try {
            val filename = "PhotoEditor_" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoEditor")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
                status = "تم حفظ الصورة في Pictures/PhotoEditor"
            } else {
                status = "تعذر إنشاء ملف الصورة"
            }
        } catch (error: Exception) {
            status = "فشل حفظ الصورة"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF111318)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("PhotoEditor", color = Color.White, fontSize = 23.sp)
                Text("0.1.0", color = Color(0xFF9DA6B5), fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF20242D), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val image = editedBitmap
                if (image != null) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = "الصورة قيد التحرير",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        "مساحة العمل\n\nافتح صورة من جهازك لتظهر هنا",
                        color = Color(0xFFB8C0CC),
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(status, color = Color(0xFFB8C0CC), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) { Text("فتح صورة") }
                Button(
                    onClick = {
                        if (originalBitmap != null) {
                            grayscale = !grayscale
                            applyCurrentEffect()
                        } else status = "افتح صورة أولاً"
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (grayscale) "إلغاء الأبيض والأسود" else "أبيض وأسود") }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        editedBitmap = originalBitmap
                        grayscale = false
                        status = "تم التراجع إلى الصورة الأصلية"
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("إعادة ضبط") }
                Button(
                    onClick = { exportImage() },
                    enabled = editedBitmap != null,
                    modifier = Modifier.weight(1f)
                ) { Text("حفظ الصورة") }
            }
        }
    }
}
