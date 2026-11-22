package chat.simplex.common.views.chatlist

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.chat.item.CIFileViewScope
import chat.simplex.common.views.chat.item.ItemAction
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.*
import chat.simplex.common.views.usersettings.*
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal val LocalChatMenuVisible = staticCompositionLocalOf<MutableState<Boolean>?> { null }
internal val LocalAnexChatRow = staticCompositionLocalOf { false }

internal val chatListAvatarSize: Dp
  @Composable get() = (if (LocalAnexChatRow.current) 54.dp else 72.dp) * fontSizeSqrtMultiplier

internal val chatListAvatarGap: Dp
  @Composable get() = if (LocalAnexChatRow.current) 12.dp else 8.dp

@Composable
internal fun ChatListAvatar(content: @Composable (Dp) -> Unit) {
  val size = chatListAvatarSize
  if (LocalAnexChatRow.current) {
    Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colors.onBackground.copy(alpha = 0.055f)),
      contentAlignment = Alignment.Center) { content(size) }
  } else {
    content(size)
  }
}

@Composable
internal fun AndroidChatsScreen(
  search: MutableState<TextFieldValue>, list: LazyListState, picker: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit, stopped: Boolean
) {
  val glassEnabled = glassEnabled()
  val backdrop = if (glassEnabled) rememberGlassBackdrop((platform.androidApiLevel ?: 0) >= 31) else null
  val theme = MaterialTheme.colors
  val filter = chatModel.activeChatTagFilter
  val contacts = ActiveFilter.PresetTag(PresetTagKind.CONTACTS)
  val linkSearch = remember { mutableStateOf(false) }
  val linkMatches = remember { mutableStateOf(emptySet<String>()) }
  val candidate = remember { mutableStateOf<String?>(null) }
  val chats = filteredChats(linkSearch, linkMatches, search.value.text, chatModel.chats.value, filter.value)
  val scope = rememberCoroutineScope()
  val keyboard by getKeyboardState()
  val running = chatModel.chatRunning.value
  val pickerState by picker.collectAsState()
  val density = LocalDensity.current
  var barHeight by remember { mutableStateOf(70.dp) }
  val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  val barVisible by remember(list) { derivedStateOf { !list.lastScrolledForward || !list.canScrollBackward || !list.canScrollForward } }
  val openSettings = {
    ModalManager.start.showModalCloseable(cardScreen = true) { close -> SettingsView(chatModel, setPerformLA, close) }
    Unit
  }
  fun select(next: ActiveFilter?) {
    search.value = TextFieldValue()
    if (picker.value.isVisible()) picker.value = AnimatedViewState.HIDING
    filter.value = next
    scope.launch { if (list.layoutInfo.totalItemsCount > 0) list.scrollToItem(0) }
  }
  DisposableEffect(list) {
    onDispose { lazyListState = list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
  }

  val menuVisible = remember { mutableStateOf(false) }
  var headerHeight by remember { mutableStateOf(170.dp) }
  CompositionLocalProvider(LocalGlassBackdrop provides backdrop, LocalChatMenuVisible provides menuVisible) {
    Box(Modifier.fillMaxSize().background(theme.background).statusBarsPadding().imePadding()
      .then(if (menuVisible.value && glassEnabled && (platform.androidApiLevel ?: 0) >= 31) Modifier.blur(5.dp) else Modifier)) {
      Box(backdrop?.capture(Modifier.fillMaxSize()) ?: Modifier.fillMaxSize()) {
        when {
          running == null -> Text(stringResource(MR.strings.loading_chats), Modifier.align(Alignment.Center), color = theme.secondary)
          chats.isEmpty() -> Box(Modifier.align(Alignment.Center).padding(horizontal = 16.dp)) { NoChatsView(search) }
          else -> LazyColumn(Modifier.fillMaxSize(), state = list,
            contentPadding = PaddingValues(top = headerHeight + 4.dp, bottom = bottomInset + barHeight + 20.dp)) {
            items(chats, key = { it.remoteHostId to it.id }) { chat ->
              val nextSelected = remember { mutableStateOf(false) } // Only used in the desktop row.
              CompositionLocalProvider(LocalAnexChatRow provides true) {
                ChatListNavLinkView(chat, nextSelected)
              }
            }
          }
        }
      }
      Column(Modifier.fillMaxWidth().onSizeChanged { headerHeight = with(density) { it.height.toDp() } }
        .background(Brush.verticalGradient(listOf(theme.background, theme.background.copy(alpha = 0.94f),
          theme.background.copy(alpha = 0.80f), theme.background.copy(alpha = 0.22f))))) {
        if (keyboard != KeyboardState.Opened) {
          ChatsHeader(glassEnabled, stopped, running == true, openSettings)
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(CircleShape)
          .background(theme.onBackground.copy(alpha = if (theme.isLight) 0.065f else 0.10f).compositeOver(theme.background))) {
          ChatListSearchBar(list, search, linkSearch, linkMatches, candidate)
        }
        candidate.value?.let { ConnectByNameRow(it, search, candidate, close = null) }
        ChatListFilters(filter.value, ::select)
      }
      Box(Modifier.fillMaxSize().padding(bottom = if (pickerState.isGone()) 0.dp else barHeight + bottomInset + 8.dp)) {
        tryOrShowError("UserPicker", error = {}) {
          UserPicker(chatModel, picker, setPerformLA)
        }
      }
      // Fade the scrolling content before it passes under the floating controls.
      if (keyboard != KeyboardState.Opened) {
        androidx.compose.animation.AnimatedVisibility(barVisible || pickerState.isVisible(), Modifier.align(Alignment.BottomCenter),
          enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
          Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(
            theme.background.copy(alpha = 0f), theme.background.copy(alpha = 0.12f), theme.background.copy(alpha = 0.41f)
          ))).navigationBarsPadding().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center) {
            Box(Modifier.onSizeChanged { barHeight = with(density) { it.height.toDp() } }) {
              ChatListTabBar(backdrop, filter.value == contacts, pickerState.isVisible(),
                profileUnread = chatModel.users.any { !it.user.activeUser && !it.user.hidden && it.unreadCount > 0 },
                onChats = { select(null) }, onContacts = { select(contacts) },
                onProfile = { if (chatModel.users.isEmpty()) openSettings() else picker.value = AnimatedViewState.VISIBLE })
            }
          }
        }
      }
      if (menuVisible.value) Spacer(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.34f)))
    }
  }
}

