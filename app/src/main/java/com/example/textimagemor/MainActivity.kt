package com.example.textimagemor

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
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
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.view.WindowCompat
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin
import android.text.TextPaint as AndroidTextPaint

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun enableEdgeToEdge() {
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputText = remember { mutableStateOf("") }
    val currentBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val scrollState = rememberScrollState()
    val saveFailedText = stringResource(R.string.save_failed)

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            currentBitmap.value?.let { bmp ->
                saveBitmapToGallery(context, bmp)
            }
        } else {
            Toast.makeText(context, saveFailedText, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = inputText.value,
            onValueChange = { inputText.value = it },
            label = { Text(stringResource(R.string.hint_input_text)) },
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
                Text(stringResource(R.string.button_clear_content))
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
                Text(stringResource(R.string.button_generate))
            }
        }

        if (currentBitmap.value != null) {
            androidx.compose.foundation.Image(
                bitmap = currentBitmap.value!!.asImageBitmap(),
                contentDescription = stringResource(R.string.generated_image_content_description),
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
                    Text(stringResource(R.string.button_share))
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
                            Toast.makeText(context, saveFailedText, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.button_save))
                }
            }

            Button(
                onClick = {
                    currentBitmap.value = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.button_clear))
            }
        }
    }
}

fun textToBitmap(text: String): Bitmap {
    val baseTextSize = 56f
    val padding = 60
    val bitmapWidth = 1080
    val seed = System.currentTimeMillis()
    val availableWidth = (bitmapWidth - 2 * padding).toFloat()

    val rawLines = text.split("\n").toMutableList()
    if (rawLines.isEmpty()) rawLines.add("")

    val measurePaint = AndroidTextPaint().apply {
        textSize = baseTextSize
        isAntiAlias = true
    }
    val fontMetrics = measurePaint.fontMetricsInt
    val lineHeight = (fontMetrics.bottom - fontMetrics.top) * 1.4f
    val waveAmplitude = lineHeight * 0.7f

    var totalVisualLines = 0
    for (line in rawLines) {
        var x = 0f
        totalVisualLines++
        for (char in line) {
            val charWidth = measurePaint.measureText(char.toString())
            if (x + charWidth > availableWidth && x > 0) {
                totalVisualLines++
                x = charWidth
            } else {
                x += charWidth
            }
        }
    }

    val bitmapHeight = maxOf(
        (padding * 2 + (lineHeight + waveAmplitude * 2 + 12) * totalVisualLines).toInt(),
        200
    )

    val bitmap = createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    addBackgroundTexture(canvas, bitmapWidth, bitmapHeight)

    var baseY = padding + waveAmplitude + 16 - fontMetrics.top
    for (lineIdx in rawLines.indices) {
        val line = rawLines[lineIdx]
        val lineRandom = Random(seed + lineIdx * 9973L)

        val frequency = 1.8f + lineRandom.nextFloat() * 2.0f
        val phase = lineRandom.nextFloat() * Math.PI.toFloat() * 2f
        val amplitude = waveAmplitude * (0.7f + lineRandom.nextFloat() * 0.6f)

        var currentX = padding.toFloat()
        var prevWasSameType = false
        var sameTypeGroupY = 0f

        for (charIdx in line.indices) {
            val ch = line[charIdx]
            val charText = ch.toString()
            val charRandom = Random(seed + lineIdx * 9973L + charIdx * 7919L)

            val sizeScale = 0.88f + charRandom.nextFloat() * 0.24f
            val baseAlpha = 0.55f + charRandom.nextFloat() * 0.45f
            val charAlpha = (baseAlpha * 255).toInt()
            val charPaint = AndroidTextPaint().apply {
                isAntiAlias = true
                textSize = baseTextSize * sizeScale
                color = Color.argb(charAlpha, 25, 28, 32)
                isFakeBoldText = charRandom.nextFloat() < 0.15f
            }

            val charWidth = charPaint.measureText(charText)

            if (currentX + charWidth > availableWidth && currentX > padding) {
                baseY += lineHeight + waveAmplitude * 2 + 8
                currentX = padding.toFloat()
                prevWasSameType = false
                sameTypeGroupY = 0f
            }

            val currentIsSameType = isSameTypeChar(ch)
            val charY: Float
            if (currentIsSameType && prevWasSameType) {
                charY = sameTypeGroupY
            } else {
                val progress = currentX / bitmapWidth.toFloat()
                val angle = progress * Math.PI.toFloat() * 2f * frequency + phase
                val sineOffsetY = amplitude * sin(angle.toDouble()).toFloat()
                charY = baseY + sineOffsetY
                sameTypeGroupY = charY
            }
            prevWasSameType = currentIsSameType

            val charX = currentX

            val rotation = (charRandom.nextFloat() - 0.5f) * 28f
            val deformSkewX = (charRandom.nextFloat() - 0.5f) * 0.16f
            val deformSkewY = (charRandom.nextFloat() - 0.5f) * 0.12f
            val deformScaleX = 0.85f + charRandom.nextFloat() * 0.30f
            val deformScaleY = 0.85f + charRandom.nextFloat() * 0.30f
            val has3D = charRandom.nextFloat() < 0.35f
            val isOutline = charRandom.nextFloat() < 0.25f
            val centerX = charX + charWidth / 2
            val centerY = charY

            if (has3D) {
                val depthLayers = 3 + charRandom.nextInt(3)
                for (layer in 1..depthLayers) {
                    val offset = layer * 1.0f
                    val layerFraction = baseAlpha * (0.65f - layer * 0.12f) * 0.35f
                    val layerAlpha = maxOf((layerFraction * 255).toInt(), 6)
                    val layerPaint = AndroidTextPaint(charPaint).apply {
                        color = Color.argb(layerAlpha, 20, 22, 26)
                        style = Paint.Style.FILL
                    }
                    canvas.drawText(charText, charX + offset, charY + offset, layerPaint)
                }
            }

            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.rotate(rotation)
            canvas.skew(deformSkewX, deformSkewY)
            canvas.scale(deformScaleX, deformScaleY)
            canvas.translate(-centerX, -centerY)

            if (isOutline) {
                val outlinePaint = AndroidTextPaint(charPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f + charRandom.nextFloat() * 2.2f
                    alpha = (baseAlpha * 0.85f * 255).toInt()
                }
                canvas.drawText(charText, charX, charY, outlinePaint)
            }

            canvas.drawText(charText, charX, charY, charPaint)
            canvas.restore()

            if (charRandom.nextFloat() < 0.10f) {
                val sx = charX + charRandom.nextFloat() * charWidth
                val sy = charY - charRandom.nextFloat() * 18
                val path = Path()
                path.moveTo(sx, sy)
                val dx = sx + (charRandom.nextFloat() - 0.5f) * 18
                val dy = sy + charRandom.nextFloat() * 22
                val cpx = (sx + dx) / 2 + (charRandom.nextFloat() - 0.5f) * 14
                val cpy = (sy + dy) / 2 + (charRandom.nextFloat() - 0.5f) * 14
                path.quadTo(cpx, cpy, dx, dy)
                canvas.drawPath(path, Paint().apply {
                    color = Color.argb(
                        (charRandom.nextFloat() * 70 + 30).toInt(), 30, 35, 35
                    )
                    strokeWidth = 0.7f + charRandom.nextFloat() * 1.6f
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                })
            }

            val spacing = charWidth * (0.88f + charRandom.nextFloat() * 0.26f)
            currentX += spacing + (charRandom.nextFloat() - 0.45f) * 6
        }

        baseY += lineHeight + waveAmplitude * 2 + 8
    }

    return bitmap
}

