package chat.simplex.common.views.chatlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.chatModel
import chat.simplex.common.platform.LazyColumnWithScrollBar
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.call.CallMediaType
import chat.simplex.common.views.chat.startChatCall
import chat.simplex.common.views.helpers.*
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.painterResource
import kotlinx.coroutines.CancellationException

@Composable
internal fun CallsHistory(modifier: Modifier, bottom: Dp) {
  val user = chatModel.currentUser.value ?: return
  val host = chatModel.currentRemoteHost.value?.remoteHostId
  key(host, user.userId) {
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
        Text("Calls", Modifier.weight(1f), style = MaterialTheme.typography.h1)
        CompositionLocalProvider(LocalGlassBackdrop provides null) {
          ChatListRoundButton(MR.images.ic_add, "New call", glass, accent = true, enabled = canBeginCall()) {
            ModalManager.start.showModalCloseable(glassNavigation = true) { close -> NewCallPicker(host, close) }
          }
        }
      }
      LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
        items(history, key = { it.chatItem.id }) { entry ->
          val info = entry.chatInfo as ChatInfo.Direct
          val current = contacts.firstOrNull { it.id == info.id }
          val received = entry.chatItem.content as? CIContent.RcvCall
          val missed = received?.status == CICallStatus.Missed
          val direction = when { missed -> "↙ Missed"; received != null -> "↙ Incoming"; else -> "↗ Outgoing" }
          CallRow(current ?: info, direction + " · " + entry.chatItem.timestampText,
            current != null && callable(current), missed = missed) {
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

private fun canBeginCall() = chatModel.activeCall.value == null && chatModel.chatRunning.value == true

private fun callable(info: ChatInfo.Direct) = canBeginCall() && info.contact.ready && info.contact.active &&
  info.contact.mergedPreferences.calls.enabled.forUser

@Composable
private fun NewCallPicker(host: Long?, close: () -> Unit) {
  var query by rememberSaveable { mutableStateOf("") }
  var starting by remember { mutableStateOf(false) }
  val matches = chatModel.chats.value.mapNotNull { it.chatInfo as? ChatInfo.Direct }
    .filter { callable(it) && it.contact.chatViewName.contains(query.trim(), ignoreCase = true) }
  val oneHand by appPrefs.oneHandUI.state
  val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val barHeight = AppBarHeight * fontSizeSqrtMultiplier
  LazyColumnWithScrollBar(
    modifier = Modifier.imePadding(),
    contentPadding = PaddingValues(
      start = 16.dp, end = 16.dp,
      top = status + if (oneHand) 12.dp else barHeight,
      bottom = nav + if (oneHand) barHeight else 12.dp,
    )
  ) {
    item {
      AppBarTitle("New call", withPadding = false)
      OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(bottom = 8.dp),
        singleLine = true, label = { Text("Search contacts") })
      if (matches.isEmpty()) Text("No callable contacts found.", Modifier.padding(vertical = 20.dp))
    }
    items(matches, key = { it.contact.contactId }) { info ->
      CallRow(info, "", enabled = !starting, selectRow = true) {
        if (!starting && callable(info)) {
          starting = true
          close()
          startChatCall(host, info, CallMediaType.Audio)
        }
      }
    }
  }
}

@Composable
private fun CallRow(info: ChatInfo.Direct, detail: String, enabled: Boolean, missed: Boolean = false, selectRow: Boolean = false, call: () -> Unit) {
  Row(Modifier.fillMaxWidth().then(if (selectRow) Modifier.clickable(enabled = enabled, onClick = call) else Modifier).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    ProfileImage(48.dp, info.contact.profile.image, icon = info.contact.chatIconName)
    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
      Text(info.contact.chatViewName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      if (detail.isNotEmpty()) Text(detail, color = if (missed) MaterialTheme.colors.error else MaterialTheme.colors.secondary)
    }
    if (selectRow) {
      Icon(painterResource(MR.images.ic_call_500), null, Modifier.padding(horizontal = 12.dp))
    } else IconButton(call, enabled = enabled) {
      Icon(painterResource(MR.images.ic_call_500), "Call " + info.contact.chatViewName)
    }
  }
  Divider(Modifier.padding(start = 60.dp))
}