@Composable
private fun ChatsHeader(glass: Boolean, stopped: Boolean, running: Boolean, openSettings: () -> Unit) {
  val more = remember { mutableStateOf(false) }
  val newChat = remember { mutableStateOf(false) }
  val oneHand = remember { appPrefs.oneHandUI.state }
  val showSummary = rememberShowServersSummary()
  val theme = MaterialTheme.colors
  Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically) {
    Text(stringResource(MR.strings.your_chats), Modifier.weight(1f), color = theme.onBackground,
      fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-1).sp,
      maxLines = 1, overflow = TextOverflow.Ellipsis)
    val update = chatModel.updatingProgress.value
    if (update != null) {
      IconButton({ chatModel.updatingRequest?.close() }) {
        if (update == -1f) CIFileViewScope.progressIndicator() else CIFileViewScope.progressCircle((update * 100).toLong(), 100)
      }
    } else if (connectProgressManager.showConnectProgress != null) {
      CIFileViewScope.progressIndicator()
    }
    if (stopped) {
      IconButton({ AlertManager.shared.showAlertMsg(generalGetString(MR.strings.chat_is_stopped_indication),
        generalGetString(MR.strings.you_can_start_chat_via_setting_or_by_restarting_the_app)) }) {
        Icon(painterResource(MR.images.ic_report_filled), stringResource(MR.strings.chat_is_stopped), tint = theme.error)
      }
    }
    Box {
      ChatListRoundButton(MR.images.ic_more_horiz, stringResource(MR.strings.icon_descr_more_button), glass) { more.value = true }
      DefaultDropdownMenu(more) {
        ItemAction(stringResource(MR.strings.settings_section_title_settings), painterResource(MR.images.ic_settings)) {
          more.value = false
          openSettings()
        }
        if (running) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            val open = {
              more.value = false
              showSummary()
            }
            SubscriptionStatusIndicator(open)
            Text(stringResource(MR.strings.settings_section_title_network_connection),
              Modifier.weight(1f).clickable(onClick = open).padding(vertical = 12.dp))
          }
        }
      }
    }
    Box {
      ChatListRoundButton(MR.images.ic_add, stringResource(MR.strings.new_chat), glass, accent = true, enabled = running && !stopped) {
        newChat.value = true
      }
      DefaultDropdownMenu(newChat) {
        fun openConnection(option: NewChatOption) {
          newChat.value = false
          ModalManager.start.showModalCloseable { close ->
            NewChatView(chatModel.currentRemoteHost.value, option, showQRCodeScanner = option == NewChatOption.CONNECT, close = close)
          }
        }
        ItemAction(stringResource(MR.strings.new_1_time_link), painterResource(MR.images.ic_add_link)) { openConnection(NewChatOption.INVITE) }
        ItemAction(stringResource(MR.strings.scan_paste_link), painterResource(MR.images.ic_qr_code_scanner)) { openConnection(NewChatOption.CONNECT) }
        ItemAction(stringResource(MR.strings.your_public_address), painterResource(MR.images.ic_qr_code)) {
          newChat.value = false
          ModalManager.start.showModalCloseable { close -> UserAddressView(chatModel, close = close) }
        }
        ItemAction(stringResource(MR.strings.new_chat), painterResource(MR.images.ic_forum)) {
          newChat.value = false
          showNewChatSheet(oneHand)
        }
      }
    }
  }
}

