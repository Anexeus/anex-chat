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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.model.GlassMode
import androidx.compose.material.LocalContentColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import chat.simplex.common.views.helpers.copyViewToAppBar

internal object AnexGlass {
  val pill = CircleShape
  val panel = RoundedCornerShape(24.dp)
  val margin = 14.dp
  val gap = 8.dp
  val verticalInset = 6.dp


  fun fill(light: Boolean) = if (light) Color(0xA8F5F6F8) else Color(0x8305090F)

}

@Composable
internal expect fun systemPrefersSolidGlass(): Boolean

@Composable
internal fun glassEnabled() = when (appPrefs.anexGlassMode.state.value) {
  GlassMode.Auto -> !systemPrefersSolidGlass()
  GlassMode.On -> true
  GlassMode.Off -> false
}

internal val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }
internal val LocalGlassConversation = staticCompositionLocalOf { false }
internal val LocalGlassMenu = staticCompositionLocalOf { false }

internal fun readableGlassInk(preferred: Color, surface: Color): Color {
  val light = surface.luminance()
  val ink = preferred.luminance()
  if ((maxOf(light, ink) + 0.05f) / (minOf(light, ink) + 0.05f) >= 4.5f) return preferred
  return if (light > 0.179f) Color.Black else Color.White
}

@Composable
internal expect fun glassOptics(shape: Shape, lens: Boolean, sharp: Boolean = false): Modifier

// The recorded planes contain content only. Controls replay them in screen coordinates.
@Stable
internal class GlassPlane(private val layer: GraphicsLayer) {
  var origin by mutableStateOf<Offset?>(null)
    private set

  fun capture(modifier: Modifier) = modifier.onGloballyPositioned { origin = it.positionOnScreen() }
    .copyViewToAppBar(1, layer)

  fun androidx.compose.ui.graphics.drawscope.DrawScope.paintAt(position: Offset) {
    val source = origin ?: return
    if (!source.isSpecified || !position.isSpecified || layer.isReleased || layer.size == IntSize.Zero) return
    translate(source.x - position.x, source.y - position.y) { drawLayer(layer) }
  }
}

@Composable
internal fun rememberGlassPlane(): GlassPlane {
  val layer = rememberGraphicsLayer()
  return remember(layer) { GlassPlane(layer) }
}

@Composable
internal fun BoxScope.LensGlyphs(plane: GlassPlane) {
  var position by remember { mutableStateOf<Offset?>(null) }
  Box(Modifier.matchParentSize().onGloballyPositioned { position = it.positionOnScreen() }
    .then(glassOptics(AnexGlass.pill, lens = true, sharp = true)).drawBehind {
      position?.let { target -> with(plane) { paintAt(target) } }
    })
}

@Stable
internal class GlassBackdrop(private val base: GlassPlane?, private val messages: GlassPlane?) {
  fun capture(modifier: Modifier) = base?.capture(modifier) ?: modifier
  fun captureMessages(modifier: Modifier) = messages?.capture(modifier) ?: modifier

  @Composable
  fun BoxScope.FrostedLayer(background: Color, shape: Shape, lens: Boolean, includeMessages: Boolean) {
    if (base == null) return
    var position by remember { mutableStateOf<Offset?>(null) }
    Box(Modifier.matchParentSize().onGloballyPositioned { position = it.positionOnScreen() }
      .then(glassOptics(shape, lens)).drawBehind {
        val target = position ?: return@drawBehind
        drawRect(background)
        with(base) { paintAt(target) }
        if (includeMessages && messages != null) with(messages) { paintAt(target) }
      })
  }
}

@Composable
internal fun rememberGlassBackdrop(blur: Boolean, conversation: Boolean = false): GlassBackdrop {
  val base = if (blur) rememberGlassPlane() else null
  val messages = if (blur && conversation) rememberGlassPlane() else null
  return remember(base, messages) { GlassBackdrop(base, messages) }
}

@Composable
internal fun AnexGlassSurface(
  modifier: Modifier = Modifier,
  shape: Shape = AnexGlass.pill,
  backdrop: GlassBackdrop? = null,
  solid: Boolean = false,
  tint: Color = Color.Unspecified,
  lens: Boolean = false,
  includeMessages: Boolean = true,
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
  // Keep the body clear. Most of the reflected light belongs to the curved edge.
  val rim = Brush.linearGradient(
    0f to Color.White.copy(alpha = if (lens) 0.50f else 0.26f),
    0.26f to Color.White.copy(alpha = 0.065f),
    0.59f to Color.Black.copy(alpha = 0.16f),
    0.88f to Color.White.copy(alpha = if (lens) 0.31f else 0.12f),
    1f to Color.White.copy(alpha = 0.055f)
  )
  Box(modifier.shadow(if (lens) 1.dp else 4.dp, shape, clip = false).clip(shape).border(0.65.dp, rim, shape)) {
    backdrop?.run { FrostedLayer(background, shape, lens, includeMessages) }
    Box(Modifier.matchParentSize().background(fill))
    CompositionLocalProvider(LocalContentColor provides readableGlassInk(MaterialTheme.colors.onBackground, fill.compositeOver(background))) {
      content()
    }
  }
}

@Composable
internal fun ConversationGlass(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
  val backdrop = LocalGlassBackdrop.current
  AnexGlassSurface(modifier, backdrop = backdrop, solid = backdrop == null, content = content)
}
