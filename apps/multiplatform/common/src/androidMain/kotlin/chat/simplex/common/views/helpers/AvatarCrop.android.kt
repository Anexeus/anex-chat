package chat.simplex.common.views.helpers

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import chat.simplex.common.ui.theme.*
import kotlin.math.roundToInt

@Composable
internal fun AvatarCrop(image: ImageBitmap, cancel: () -> Unit, done: (ImageBitmap) -> Unit) {
  Dialog(cancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
      val width = constraints.maxWidth.toFloat()
      val height = constraints.maxHeight.toFloat()
      val diameter = minOf(width * 0.90f, height * 0.72f)
      val center = Offset(width / 2, height / 2)
      val baseSide = minOf(image.width, image.height).toFloat()
      var zoom by rememberSaveable { mutableFloatStateOf(1f) }
      var cx by rememberSaveable { mutableFloatStateOf(image.width / 2f) }
      var cy by rememberSaveable { mutableFloatStateOf(image.height / 2f) }
      val side = baseSide / zoom
      val scale = diameter / side
      Canvas(Modifier.fillMaxSize().pointerInput(image, diameter) {
        detectTransformGestures { centroid, pan, change, _ ->
          val oldScale = diameter * zoom / baseSide
          val nextZoom = (zoom * change).coerceIn(1f, 8f)
          val newScale = diameter * nextZoom / baseSide
          val half = baseSide / (2 * nextZoom)
          val delta = (centroid - center) * (1f / oldScale - 1f / newScale) - pan / newScale
          cx = (cx + delta.x).coerceIn(half, image.width - half)
          cy = (cy + delta.y).coerceIn(half, image.height - half)
          zoom = nextZoom
        }
      }) {
        withTransform({
          translate(center.x - cx * scale, center.y - cy * scale)
          scale(scale, scale, Offset.Zero)
        }) { drawImage(image) }
        val rim = Path().apply { addOval(Rect(center, diameter / 2)) }
        clipPath(rim, ClipOp.Difference) { drawRect(Color.Black.copy(alpha = 0.62f)) }
        drawCircle(Color.White, diameter / 2, center, style = Stroke(1.dp.toPx()))
      }
      Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        @Composable fun Action(label: String, click: () -> Unit) {
          AnexGlassSurface(solid = !glassEnabled(), tint = Color(0xCA20252A)) {
            TextButton(click) { Text(label, color = Color.White) }
          }
        }
        Action("Cancel", cancel)
        Action("Done") {
          val length = side.roundToInt().coerceIn(1, baseSide.toInt())
          val x = (cx - side / 2).roundToInt().coerceIn(0, image.width - length)
          val y = (cy - side / 2).roundToInt().coerceIn(0, image.height - length)
          done(Bitmap.createBitmap(image.asAndroidBitmap(), x, y, length, length).asImageBitmap())
        }
      }
    }
  }
}
