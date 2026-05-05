package space.diomentia.ehzdiagnosticstracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.data.BarcodeAnalyzer
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime


class ScanIdQrActivity : ComponentActivity() {
    companion object {
        const val QR_DATA = "qr_data"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        setContent {
            CompositionLocalProvider(
                LocalCoroutineScope provides lifecycleScope,
                LocalSnackbarHostState provides SnackbarHostState()
            ) {
                ScanIdQrScreen()
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun ScanIdQrScreen() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = LocalCoroutineScope.current

    askCameraPermissions {
        activity?.run {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    PtmTheme {
        Scaffold(
            topBar = {
                PtmTopBar(
                    title = { Text("${stringResource(R.string.scanning)}...") }
                )
            },
            bottomBar = {
                PtmBottomBar(
                    navigation = {
                        ActionButton(
                            icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                            label = stringResource(R.string.back)
                        ) {
                            activity?.run {
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            }
                        }
                    }
                )
            },
            snackbarHost = { PtmSnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            var pingWrongFormat = 0L
            // stop blaming me for not using padding
            // just gimme my borderless camera!!
            @Suppress("UNUSED_EXPRESSION")
            if (false) innerPadding
            Column {
                BarcodeCameraPreview(
                    modifier = Modifier.fillMaxSize()
                ) { data ->
                    if (data.isNotBlank() && data.all { c -> c.isLetterOrDigit() }) {
                        activity?.run {
                            val intent = Intent().also {
                                it.putExtra(ScanIdQrActivity.QR_DATA, data)
                            }
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }
                        coroutineScope?.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("a")
                        }
                    } else {
                        val t = System.currentTimeMillis()
                        if (t - pingWrongFormat > 5000) {
                            coroutineScope?.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(context.getString(R.string.qr_no_id))
                            }
                            pingWrongFormat = t
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarcodeCameraPreview(
    modifier: Modifier = Modifier,
    barcodeListener: (data: String) -> Unit
) {
    if (LocalInspectionMode.current) {
        return
    }

    val context = LocalContext.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var preview by remember { mutableStateOf<Preview?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { androidViewContext ->
            PreviewView(androidViewContext).apply {
                // some configurations of layout since it is not compose element
                scaleType = PreviewView.ScaleType.FILL_CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            }
        },
        update = { previewView ->
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            val cameraExecutor = Executors.newSingleThreadExecutor()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraProvider = cameraProviderFuture.get()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { data ->
                            barcodeListener(data)
                        })
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("ScanId", "failed camera preview: ${e.localizedMessage}")
                }
            }, mainExecutor)
        }
    )
}


@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ScanIdScreenPreview() = ScanIdQrScreen()