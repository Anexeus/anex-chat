package chat.simplex.common.views.chatlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.MaterialTheme
import kotlin.math.abs
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import chat.simplex.common.ui.theme.*
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.simplex.common.platform.onRightClick
import chat.simplex.common.views.helpers.*

@Composable
actual fun ChatListNavLinkLayout(
  chatLinkPreview: @Composable () -> Unit,
  click: () -> Unit,
  dropdownMenuItems: (@Composable () -> Unit)?,
  showMenu: MutableState<Boolean>,
  disabled: Boolean,
  selectedChat: State<Boolean>,
  nextChatSelected: State<Boolean>,
) {
  var modifier = Modifier.fillMaxWidth()

  if (!disabled) modifier = modifier
    .combinedClickable(onClick = click, onLongClick = { showMenu.value = true })
    .onRightClick { showMenu.value = true }
  val inbox = LocalAnexChatRow.current
  if (inbox && !disabled && dropdownMenuItems != null) {
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    var distance by remember { mutableFloatStateOf(0f) }
    modifier = modifier.draggable(
      state = rememberDraggableState { distance += it },
      orientation = Orientation.Horizontal,
      onDragStarted = { distance = 0f },
      onDragStopped = { if (abs(distance) >= threshold) showMenu.value = true }
    )
  }
  Box(modifier) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = if (inbox) 16.dp else 8.dp, end = if (inbox) 16.dp else 12.dp, top = 8.dp, bottom = 8.dp),
      verticalAlignment = if (inbox) Alignment.CenterVertically else Alignment.Top
    ) {
      chatLinkPreview()
    }
    if (dropdownMenuItems != null) {
      if (inbox && showMenu.value) RowActionPopup({ showMenu.value = false }, dropdownMenuItems)
      else if (!inbox) DefaultDropdownMenu(showMenu, dropdownMenuItems = dropdownMenuItems)
    }
  }
  if (inbox) {
    Divider(Modifier.padding(start = 16.dp + chatListAvatarSize + chatListAvatarGap, end = 16.dp),
      color = MaterialTheme.colors.onBackground.copy(alpha = 0.14f), thickness = 0.5.dp)
  } else {
    Divider(Modifier.padding(horizontal = 8.dp))
  }
}

@Composable
private fun RowActionPopup(dismiss: () -> Unit, actions: @Composable () -> Unit) {
  val visible = LocalChatMenuVisible.current
  val backdrop = LocalGlassBackdrop.current
  val haptic = LocalHapticFeedback.current
  DisposableEffect(Unit) {
    visible?.value = true
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    onDispose { visible?.value = false }
  }
  val margin = with(LocalDensity.current) { 12.dp.roundToPx() }
  val anchor = remember(margin) { object : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
      val x = if (layoutDirection == LayoutDirection.Ltr) margin else windowSize.width - popupContentSize.width - margin
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