@Composable
private fun ChatListFilters(active: ActiveFilter?, select: (ActiveFilter?) -> Unit) {
  val rhId = chatModel.remoteHostId()
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    FilterPill(stringResource(MR.strings.chat_list_all), active == null, { select(null) })
    for (tag in PresetTagKind.entries) {
      val filter = ActiveFilter.PresetTag(tag)
      if (tag == PresetTagKind.CONTACTS || (chatModel.presetTags[tag] ?: 0) > 0 || active == filter) {
        FilterPill(stringResource(presetTagLabel(tag, active == filter).third), active == filter, { select(filter) })
      }
    }
    FilterPill(stringResource(MR.strings.anex_unread_chats), active == ActiveFilter.Unread, { select(ActiveFilter.Unread) })
    for (tag in chatModel.userTags.value) key(tag.chatTagId) {
      val filter = ActiveFilter.UserTag(tag)
      val menu = remember { mutableStateOf(false) }
      val saving = remember { mutableStateOf(false) }
      val label = listOfNotNull(tag.chatTagEmoji, tag.chatTagText).joinToString(" ")
      Box {
        FilterPill(label, active == filter, { select(filter) }, enabled = !saving.value,
          unread = (chatModel.unreadTags[tag.chatTagId] ?: 0) > 0, onLongClick = { menu.value = true })
        TagsDropdownMenu(rhId, tag, menu, saving)
      }
    }
    FilterPill(stringResource(MR.strings.chat_list_add_list), false, {
      ModalManager.start.showModalCloseable { close -> TagListEditor(rhId, close = close) }
    }, add = true)
  }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, click: () -> Unit, enabled: Boolean = true,
                       unread: Boolean = false, add: Boolean = false, onLongClick: (() -> Unit)? = null) {
  val theme = MaterialTheme.colors
  val fill = theme.onBackground.copy(alpha = if (selected) 0.18f else 0.065f)
  val action = if (onLongClick == null) Modifier.selectable(selected, enabled, role = Role.Button, onClick = click)
    else Modifier.combinedClickable(enabled = enabled, role = Role.Button, onLongClick = onLongClick, onClick = click)
  Box(Modifier.clip(CircleShape).then(action).heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
    Row(Modifier.background(fill, CircleShape).padding(horizontal = 13.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
      if (add) Icon(painterResource(MR.images.ic_add), null, Modifier.padding(end = 3.dp).size(15.dp), tint = theme.onBackground)
      Text(label, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp,
        color = theme.onBackground)
      if (unread) Spacer(Modifier.padding(start = 4.dp).size(6.dp).background(theme.primary, CircleShape))
    }
  }
}
