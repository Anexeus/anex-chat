package chat.simplex.common.ui.theme

import androidx.compose.runtime.Composable

@Composable
internal actual fun systemPrefersSolidGlass(): Boolean = true

@Composable
internal actual fun glassOptics(shape: androidx.compose.ui.graphics.Shape, lens: Boolean): androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
