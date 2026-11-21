package chat.simplex.common.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import chat.simplex.common.views.helpers.copyViewToAppBar

internal object AnexGlass {
  val pill = CircleShape
  val panel = RoundedCornerShape(24.dp)
  val margin = 14.dp
  val gap = 8.dp
  val verticalInset = 6.dp
  val blurRadius = 18.dp

  fun fill(light: Boolean) = if (light) Color(0xB9F1F1F3) else Color(0xA62A2A2D)
  fun rim(light: Boolean) = if (light) Color(0xBFFFFFFF) else Color(0x60FFFFFF)
  fun edge(light: Boolean) = if (light) Color(0x35555658) else Color(0x24FFFFFF)
  fun sheen(light: Boolean) = if (light) Color(0x50FFFFFF) else Color(0x15FFFFFF)
  fun lens(light: Boolean) = if (light) Color(0x845AC0FF) else Color(0x8C35618C)
}

@Composable
internal expect fun systemPrefersSolidGlass(): Boolean

@Stable
internal class GlassBackdrop(private val layer: GraphicsLayer?) {
  private var origin by mutableStateOf<Offset?>(null)

  fun capture(modifier: Modifier): Modifier = if (layer == null) modifier else modifier
    .onGloballyPositioned { origin = it.positionInRoot() }
    .copyViewToAppBar(1, layer)

  @Composable
  fun BoxScope.FrostedLayer(background: Color) {
    val captured = layer ?: return
    var position by remember { mutableStateOf<Offset?>(null) }
    Box(
      Modifier.matchParentSize()
        .onGloballyPositioned { position = it.positionInRoot() }
        .blur(AnexGlass.blurRadius, BlurredEdgeTreatment.Rectangle)
        .drawBehind {
          val source = origin ?: return@drawBehind
          val target = position ?: return@drawBehind
          if (captured.size != IntSize.Zero && !captured.isReleased) {
            drawRect(background)
            translate(source.x - target.x, source.y - target.y) { drawLayer(captured) }
          }
        }
    )
  }
}

@Composable
internal fun rememberGlassBackdrop(blur: Boolean): GlassBackdrop {
  val layer = if (blur) rememberGraphicsLayer() else null
  return remember(layer) { GlassBackdrop(layer) }
}

@Composable
internal fun AnexGlassSurface(
  modifier: Modifier = Modifier,
  shape: Shape = AnexGlass.pill,
  backdrop: GlassBackdrop? = null,
  lens: Boolean = false,
  solid: Boolean = false,
  tint: Color = Color.Unspecified,
  content: @Composable BoxScope.() -> Unit
) {
  val light = MaterialTheme.colors.isLight
  val background = MaterialTheme.colors.background
  val fill = if (tint != Color.Unspecified) tint else AnexGlass.fill(light)
  if (solid) {
    val opaque = if (tint != Color.Unspecified) tint.copy(alpha = 1f)
      else if (light) Color(0xFFF0F0F2) else Color(0xFF28282B)
    Box(modifier.shadow(3.dp, shape).clip(shape).background(opaque)
      .border(1.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.32f), shape), content = content)
    return
  }
  val rim = Brush.verticalGradient(listOf(AnexGlass.rim(light), AnexGlass.edge(light)))
  val highlight = Brush.verticalGradient(
    0f to AnexGlass.sheen(light),
    0.55f to Color.Transparent,
    1f to AnexGlass.sheen(light).copy(alpha = 0.07f)
  )
  Box(
    modifier
      .then(if (lens) Modifier else Modifier.shadow(5.dp, shape, clip = false))
      .clip(shape)
      .border(1.dp, rim, shape)
  ) {
    backdrop?.run { FrostedLayer(background) }
    Box(
      Modifier.matchParentSize()
        .background(if (lens) AnexGlass.lens(light) else fill)
        .background(highlight)
    )
    content()
  }
}
