package chat.simplex.common.views.chatlist

import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlin.math.roundToInt
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.platform.platform
import chat.simplex.res.*
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
internal fun ChatListRoundButton(icon: ImageResource, description: String, glass: Boolean,
                                accent: Boolean = false, enabled: Boolean = true, click: () -> Unit) {
  val color = MaterialTheme.colors.primary
  val prominent = accent && enabled
  val ink = when {
    !enabled -> MaterialTheme.colors.secondary
    !prominent -> MaterialTheme.colors.onBackground
    color.luminance() > 0.179f -> Color.Black
    else -> Color.White
  }
  Box(Modifier.size(48.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = click),
    contentAlignment = Alignment.Center) {
    AnexGlassSurface(Modifier.size(40.dp), solid = !glass, backdrop = LocalGlassBackdrop.current,
      tint = if (prominent) color.copy(alpha = 0.94f) else Color.Unspecified) {
      Icon(painterResource(icon), description, Modifier.align(Alignment.Center).size(23.dp), tint = ink)
    }
  }
}

@Composable
internal fun ChatListTabBar(
  backdrop: GlassBackdrop?, contacts: Boolean, profileOpen: Boolean, profileUnread: Boolean,
  onChats: () -> Unit, onContacts: () -> Unit, onProfile: () -> Unit
) {
  val selected = when {
    profileOpen -> 2
    contacts -> 1
    else -> 0
  }
  val ink = readableGlassInk(MaterialTheme.colors.onBackground, AnexGlass.fill(MaterialTheme.colors.isLight).compositeOver(MaterialTheme.colors.background))
  val light = MaterialTheme.colors.isLight
  AnexGlassSurface(Modifier.widthIn(max = 360.dp).fillMaxWidth(), backdrop = backdrop, solid = backdrop == null) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(5.dp)) {
      val segment = maxWidth / 3
      val presses = remember { List(3) { MutableInteractionSource() } }
      val pressed = presses.map { it.collectIsPressedAsState().value }.indexOf(true)
      var drag by remember { mutableStateOf<Float?>(null) }
      val segmentPx = with(LocalDensity.current) { segment.toPx().coerceAtLeast(1f) }
      val target = drag ?: (if (pressed >= 0 && backdrop != null) pressed else selected).toFloat()
      val springPosition by animateFloatAsState(target, spring(dampingRatio = 0.80f, stiffness = 580f))
      val position = drag ?: springPosition
      val held = backdrop != null && (drag != null || pressed >= 0)
      val lift by animateFloatAsState(if (held) 1f else 0f, spring(stiffness = 640f))
      val glyphs = if (backdrop != null && (platform.androidApiLevel ?: 0) >= 31)
        rememberGlassPlane() else null
      val drawLens: @Composable () -> Unit = {
        Box(Modifier.matchParentSize()) {
          val frame = Modifier.offset { IntOffset((position * segmentPx).roundToInt(), 0) }.width(segment).fillMaxHeight()
            .graphicsLayer { scaleX = 1f + 0.045f * lift; scaleY = 1f + 0.065f * lift }
          if (backdrop == null) {
            Spacer(frame.background(if (light) Color.White else Color(0xFF49494E), CircleShape))
          } else {
            val tint = if (light) Color.White.copy(alpha = 0.57f) else Color(0xFF8792A3).copy(alpha = 0.16f + 0.08f * lift)
            AnexGlassSurface(frame, backdrop = backdrop, tint = tint, lens = true) { if (glyphs != null) LensGlyphs(glyphs) }
          }
        }
      }
      if (glyphs == null) drawLens()
      val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
      Row((glyphs?.capture(Modifier.fillMaxWidth()) ?: Modifier.fillMaxWidth()).selectableGroup().draggable(
        state = rememberDraggableState { delta ->
          drag = ((drag ?: selected.toFloat()) + delta / segmentPx).coerceIn(0f, 2f)
        },
        orientation = Orientation.Horizontal, reverseDirection = rtl,
        onDragStarted = { point ->
          val index = (point.x / segmentPx - 0.5f).coerceIn(0f, 2f)
          drag = if (rtl) 2f - index else index
        },
        onDragStopped = {
          val destination = drag?.roundToInt()
          drag = null
          when (destination) {
            0 -> onChats()
            1 -> onContacts()
            2 -> onProfile()
          }
        }
      )) {
        @Composable
        fun RowScope.Tab(index: Int, icon: ImageResource, label: String, click: () -> Unit) {
          Column(
            Modifier.weight(1f).clip(CircleShape).selectable(selected == index, interactionSource = presses[index], indication = null, role = Role.Tab, onClick = click)
              .heightIn(min = 49.dp).padding(horizontal = 2.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box {
              Icon(painterResource(icon), null, Modifier.size(22.dp), tint = ink)
              if (index == 2 && profileUnread) {
                Spacer(Modifier.align(Alignment.TopEnd).size(7.dp).background(MaterialTheme.colors.primary, CircleShape))
              }
            }
            Text(label, color = ink, fontSize = 11.sp, lineHeight = 14.sp,
              fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Medium,
              maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
        Tab(0, MR.images.ic_forum, stringResource(MR.strings.your_chats), onChats)
        Tab(1, MR.images.ic_group, stringResource(MR.strings.chat_list_contacts), onContacts)
        Tab(2, MR.images.ic_account_circle_filled, stringResource(MR.strings.settings_section_title_you), onProfile)
      }
      if (glyphs != null) drawLens()
    }
  }
}
