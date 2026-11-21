package chat.simplex.common.platform

internal actual fun readAnexServerDefaults(): List<String> =
  androidAppContext.assets.open("anex-server-defaults.txt").bufferedReader().use { it.readLines() }
