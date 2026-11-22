package chat.simplex.common.views.newchat

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import boofcv.abst.fiducial.QrCodeDetector
import boofcv.alg.color.ColorFormat
import boofcv.android.ConvertCameraImage
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.struct.image.GrayU8
import chat.simplex.common.platform.TAG
import chat.simplex.common.ui.theme.DEFAULT_PADDING_HALF
import chat.simplex.common.views.helpers.*
import chat.simplex.res.*
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.common.util.concurrent.ListenableFuture
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import chat.simplex.common.ui.theme.*
import kotlinx.coroutines.delay
import java.util.concurrent.*

// Adapted from learntodroid - https://gist.github.com/learntodroid/8f839be0b29d0378f843af70607bd7f5

@Composable
actual fun QRCodeScanner(
  showQRCodeScanner: MutableState<Boolean>,
  padding: PaddingValues,
  onBarcode: suspend (String) -> Boolean
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val handle by rememberUpdatedState(onBarcode)
  val scope = rememberCoroutineScope()
  Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val modifier = Modifier
      .padding(padding)
      .clipToBounds()
      .sizeIn(maxWidth = 400.dp, maxHeight = 400.dp)
      .aspectRatio(1f)
    val showScanner = remember { showQRCodeScanner }
    if (showScanner.value && cameraPermissionState.status == PermissionStatus.Granted) {
      val view = remember(context) { PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
      } }
      var camera by remember { mutableStateOf<Camera?>(null) }
      var lit by remember { mutableStateOf(false) }
      var failed by remember { mutableStateOf(false) }
      DisposableEffect(view, lifecycleOwner) {
        val worker = Executors.newSingleThreadExecutor()
        val busy = AtomicBoolean()
        var previous: String? = null
        var disposed = false
        val future = ProcessCameraProvider.getInstance(context)
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        val detector = lazy { FactoryFiducial.qrcode(null, GrayU8::class.java) }
        analysis.setAnalyzer(worker) { frame ->
          val code = try {
            if (busy.get()) null else imageProxyToGrayU8(frame)?.let {
              detector.value.process(it)
              detector.value.detections.firstOrNull()?.message
            }
          } finally { frame.close() }
          if (code != null && busy.compareAndSet(false, true)) scope.launch {
            try {
              if (!disposed && code != previous) {
                if (handle(code)) previous = code
                delay(1000)
              }
            } finally { busy.set(false) }
          }
        }
        var provider: ProcessCameraProvider? = null
        future.addListener({
          if (!disposed) try {
            val p = future.get()
            provider = p
            val selector = if (p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            camera = p.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
          } catch (e: Exception) {
            failed = true
            Log.w(TAG, "QR camera unavailable: " + e.javaClass.simpleName)
          }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
          disposed = true
          analysis.clearAnalyzer()
          provider?.unbind(preview, analysis)
          worker.shutdown()
        }
      }
      Box(modifier) {
        AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
        if (failed) Text(stringResource(MR.strings.camera_not_available), Modifier.align(Alignment.Center))
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
          AnexGlassSurface(Modifier.align(Alignment.BottomEnd).padding(8.dp), solid = !glassEnabled()) {
            TextButton(onClick = {
              val enable = !lit
              camera?.cameraControl?.enableTorch(enable)
              lit = enable
            }) { Text(if (lit) "Torch off" else "Torch on", color = LocalContentColor.current) }
          }
        }
      }
    } else {
      val buttonColors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.9f),
        contentColor = MaterialTheme.colors.primary,
        disabledBackgroundColor = MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.9f),
        disabledContentColor = MaterialTheme.colors.primary,
      )
      var permissionRequested by rememberSaveable { mutableStateOf(false) }
      when {
        cameraPermissionState.status is PermissionStatus.Denied && !permissionRequested && showScanner.value -> {
          LaunchedEffect(Unit) {
            permissionRequested = true
            cameraPermissionState.launchPermissionRequest()
          }
        }
        cameraPermissionState.status is PermissionStatus.Denied -> {
          Button({ cameraPermissionState.launchPermissionRequest() }, modifier = modifier, colors = buttonColors) {
            Icon(painterResource(MR.images.ic_camera_enhance), null)
            Spacer(Modifier.width(DEFAULT_PADDING_HALF))
            Text(stringResource(MR.strings.enable_camera_access))
          }
        }
        cameraPermissionState.status == PermissionStatus.Granted -> {
          Button({ showQRCodeScanner.value = true }, modifier = modifier, colors = buttonColors) {
            Icon(painterResource(MR.images.ic_qr_code), null)
            Spacer(Modifier.width(DEFAULT_PADDING_HALF))
            Text(stringResource(MR.strings.tap_to_scan))
          }
        }
        !LocalContext.current.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) -> {
          Button({  }, enabled = false, modifier = modifier, colors = buttonColors) {
            Text(stringResource(MR.strings.camera_not_available))
          }
        }
      }
    }
  }
}

@SuppressLint("UnsafeOptInUsageError")
private fun imageProxyToGrayU8(img: ImageProxy) : GrayU8? {
  val image = img.image
  if (image != null) {
    val outImg = GrayU8()
    ConvertCameraImage.imageToBoof(image, ColorFormat.GRAY, outImg, null)
    return outImg
  }
  return null
}
