package chat.simplex.common.views.helpers

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.chat.item.CenteredRowLayout
import chat.simplex.res.*
import kotlin.math.absoluteValue

@Composable
fun DefaultAppBar(
  navigationButton: (@Composable RowScope.() -> Unit)? = null,
  title: (@Composable () -> Unit)? = null,
  fixedTitleText: String? = null,
  onTitleClick: (() -> Unit)? = null,
  onTop: Boolean,
  showSearch: Boolean = false,
  searchAlwaysVisible: Boolean = false,
  searchPlaceholder: String? = null,
  onSearchValueChanged: (String) -> Unit = {},
  searchTrailingContent: @Composable (() -> Unit)? = null,
  solidBackground: Boolean = false,
  buttons: @Composable RowScope.() -> Unit = {},
) {
  // If I just disable clickable modifier when don't need it, it will stop passing clicks to search. Replacing the whole modifier
  val modifier = if (!showSearch) {
    Modifier.clickable(enabled = onTitleClick != null, onClick = onTitleClick ?: { })
  } else if (!onTop) Modifier.imePadding()
  else Modifier

  val themeBackgroundMix = MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.97f)
  val prefAlpha = remember { appPrefs.inAppBarsAlpha.state }
  val handler = LocalAppBarHandler.current
  val connection = LocalAppBarHandler.current?.connection
  val titleText = remember(handler?.title?.value, fixedTitleText) {
    if (fixedTitleText != null) {
      mutableStateOf(fixedTitleText)
    } else {
      handler?.title ?: mutableStateOf("")
    }
  }
  val keyboardInset = WindowInsets.ime
  val settingsGlass = LocalSettingsGlass.current
  val barInsets = if (onTop) Modifier.statusBarsPadding() else Modifier.navigationBarsPadding()
  Box(modifier.then(if (settingsGlass) barInsets.padding(horizontal = 10.dp, vertical = 2.dp) else Modifier)) {
    val density = LocalDensity.current
    val blurRadius = remember { appPrefs.appearanceBarsBlurRadius.state }
    if (settingsGlass) {
      val glass = LocalGlassBackdrop.current
      val tint = if (MaterialTheme.colors.isLight) Color(0x62F6FAFF) else Color(0x42222F3F)
      AnexGlassSurface(Modifier.matchParentSize(), backdrop = glass, solid = glass == null,
        tint = if (glass != null) tint else Color.Unspecified, lens = glass != null) {}
    } else Box(Modifier
      .matchParentSize()
      .then(if (solidBackground) Modifier else Modifier.blurredBackgroundModifier(
        keyboardInset, handler, blurRadius, prefAlpha, handler?.keyboardCoversBar == true, onTop, density
      ))
      .drawWithCache {
        // store it as a variable, don't put it inside if without holding it here. Compiler don't see it changes otherwise
        val alpha = if (solidBackground) 1f else prefAlpha.value
        val backgroundColor = if (solidBackground || title != null || fixedTitleText != null || connection == null || !onTop) {
          themeBackgroundMix.copy(alpha)
        } else {
          themeBackgroundMix.copy(topTitleAlpha(false, connection))
        }
        onDrawBehind {
          drawRect(backgroundColor)
        }
      }
    )
    Box(
      Modifier
        .fillMaxWidth()
        .then(if (!settingsGlass && !onTop) Modifier.navigationBarsPadding() else Modifier)
        .heightIn(min = AppBarHeight * fontSizeSqrtMultiplier)
    ) {
      AppBar(
        title = {
          if (showSearch) {
            val placeholder = searchPlaceholder ?: stringResource(MR.strings.search_verb)
            SearchTextField(Modifier.fillMaxWidth(), alwaysVisible = searchAlwaysVisible, placeholder = placeholder, trailingContent = searchTrailingContent, reducedCloseButtonPadding = 12.dp, onValueChange = onSearchValueChanged)
          } else if (title != null) {
            title()
          } else if (titleText.value.isNotEmpty() && connection != null) {
            Row(
              Modifier
                .graphicsLayer {
                  alpha = if (fixedTitleText != null) 1f else topTitleAlpha(true, connection)
                }
            ) {
              Text(
                titleText.value,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        },
        navigationIcon = if (settingsGlass && navigationButton != null) ({
          Row(Modifier.clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.11f))) { navigationButton() }
        }) else navigationButton,
        addStatusInset = !settingsGlass,
        buttons = if (!showSearch) buttons else {{}},
        centered = !showSearch && (title != null || !onTop),
        onTop = onTop,
      )
      if (!settingsGlass) AppBarDivider(onTop, title != null || fixedTitleText != null, connection)
    }
  }
}


@Composable
fun CallAppBar(
  title: @Composable () -> Unit,
  onBack: () -> Unit
) {
  AppBar(
    title,
    navigationIcon = { NavigationButtonBack(tintColor = Color(0xFFFFFFD8), onButtonClicked = onBack) },
    centered = false,
    onTop = true
  )
}

