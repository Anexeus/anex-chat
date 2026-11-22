package chat.simplex.common.views.helpers

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.chatlist.LocalChatMenuVisible

@Composable
internal fun RowActionPopup(dismiss: () -> Unit, anchorToItem: Boolean = false, actions: @Composable () -> Unit) {
  val visible = LocalChatMenuVisible.current
  val backdrop = LocalGlassBackdrop.current
  val haptic = LocalHapticFeedback.current
  DisposableEffect(Unit) {
    visible?.value = true
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    onDispose { visible?.value = false }
  }
  val margin = with(LocalDensity.current) { 12.dp.roundToPx() }
  val anchor = remember(margin, anchorToItem) { object : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
      val x = if (anchorToItem) (anchorBounds.center.x - popupContentSize.width / 2)
        .coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
      else if (layoutDirection == LayoutDirection.Ltr) margin else windowSize.width - popupContentSize.width - margin
      val below = anchorBounds.bottom + margin
      val y = if (below + popupContentSize.height <= windowSize.height - margin) below else anchorBounds.top - popupContentSize.height - margin
      return IntOffset(x, y.coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)))
    }
  } }
  Popup(anchor, onDismissRequest = dismiss, properties = PopupProperties(focusable = true)) {
    val scale = remember { Animatable(0.89f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.78f)) }
    BoxWithConstraints {
      AnexGlassSurface(Modifier.padding(6.dp).width(255.dp).heightIn(max = (maxHeight - 32.dp).coerceAtLeast(1.dp))
        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }, shape = RoundedCornerShape(24.dp),
        backdrop = backdrop, solid = backdrop == null) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
          CompositionLocalProvider(LocalGlassMenu provides true) { actions() }
        }
      }
    }
  }
}