private fun isSameTypeChar(c: Char): Boolean = when (c) {
    in '0'..'9' -> true
    in 'a'..'z' -> true
    in 'A'..'Z' -> true
    in '!'..'/' -> true
    in ':'..'@' -> true
    in '['..'`' -> true
    in '{'..'~' -> true
    else -> false
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

    // 直接使用原始图片，不需要整体旋转
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val newWidth = result.width
    val newHeight = result.height

    val linePaint1 = Paint().apply {
        color = Color.argb(55, 0, 0, 0)
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
        color = Color.argb(48, 0, 0, 0)
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
        color = Color.argb(35, 50, 50, 50)
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
        color = Color.argb(38, 0, 0, 0)
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
        color = Color.argb(32, 0, 0, 0)
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

    val linePaint6 = Paint().apply {
        color = Color.argb(30, 40, 40, 40)
        strokeWidth = 0.8f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    val spacing6 = 7
    val path6 = Path()
    for (i in -newHeight / spacing6 until newWidth / spacing6 + newHeight / spacing6) {
        val baseX = (i * spacing6).toFloat()
        path6.reset()
        path6.moveTo(baseX, 0f)
        for (y in 0..newHeight step 6) {
            val waveOffset = (sin(y * 0.11 + 2.3) * 8 + cos(y * 0.07) * 5).toFloat()
            path6.lineTo(baseX + y * 0.7f + waveOffset, y.toFloat())
        }
        canvas.drawPath(path6, linePaint6)
    }

    // 降低对比度
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

    // 添加高斯噪声
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
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val imageFile = java.io.File(cacheDir, "share_image_${System.currentTimeMillis()}.jpg")

        val outputStream = java.io.FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        outputStream.flush()
        outputStream.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_title)))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, context.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
    }
}
