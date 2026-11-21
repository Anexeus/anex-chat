package chat.simplex.common.ui.theme

import android.app.UiModeManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun systemPrefersSolidGlass(): Boolean {
  val context = LocalContext.current
  var solid by remember(context) { mutableStateOf(true) }
  DisposableEffect(context) {
    val resolver = context.contentResolver
    val accessibility = context.getSystemService(AccessibilityManager::class.java)
    val uiMode = context.getSystemService(UiModeManager::class.java)
    val reduceTransparency = listOf("reduce_transparency", "accessibility_reduce_transparency")

    fun enabled(readSetting: () -> Int): Boolean = try {
      readSetting() == 1
    } catch (_: SecurityException) {
      // Some Android builds restrict access to manufacturer settings.
      false
    }

    fun refresh() {
      val highContrastText = (Build.VERSION.SDK_INT >= 36 && accessibility?.isHighContrastTextEnabled == true) ||
        enabled { Settings.Secure.getInt(resolver, "high_text_contrast_enabled", 0) }
      val increasedContrast = Build.VERSION.SDK_INT >= 34 && (uiMode?.contrast ?: 0f) > 0f
      val reducedTransparency = reduceTransparency.any { key ->
        enabled { Settings.Global.getInt(resolver, key, 0) } ||
          enabled { Settings.Secure.getInt(resolver, key, 0) } ||
          enabled { Settings.System.getInt(resolver, key, 0) }
      }
      solid = highContrastText || increasedContrast || reducedTransparency ||
        enabled { Settings.Global.getInt(resolver, "disable_window_blurs", 0) }
    }

    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) = refresh()
    }
    val settings = listOf(
      Settings.Secure.getUriFor("high_text_contrast_enabled"),
      Settings.Global.getUriFor("disable_window_blurs")
    ) + reduceTransparency.flatMap { key ->
      listOf(Settings.Global.getUriFor(key), Settings.Secure.getUriFor(key), Settings.System.getUriFor(key))
    }
    settings.forEach { uri ->
      try {
        resolver.registerContentObserver(uri, false, observer)
      } catch (_: SecurityException) {
        // A restricted setting cannot be observed on this Android build.
      }
    }

    val contrastListener = if (Build.VERSION.SDK_INT >= 34) {
      UiModeManager.ContrastChangeListener { refresh() }.also {
        uiMode?.addContrastChangeListener(context.mainExecutor, it)
      }
    } else null
    val textContrastListener = if (Build.VERSION.SDK_INT >= 36) {
      AccessibilityManager.HighContrastTextStateChangeListener { refresh() }.also {
        accessibility?.addHighContrastTextStateChangeListener(context.mainExecutor, it)
      }
    } else null

    refresh()
    onDispose {
      resolver.unregisterContentObserver(observer)
      if (Build.VERSION.SDK_INT >= 34 && contrastListener != null) {
        uiMode?.removeContrastChangeListener(contrastListener)
      }
      if (Build.VERSION.SDK_INT >= 36 && textContrastListener != null) {
        accessibility?.removeHighContrastTextStateChangeListener(textContrastListener)
      }
    }
  }
  return solid
}
