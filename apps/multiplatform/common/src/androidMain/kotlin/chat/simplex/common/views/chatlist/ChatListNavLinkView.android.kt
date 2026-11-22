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
      if (inbox && showMenu.value) RowActionPopup({ showMenu.value = false }, actions = dropdownMenuItems)
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
