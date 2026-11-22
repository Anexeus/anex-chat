package chat.simplex.common.views.database

import SectionTextFooter
import SectionView
import androidx.compose.material.*
import androidx.compose.runtime.*
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.*
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.usersettings.SettingsActionItem
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun showArchiveScreen(restore: Boolean) {
  ModalManager.fullscreen.showModal(cardScreen = true, settingsGlass = true) { ArchiveScreen(restore) }
}

@Composable
private fun ArchiveScreen(restore: Boolean) {
  val progress = remember { mutableStateOf(false) }
  val lastStart = remember { mutableStateOf(appPrefs.chatLastStart.get()) }
  val title = if (restore) "Restore from backup" else "Back up chats"
  val needsPassphrase = chatModel.chatDbEncrypted.value != true || appPrefs.initialRandomDBPassphrase.state.value
  val picker = rememberFileChooserLauncher(getContent = restore) { uri ->
    if (uri != null) {
      val wasStopped = chatModel.chatRunning.value != true
      if (restore) {
        AlertManager.shared.showAlertDialog(
          title = "Restore this backup?",
          text = "This replaces all chats and profiles on this phone. You will need the database passphrase used when the backup was made.",
          confirmText = "Restore",
          destructive = true,
          onConfirm = {
            stopChatRunBlockStartChat(wasStopped, lastStart, progress) {
              val restored = importArchive(uri, mutableStateOf(0 to 0L), progress, migration = false, openAfterRestore = true)
              // Restore opens the imported database itself. Resume the original if the file was rejected.
              !restored && !chatModel.chatDbChanged.value
            }
          }
        )
      } else {
        stopChatRunBlockStartChat(wasStopped, lastStart, progress) {
          progress.value = true
          try {
            saveBackup(uri)
          } finally {
            progress.value = false
          }
          true
        }
      }
    }
  }
  ColumnWithScrollBar {
    AppBarTitle(title)
    SectionTextFooter(if (restore) "Choose your backup ZIP from this phone, a USB drive, or a storage app in the Android file picker."
      else "This saves all profiles on this phone. Choose a private folder, USB drive, or cloud storage app in the Android file picker. Your database is encrypted with its passphrase. The ZIP can also contain readable attachments.")
    SectionView {
      if (!restore && needsPassphrase) {
        SettingsActionItem(painterResource(MR.images.ic_lock), "Set database passphrase", {
          ModalManager.fullscreen.showModal(cardScreen = true, settingsGlass = true) { DatabaseEncryptionView(chatModel, false) }
        }, disabled = progress.value)
      } else {
        SettingsActionItem(painterResource(if (restore) MR.images.ic_download else MR.images.ic_ios_share),
          if (restore) "Choose backup ZIP" else "Choose where to save", {
            withLongRunningApi {
              val date = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
              picker.launch(if (restore) "application/zip" else "Anex-Chat-$date.zip")
            }
          }, disabled = progress.value)
      }
    }
    if (!restore && needsPassphrase) SectionTextFooter("Choose a passphrase you can enter when restoring. Then return here to save your backup.")
    if (progress.value) LinearProgressIndicator()
  }
}

private suspend fun saveBackup(destination: URI) {
  val staging = Files.createTempDirectory(databaseExportDir.toPath(), "anex-backup-").toFile()
  try {
    val (path, errors) = exportChatArchive(chatModel, staging, mutableStateOf(null))
    withContext(Dispatchers.IO) { writeFileToUri(File(path), destination) }
    if (errors.isEmpty()) {
      appPrefs.lastSavedBackup.set(Clock.System.now())
      withContext(Dispatchers.Main) { showToast(generalGetString(MR.strings.file_saved)) }
    } else {
      AlertManager.shared.showAlertMsg("Backup saved with missing files", "Your databases were saved, but some attachments could not be included.")
    }
  } catch (e: Exception) {
    AlertManager.shared.showAlertMsg("Could not save backup", "Check the available space and try again.")
  } finally {
    staging.deleteRecursively()
  }
}
