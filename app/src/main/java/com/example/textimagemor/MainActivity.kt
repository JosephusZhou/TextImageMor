package com.example.textimagemor

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.core.view.WindowCompat
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import kotlin.math.cos
import kotlin.math.sin
import android.text.TextPaint as AndroidTextPaint
import java.util.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启用沉浸式状态栏，让内容延伸到系统栏后面
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputText = remember { mutableStateOf("") }
    val currentBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val scrollState = rememberScrollState()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            currentBitmap.value?.let { bmp ->
                saveBitmapToGallery(context, bmp)
            }
        } else {
            Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // 顶部避免状态栏遮挡
            .navigationBarsPadding() // 底部避免导航栏遮挡
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = context.getString(R.string.app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = inputText.value,
            onValueChange = { inputText.value = it },
            label = { Text(context.getString(R.string.hint_input_text)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            maxLines = 10
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { inputText.value = "" },
                modifier = Modifier.weight(1f)
            ) {
                Text(context.getString(R.string.button_clear_content))
            }

            Button(
                onClick = {
                    if (inputText.value.isNotEmpty()) {
                        val bitmap = textToBitmap(inputText.value)
                        val bitmapWithMoire = addMoireEffect(bitmap)
                        currentBitmap.value = bitmapWithMoire
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(context.getString(R.string.button_generate))
            }
        }

        if (currentBitmap.value != null) {
            androidx.compose.foundation.Image(
                bitmap = currentBitmap.value!!.asImageBitmap(),
                contentDescription = "Generated Image",
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        currentBitmap.value?.let { bmp ->
                            shareBitmap(context, bmp)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(context.getString(R.string.button_share))
                }

                Button(
                    onClick = {
                        currentBitmap.value = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(context.getString(R.string.button_clear))
                }
            }

            Button(
                onClick = {
                    currentBitmap.value?.let { bmp ->
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            saveBitmapToGallery(context, bmp)
                        }
                    } ?: run {
                        Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(context.getString(R.string.button_save))
            }
        }
    }
}

fun textToBitmap(text: String): Bitmap {
    val textPaint = AndroidTextPaint().apply {
        color = Color.BLACK
        textSize = 50f
        isAntiAlias = true
    }

    val lines = text.split("\n")
    val lineHeight = textPaint.fontMetricsInt.bottom - textPaint.fontMetricsInt.top
    val padding = 60
    val bitmapWidth = 1080
    val bitmapHeight = padding * 2 + lineHeight * lines.size

    val bitmap = createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 先绘制背景纹理（直的）
    addBackgroundTexture(canvas, bitmapWidth, bitmapHeight)

    val x = padding.toFloat()
    var y = (padding - textPaint.fontMetricsInt.top).toFloat()

    val random = Random()

    // 然后在上面绘制字符（每个字符独立旋转5-10度，所以是倾斜的）
    for (line in lines) {
        var currentX = x
        line.forEachIndexed { index, char ->
            val rotation = random.nextInt(6) + 5
            val alpha = ((random.nextFloat() * 0.2f + 0.4f) * 255).toInt()
            textPaint.alpha = alpha

            val spacingPerturbation = if (index > 0) random.nextInt(11) - 5 else 0

            canvas.save()
            val charX = currentX + spacingPerturbation
            canvas.translate(charX, y)
            canvas.rotate(rotation.toFloat())

            val shadowPaint = Paint(textPaint).apply {
                color = Color.argb((alpha * 0.3).toInt(), 0, 0, 0)
                maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawText(char.toString(), 2f, 2f, shadowPaint)

            canvas.drawText(char.toString(), 0f, 0f, textPaint)
            canvas.restore()

            val charWidth = textPaint.measureText(char.toString())
            val overlapFactor = random.nextFloat() * 0.3f
            currentX = charX + charWidth * (1 - overlapFactor)
        }
        y += lineHeight
    }

    return bitmap
}
fun addBackgroundTexture(canvas: Canvas, width: Int, height: Int) {
    val random = Random()
    canvas.drawColor(Color.argb(255, 245, 245, 240))

    val gridPaint = Paint().apply {
        color = Color.argb(15, 200, 200, 200)
        strokeWidth = 0.5f
        isAntiAlias = true
    }

    for (y in 0 until height step 20) {
        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
    }

    for (x in 0 until width step 20) {
        canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
    }

    val noisePaint = Paint()
    for (i in 0 until (width * height * 0.02).toInt()) {
        val x = random.nextInt(width)
        val y = random.nextInt(height)
        noisePaint.color = Color.argb(
            random.nextInt(20) + 10,
            random.nextInt(100),
            random.nextInt(100),
            random.nextInt(100)
        )
        canvas.drawPoint(x.toFloat(), y.toFloat(), noisePaint)
    }

    val gradientPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                Color.argb(10, 100, 100, 100),
                Color.argb(0, 0, 0, 0),
                Color.argb(10, 100, 100, 100)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)
}

fun addMoireEffect(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    // 直接使用原始图片，不需要整体旋转（字符级旋转已在textToBitmap中实现）
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val newWidth = result.width
    val newHeight = result.height

    val linePaint1 = Paint().apply {
        color = Color.argb(40, 0, 0, 0)
        strokeWidth = 1.5f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    val spacing1 = 8
    val path1 = Path()
    for (i in -newHeight / spacing1 until newWidth / spacing1 + newHeight / spacing1) {
        val baseX = (i * spacing1).toFloat()
        path1.reset()
        path1.moveTo(baseX, 0f)
        for (y in 0..newHeight step 8) {
            val waveOffset = (sin(y * 0.1) * 12 + cos(y * 0.06) * 8).toFloat()
            path1.lineTo(baseX + y * 0.8f + waveOffset, y.toFloat())
        }
        canvas.drawPath(path1, linePaint1)
    }

    val linePaint2 = Paint().apply {
        color = Color.argb(35, 0, 0, 0)
        strokeWidth = 1.2f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    val spacing2 = 9
    val path2 = Path()
    for (i in -newHeight / spacing2 until newWidth / spacing2 + newHeight / spacing2) {
        val baseX = (i * spacing2).toFloat()
        path2.reset()
        path2.moveTo(baseX, newHeight.toFloat())
        for (y in 0..newHeight step 8) {
            val waveOffset =
                (sin(y * 0.08 + 1.5) * 15 + cos(y * 0.05) * 10).toFloat()
            path2.lineTo(baseX + y * 0.9f + waveOffset, newHeight - y.toFloat())
        }
        canvas.drawPath(path2, linePaint2)
    }

    val linePaint3 = Paint().apply {
        color = Color.argb(25, 50, 50, 50)
        strokeWidth = 1f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    val path3 = Path()
    for (y in 0 until newHeight step 6) {
        path3.reset()
        path3.moveTo(0f, y.toFloat())
        for (x in 0..newWidth step 8) {
            val waveOffset = (sin(x * 0.08) * 6 + cos(x * 0.04) * 4).toFloat()
            path3.lineTo(x.toFloat(), y + waveOffset)
        }
        canvas.drawPath(path3, linePaint3)
    }

    val linePaint4 = Paint().apply {
        color = Color.argb(28, 0, 0, 0)
        strokeWidth = 1f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    val spacing4 = 10
    val path4 = Path()
    for (i in 0 until newWidth / spacing4) {
        val baseX = (i * spacing4).toFloat()
        path4.reset()
        path4.moveTo(baseX, 0f)
        for (y in 0..newHeight step 8) {
            val waveOffset = (sin(y * 0.09) * 10 + cos(y * 0.05) * 6).toFloat()
            path4.lineTo(baseX + waveOffset, y.toFloat())
        }
        canvas.drawPath(path4, linePaint4)
    }

    val linePaint5 = Paint().apply {
        color = Color.argb(22, 0, 0, 0)
        strokeWidth = 1f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    val spacing5 = 11
    val path5 = Path()
    for (i in 0 until newWidth / spacing5) {
        val baseX = (i * spacing5).toFloat()
        path5.reset()
        path5.moveTo(baseX, newHeight.toFloat())
        for (y in newHeight downTo 0 step 8) {
            val waveOffset = (sin(y * 0.07 + 1.8) * 12 + cos(y * 0.04) * 8).toFloat()
            path5.lineTo(baseX + waveOffset, y.toFloat())
        }
        canvas.drawPath(path5, linePaint5)
    }

    val contrastMatrix = ColorMatrix().apply {
        setScale(0.9f, 0.9f, 0.9f, 1f)
    }
    val contrastPaint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(contrastMatrix)
        isAntiAlias = true
    }
    val contrastBitmap =
        createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
    val contrastCanvas = Canvas(contrastBitmap)
    contrastCanvas.drawBitmap(result, 0f, 0f, contrastPaint)

    val noiseBitmap =
        contrastBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val noiseCanvas = Canvas(noiseBitmap)
    val random = Random()
    val noiseDensity = 0.03
    val totalPixels = newWidth * newHeight
    val noisePixels = (totalPixels * noiseDensity).toInt()

    for (i in 0 until noisePixels) {
        val x = random.nextInt(newWidth)
        val y = random.nextInt(newHeight)
        val noisePaint = Paint().apply {
            color = Color.argb(
                random.nextInt(5) + 8,
                random.nextInt(20),
                random.nextInt(20),
                random.nextInt(20)
            )
            isAntiAlias = false
        }
        noiseCanvas.drawPoint(x.toFloat(), y.toFloat(), noisePaint)
    }

    return noiseBitmap
}

fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap) {
    val timestamp =
        java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
    val filename = "TextImageMor_$timestamp.jpg"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/TextImageMor")
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    if (uri != null) {
        try {
            val outputStream = resolver.openOutputStream(uri)
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                outputStream.close()
                Toast.makeText(context, context.getString(R.string.save_success), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
    }
}

fun shareBitmap(context: android.content.Context, bitmap: Bitmap) {
    try {
        // 保存到缓存文件
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val imageFile = java.io.File(cacheDir, "share_image_${System.currentTimeMillis()}.jpg")
        
        val outputStream = java.io.FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        outputStream.flush()
        outputStream.close()
        
        // 使用 FileProvider 获取 URI
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
        
        // 创建分享 Intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // 显示分享选择器
        context.startActivity(Intent.createChooser(shareIntent, "分享图片"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
    }
}
