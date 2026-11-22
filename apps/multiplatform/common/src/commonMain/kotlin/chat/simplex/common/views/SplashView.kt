package chat.simplex.common.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import chat.simplex.common.platform.appPlatform
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.painterResource
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SplashView(nonTransparent: Boolean = false) {
  Surface(
    Modifier
      .fillMaxSize(),
    color = if (nonTransparent) MaterialTheme.colors.background.copy(1f) else MaterialTheme.colors.background,
    contentColor = LocalContentColor.current
  ) {
    if (appPlatform.isAndroid) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(painterResource(MR.images.anex_logo), null, Modifier.size(112.dp))
      }
    }
  }
}
