package chat.simplex.common.views.chatlist

import androidx.compose.animation.core.animateDpAsState
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
    AnexGlassSurface(Modifier.size(40.dp), solid = !glass,
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
  val ink = MaterialTheme.colors.onBackground
  val light = MaterialTheme.colors.isLight
  AnexGlassSurface(Modifier.widthIn(max = 360.dp).fillMaxWidth(), backdrop = backdrop, solid = backdrop == null) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(5.dp)) {
      val segment = maxWidth / 3
      val offset by animateDpAsState(segment * selected)
      val base = if (light) Color.White else Color(0xFF49494E)
      val lens = if (backdrop == null) Modifier.background(base, CircleShape) else {
        val shine = Brush.linearGradient(listOf(Color.White.copy(alpha = if (light) 0.95f else 0.36f),
          Color.Transparent, Color.White.copy(alpha = 0.14f)))
        Modifier.background(base.copy(alpha = 0.60f), CircleShape).border(1.dp, shine, CircleShape)
      }
      Box(Modifier.matchParentSize()) {
        Spacer(Modifier.offset(x = offset).width(segment).fillMaxHeight().then(lens))
      }
      Row(Modifier.fillMaxWidth().selectableGroup()) {
        @Composable
        fun RowScope.Tab(index: Int, icon: ImageResource, label: String, click: () -> Unit) {
          Column(
            Modifier.weight(1f).clip(CircleShape).selectable(selected == index, role = Role.Tab, onClick = click)
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
    }
  }
}
