package chat.simplex.common.views.chatlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import chat.simplex.common.model.*
import chat.simplex.common.platform.chatModel
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.call.CallMediaType
import chat.simplex.common.views.chat.startChatCall
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.painterResource
import kotlinx.coroutines.CancellationException

@Composable
internal fun CallsHistory(modifier: Modifier, bottom: Dp) {
  val user = chatModel.currentUser.value ?: return
  val host = chatModel.currentRemoteHost.value?.remoteHostId
  key(host, user.userId) {
    var picking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<AChatItem>() }
    var before by remember { mutableStateOf<Long?>(null) }
    var more by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var request by remember { mutableIntStateOf(0) }
    LaunchedEffect(request) {
      loading = true
      failed = false
      try {
        val page = chatModel.controller.callHistoryPage(host, user.userId, before)
        if (page == null) failed = true else {
          val known = history.map { it.chatItem.id }.toHashSet()
          history.addAll(page.asReversed().filter {
            it.chatInfo is ChatInfo.Direct && (it.chatItem.content is CIContent.RcvCall ||
              it.chatItem.content is CIContent.SndCall) && known.add(it.chatItem.id)
          })
          more = page.size == 50
          before = page.firstOrNull()?.chatItem?.id
        }
      } catch (e: CancellationException) { throw e }
      catch (e: Exception) { failed = true }
      finally { loading = false }
    }
    val contacts = chatModel.chats.value.mapNotNull { it.chatInfo as? ChatInfo.Direct }
    val glass = glassEnabled()
    Column(modifier.padding(horizontal = 16.dp)) {
      Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (picking) "New call" else "Calls", Modifier.weight(1f), style = MaterialTheme.typography.h1)
        CompositionLocalProvider(LocalGlassBackdrop provides null) {
          ChatListRoundButton(if (picking) MR.images.ic_close else MR.images.ic_add,
          if (picking) "Close contact picker" else "New call", glass) { picking = !picking; query = "" }
        }
      }
      fun callable(info: ChatInfo.Direct) = info.contact.ready && info.contact.active &&
        info.contact.mergedPreferences.calls.enabled.forUser && chatModel.activeCall.value == null &&
        chatModel.chatRunning.value == true
      if (picking) OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(),
        singleLine = true, label = { Text("Search contacts") })
      LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
        if (picking) {
          val matches = contacts.filter { callable(it) && it.contact.chatViewName.contains(query, ignoreCase = true) }
          if (matches.isEmpty()) item { Text("No available contacts", Modifier.padding(vertical = 20.dp)) }
          items(matches, key = { it.contact.contactId }) { info ->
            CallRow(info, "", true) { startChatCall(host, info, CallMediaType.Audio) }
          }
        } else {
          items(history, key = { it.chatItem.id }) { entry ->
            val info = entry.chatInfo as ChatInfo.Direct
            val current = contacts.firstOrNull { it.id == info.id }
            val received = entry.chatItem.content as? CIContent.RcvCall
            val missed = received?.status == CICallStatus.Missed
            val direction = when { missed -> "↙ Missed"; received != null -> "↙ Incoming"; else -> "↗ Outgoing" }
            CallRow(current ?: info, direction + " · " + entry.chatItem.timestampText,
              current != null && callable(current), missed) {
              if (current != null) startChatCall(host, current, CallMediaType.Audio)
            }
          }
          item {
            when {
              loading -> CircularProgressIndicator(Modifier.padding(16.dp).size(24.dp))
              failed -> TextButton({ request++ }) { Text("Couldn’t load calls. Retry") }
              more -> TextButton({ request++ }) { Text("Load older calls") }
              history.isEmpty() -> Text("No calls yet. Tap + to call a contact.", Modifier.padding(vertical = 20.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CallRow(info: ChatInfo.Direct, detail: String, enabled: Boolean, missed: Boolean = false, call: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    ProfileImage(48.dp, info.contact.profile.image, icon = info.contact.chatIconName)
    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
      Text(info.contact.chatViewName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      if (detail.isNotEmpty()) Text(detail, color = if (missed) MaterialTheme.colors.error else MaterialTheme.colors.secondary)
    }
    IconButton(call, enabled = enabled) { Icon(painterResource(MR.images.ic_call_500), "Call " + info.contact.chatViewName) }
  }
  Divider(Modifier.padding(start = 60.dp))
}
