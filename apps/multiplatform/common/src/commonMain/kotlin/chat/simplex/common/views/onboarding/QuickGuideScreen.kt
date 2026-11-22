package chat.simplex.common.views.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.ColumnWithScrollBar
import chat.simplex.common.platform.appPlatform
import chat.simplex.common.ui.theme.AnexGlassSurface
import chat.simplex.common.ui.theme.glassEnabled
import chat.simplex.common.views.helpers.*

enum class QuickGuideState { NotShown, Pending, Seen }

internal fun queueQuickGuide() {
  if (appPlatform.isAndroid && appPrefs.quickGuide.get() == QuickGuideState.NotShown) {
    appPrefs.quickGuide.set(QuickGuideState.Pending)
  }
}

private val guideCards = listOf(
  "Add a friend" to "On Chats, tap + beside Search. Show My code to your friend, or scan or paste the code they send you.",
  "Start a chat" to "After you connect, open the Contacts tab and tap your friend’s name to write a message.",
  "Send a photo" to "In a chat, tap + beside the message box. Choose a photo from your gallery or take one, then tap Send.",
  "Find Settings" to "Tap You at the bottom, then Settings. You can open this guide again from Help & support."
)

@Composable
internal fun QuickGuideScreen(firstTime: Boolean, close: () -> Unit) {
  var page by rememberSaveable { mutableIntStateOf(0) }
  val (title, body) = guideCards[page]
  val last = page == guideCards.lastIndex
  CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
    ModalView(close = close, glassNavigation = true) {
      ColumnWithScrollBar {
        AppBarTitle("Quick guide")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Step ${page + 1} of ${guideCards.size}", style = MaterialTheme.typography.caption)
          Text(title, style = MaterialTheme.typography.h3)
          Text(body, style = MaterialTheme.typography.body1)
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { if (page == 0) close() else page-- }) {
              Text(if (page > 0) "Previous" else if (firstTime) "Skip" else "Close")
            }
            AnexGlassSurface(solid = !glassEnabled()) {
              TextButton(onClick = { if (last) close() else page++ }) { Text(if (last) "Done" else "Next") }
            }
          }
          if (firstTime && page > 0) TextButton(onClick = close) { Text("Skip") }
        }
      }
    }
  }
}
