package chat.simplex.common.ui.theme

import android.annotation.TargetApi
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
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

@Composable
internal actual fun glassOptics(shape: Shape, lens: Boolean): Modifier {
  if (Build.VERSION.SDK_INT < 33) return if (Build.VERSION.SDK_INT >= 31) Modifier.blur(5.dp) else Modifier
  return refractingLayer(shape, lens)
}

@TargetApi(33)
@Composable
private fun refractingLayer(shape: Shape, lens: Boolean): Modifier {
  val density = LocalDensity.current
  val direction = LocalLayoutDirection.current
  var bounds by remember { mutableStateOf(IntSize.Zero) }
  val effect = remember(bounds, density, direction, shape, lens) {
    if (bounds.width == 0 || bounds.height == 0) null else {
      val outline = shape.createOutline(bounds.toSize(), direction, density)
      val radius = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x
        ?: minOf(bounds.width, bounds.height) / 2f
      val shader = RuntimeShader(GLASS_SHADER).apply {
        setFloatUniform("extent", bounds.width.toFloat(), bounds.height.toFloat())
        setFloatUniform("radius", radius)
        setFloatUniform("depth", density.density * if (lens) 9f else 5f)
      }
      val sigma = density.density * if (lens) 1.25f else 3.2f
      RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(shader, "scene"),
        RenderEffect.createBlurEffect(sigma, sigma, Shader.TileMode.CLAMP)).asComposeRenderEffect()
    }
  }
  return Modifier.onSizeChanged { bounds = it }.graphicsLayer { renderEffect = effect }
}

// Signed distance to a rounded rectangle supplies the surface normal. Only the curved
// perimeter shifts the scene; the flat centre and all foreground glyphs stay stable.
private const val GLASS_SHADER = """
  uniform shader scene;
  uniform float2 extent;
  uniform float radius;
  uniform float depth;

  half4 main(float2 p) {
    float2 q = p - extent * 0.5;
    float2 corner = abs(q) - (extent * 0.5 - radius);
    float2 outside = max(corner, 0.0);
    float distance = radius - length(outside) - min(max(corner.x, corner.y), 0.0);
    float2 normal = length(outside) > 0.001 ? outside / length(outside)
      : (corner.x > corner.y ? float2(1,0) : float2(0,1));
    normal *= sign(q);
    float edge = 1.0 - smoothstep(0.0, max(1.0, min(radius, depth * 2.4)), distance);
    float2 bend = normal * depth * edge * edge;
    float2 uv = clamp(p - bend, float2(0.5), extent - 0.5);
    half4 c = scene.eval(uv);
    c.r = scene.eval(clamp(uv - bend * 0.10, float2(0.5), extent - 0.5)).r;
    c.b = scene.eval(clamp(uv + bend * 0.10, float2(0.5), extent - 0.5)).b;
    float glint = pow(max(dot(normal, float2(-0.60,-0.80)), 0.0), 5.0)
      * exp(-max(distance, 0.0) / max(0.5, depth * 0.17));
    return half4(min(c.rgb + half3(glint * 0.20), half3(1.0)), c.a);
  }
"""
