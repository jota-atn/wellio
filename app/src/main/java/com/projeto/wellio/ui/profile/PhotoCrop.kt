package com.projeto.wellio.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Lado do recorte salvo, em pixels. */
private const val CROP_SIZE = 512

/** Maior lado da foto carregada para recortar: o bastante para o recorte, sem pesar na memória. */
private const val MAX_SOURCE_SIDE = 2048

/**
 * Enquadrar a foto escolhida (SPEC §9.1): a foto atrás de uma máscara circular; arrastar
 * move e pinçar aproxima. Confirmar devolve o quadrado sob o círculo.
 */
@Composable
fun PhotoCropDialog(uri: Uri, onDismiss: () -> Unit, onCropped: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val source by produceState<Result<Bitmap>?>(null, uri) {
        value = withContext(Dispatchers.IO) { runCatching { loadBitmap(context, uri) } }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val bitmap = source?.getOrNull()
            when {
                source == null -> CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
                bitmap == null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Não deu para abrir essa foto", color = Color.White)
                    TextButton(onClick = onDismiss) { Text("Voltar") }
                }
                else -> Cropper(bitmap, onDismiss, onCropped)
            }
        }
    }
}

@Composable
private fun BoxScope.Cropper(bitmap: Bitmap, onDismiss: () -> Unit, onCropped: (Bitmap) -> Unit) {
    val density = LocalDensity.current
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var area by remember { mutableStateOf(IntSize.Zero) }
    // Zoom do usuário (1 = a foto cobre o círculo) e deslocamento do centro, em pixels de tela
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val diameter = with(density) { min(area.width, area.height).toFloat() - 48.dp.toPx() }.coerceAtLeast(1f)
    val baseScale = diameter / min(bitmap.width, bitmap.height)
    val scale = baseScale * zoom

    /** Não deixa sobrar borda dentro do círculo. */
    fun clamp(o: Offset, s: Float): Offset {
        val maxX = max(0f, (bitmap.width * s - diameter) / 2)
        val maxY = max(0f, (bitmap.height * s - diameter) / 2)
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { area = it }
            .pointerInput(bitmap, diameter) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                    offset = clamp(offset + pan, baseScale * zoom)
                }
            }
            .semantics { contentDescription = "Arraste para mover e pince para aproximar a foto" }
    ) {
        Image(
            image,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .requiredSize(with(density) { bitmap.width.toDp() }, with(density) { bitmap.height.toDp() })
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
        // Escurece fora do círculo
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color.Black.copy(alpha = 0.6f))
            drawCircle(Color.Black, radius = diameter / 2, center = center, blendMode = BlendMode.Clear)
            drawCircle(Color.White.copy(alpha = 0.8f), radius = diameter / 2, center = center, style = Stroke(1.5.dp.toPx()))
        }
    }

    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White) }
        Spacer(Modifier.weight(1f))
        val colors = WellioTheme.colors.base
        Button(
            onClick = { onCropped(crop(bitmap, diameter, scale, offset)) },
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
        ) { Text("Confirmar", style = MaterialTheme.typography.labelLarge) }
    }
}

/** Quadrado da foto sob o círculo de [diameter] (tela), com a foto em [scale] e deslocada de [offset]. */
private fun crop(bitmap: Bitmap, diameter: Float, scale: Float, offset: Offset): Bitmap {
    val side = (diameter / scale).roundToInt().coerceIn(1, min(bitmap.width, bitmap.height))
    val centerX = bitmap.width / 2f - offset.x / scale
    val centerY = bitmap.height / 2f - offset.y / scale
    val left = (centerX - side / 2f).roundToInt().coerceIn(0, bitmap.width - side)
    val top = (centerY - side / 2f).roundToInt().coerceIn(0, bitmap.height - side)
    val square = Bitmap.createBitmap(bitmap, left, top, side, side)
    return Bitmap.createScaledBitmap(square, CROP_SIZE, CROP_SIZE, true)
}

/** Lê a foto já reduzida e na orientação certa (o ImageDecoder aplica a rotação da câmera). */
private fun loadBitmap(context: Context, uri: Uri): Bitmap {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val largest = max(info.size.width, info.size.height)
            if (largest > MAX_SOURCE_SIDE) decoder.setTargetSampleSize(largest / MAX_SOURCE_SIDE)
        }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val largest = max(bounds.outWidth, bounds.outHeight)
    val options = BitmapFactory.Options().apply { inSampleSize = max(1, largest / MAX_SOURCE_SIDE) }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("Foto ilegível")
}