@Composable
fun NavigationButtonBack(onButtonClicked: (() -> Unit)?, tintColor: Color = if (onButtonClicked != null) MaterialTheme.colors.primary else MaterialTheme.colors.secondary, height: Dp = 24.dp) {
  IconButton(onButtonClicked ?: {}, enabled = onButtonClicked != null) {
    Icon(
      painterResource(MR.images.ic_arrow_back_ios_new), stringResource(MR.strings.back), Modifier.height(height), tint = tintColor
    )
  }
}

@Composable
fun NavigationButtonClose(onButtonClicked: (() -> Unit)?, tintColor: Color = if (onButtonClicked != null) MaterialTheme.colors.primary else MaterialTheme.colors.secondary, height: Dp = 24.dp) {
  IconButton(onButtonClicked ?: {}, enabled = onButtonClicked != null) {
    Icon(
      painterResource(MR.images.ic_close), stringResource(MR.strings.back), Modifier.height(height), tint = tintColor
    )
  }
}

@Composable
fun ShareButton(onButtonClicked: () -> Unit) {
  IconButton(onButtonClicked) {
    Icon(
      painterResource(MR.images.ic_share), stringResource(MR.strings.share_verb), tint = MaterialTheme.colors.primary
    )
  }
}

@Composable
fun NavigationButtonMenu(onButtonClicked: () -> Unit) {
  IconButton(onClick = onButtonClicked) {
    Icon(
      painterResource(MR.images.ic_menu),
      stringResource(MR.strings.icon_descr_settings),
      tint = MaterialTheme.colors.primary,
    )
  }
}

@Composable
private fun BoxScope.AppBarDivider(onTop: Boolean, fixedAlpha: Boolean, connection: CollapsingAppBarNestedScrollConnection?) {
  if (connection != null) {
    Divider(
      Modifier
        .align(if (onTop) Alignment.BottomStart else Alignment.TopStart)
        .graphicsLayer {
          alpha = if (!onTop || fixedAlpha) 1f else topTitleAlpha(false, connection, 1f)
        }
    )
  } else {
    Divider(Modifier.align(if (onTop) Alignment.BottomStart else Alignment.TopStart))
  }
}

@Composable
private fun AppBar(
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  navigationIcon: @Composable (RowScope.() -> Unit)? = null,
  buttons: @Composable RowScope.() -> Unit = {},
  centered: Boolean,
  onTop: Boolean,
  addStatusInset: Boolean = true,
) {
  val adjustedModifier = modifier
    .then(if (onTop && addStatusInset) Modifier.statusBarsPadding() else Modifier)
    .height(AppBarHeight * fontSizeSqrtMultiplier)
    .fillMaxWidth()
    .padding(horizontal = AppBarHorizontalPadding)
  if (centered) {
    AppBarCenterAligned(adjustedModifier, title, navigationIcon, buttons)
  } else {
    AppBarStartAligned(adjustedModifier, title, navigationIcon, buttons)
  }
}

@Composable
private fun AppBarStartAligned(
  modifier: Modifier,
  title: @Composable () -> Unit,
  navigationIcon: @Composable (RowScope.() -> Unit)? = null,
  buttons: @Composable RowScope.() -> Unit
) {
  Row(
    modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (navigationIcon != null) {
      navigationIcon()
      Spacer(Modifier.width(AppBarHorizontalPadding))
    } else {
      Spacer(Modifier.width(DEFAULT_PADDING))
    }
    Row(Modifier
      .weight(1f)
      .padding(end = DEFAULT_PADDING_HALF)
    ) {
      title()
    }
    Row(
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      buttons()
    }
  }
}

@Composable
private fun AppBarCenterAligned(
  modifier: Modifier,
  title: @Composable () -> Unit,
  navigationIcon: @Composable (RowScope.() -> Unit)? = null,
  buttons: @Composable RowScope.() -> Unit,
) {
  CenteredRowLayout(modifier) {
    if (navigationIcon != null) {
      Row(
        Modifier.padding(end = AppBarHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = navigationIcon
      )
    } else {
      Spacer(Modifier)
    }
    Row(
      Modifier.padding(end = DEFAULT_PADDING_HALF)
    ) {
      title()
    }
    Row(
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      buttons()
    }
  }
}

private fun topTitleAlpha(text: Boolean, connection: CollapsingAppBarNestedScrollConnection, alpha: Float = appPrefs.inAppBarsAlpha.get()) =
  if (!connection.scrollTrackingEnabled) 0f
  else if (connection.appBarOffset.absoluteValue < AppBarHandler.appBarMaxHeightPx / 3) 0f
  else ((-connection.appBarOffset * 1.5f) / (AppBarHandler.appBarMaxHeightPx)).coerceIn(0f, if (text) 1f else alpha)

val AppBarHeight = 56.dp
val AppBarHorizontalPadding = 2.dp
