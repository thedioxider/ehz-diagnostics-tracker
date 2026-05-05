package space.diomentia.ehzdiagnosticstracker

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.data.BarcodeAnalyzer
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.CameraPreview
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime

class ScanIdQrActivity : PtmActivity() {
    companion object {
        const val QR_DATA = "qr_data"
    }

    @Composable
    override fun Screen() = ScanIdQrScreen()
}

@OptIn(ExperimentalTime::class)
@Composable
private fun ScanIdQrScreen() {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = LocalCoroutineScope.current

    askCameraPermissions {
        activity?.run {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    val pingWrongFormat = remember { LongArray(1) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { data ->
                    if (data.isNotBlank() && data.all { c -> c.isLetterOrDigit() }) {
                        activity?.run {
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(ScanIdQrActivity.QR_DATA, data)
                            )
                            finish()
                        }
                    } else {
                        val t = System.currentTimeMillis()
                        if (t - pingWrongFormat[0] > 5000) {
                            coroutineScope?.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(context.getString(R.string.qr_no_id))
                            }
                            pingWrongFormat[0] = t
                        }
                    }
                })
            }
    }

    Scaffold(
        topBar = {
            PtmTopBar(title = { Text("${stringResource(R.string.scanning)}...") })
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
        @Suppress("UNUSED_EXPRESSION")
        if (false) innerPadding
        CameraPreview(Modifier.fillMaxSize(), imageAnalysis)
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ScanIdScreenPreview() = ScanIdQrScreen()