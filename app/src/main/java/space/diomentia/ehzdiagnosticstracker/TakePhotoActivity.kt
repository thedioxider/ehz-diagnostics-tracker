package space.diomentia.ehzdiagnosticstracker

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.CameraPreview
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmOutlinedButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import java.io.File
import java.io.IOException

class TakePhotoActivity : PtmActivity() {
    companion object {
        const val EXTRA_OUTPUT = "extra_output"
    }

    @Composable
    override fun Screen() = TakePhotoScreen()
}

@Composable
private fun TakePhotoScreen() {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current

    askCameraPermissions {
        activity?.run {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    @Suppress("DEPRECATION")
    val outputUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        activity?.intent?.getParcelableExtra(TakePhotoActivity.EXTRA_OUTPUT, Uri::class.java)
    else
        activity?.intent?.getParcelableExtra(TakePhotoActivity.EXTRA_OUTPUT)

    @Suppress("DEPRECATION")
    val imageCapture = remember { ImageCapture.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build() }
    var capturedFile by remember { mutableStateOf<File?>(null) }

    val onError = {
        coroutineScope?.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(context.getString(R.string.something_wrong))
        }
        Unit
    }

    if (capturedFile != null) {
        PhotoConfirmScreen(
            file = capturedFile!!,
            snackbarHostState = snackbarHostState,
            onConfirm = {
                if (outputUri == null) {
                    activity?.setResult(Activity.RESULT_CANCELED)
                    activity?.finish()
                    return@PhotoConfirmScreen
                }
                try {
                    context.contentResolver.openOutputStream(outputUri)?.use { out ->
                        capturedFile!!.inputStream().use { it.copyTo(out) }
                    }
                    activity?.setResult(Activity.RESULT_OK)
                } catch (e: IOException) {
                    activity?.setResult(Activity.RESULT_CANCELED)
                } finally {
                    capturedFile!!.delete()
                    activity?.finish()
                }
            },
            onRetake = {
                capturedFile?.delete()
                capturedFile = null
            }
        )
    } else {
        CameraViewfinderScreen(
            imageCapture = imageCapture,
            snackbarHostState = snackbarHostState,
            onCapture = { file -> capturedFile = file },
            onCancel = {
                activity?.setResult(Activity.RESULT_CANCELED)
                activity?.finish()
            },
            onError = onError
        )
    }
}

@Composable
private fun CameraViewfinderScreen(
    imageCapture: ImageCapture,
    snackbarHostState: SnackbarHostState,
    onCapture: (File) -> Unit,
    onCancel: () -> Unit,
    onError: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { PtmSnackbarHost(snackbarHostState) },
        bottomBar = {
            PtmBottomBar(
                navigation = {
                    ActionButton(
                        icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                        label = stringResource(R.string.back)
                    ) { onCancel() }
                },
                actions = null
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CameraPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .align(Alignment.TopCenter),
                imageCapture,
                aspectRatio = AspectRatio.RATIO_4_3
            )
            ShutterButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                val tempFile = try {
                    File.createTempFile("CAPTURE", ".jpg", context.cacheDir)
                } catch (e: IOException) {
                    onError()
                    return@ShutterButton
                }
                imageCapture.takePicture(
                    ImageCapture.OutputFileOptions.Builder(tempFile).build(),
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                            onCapture(tempFile)
                        }

                        override fun onError(e: ImageCaptureException) {
                            tempFile.delete()
                            onError()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    PtmOutlinedButton(
        roundedCorners = true,
        modifier = modifier
            .size(88.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(
            painterResource(R.drawable.ic_photo_camera),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PhotoConfirmScreen(
    file: File,
    snackbarHostState: SnackbarHostState,
    onConfirm: () -> Unit,
    onRetake: () -> Unit
) {
    val bitmap = remember(file) {
        val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return@remember null
        val degrees = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        ).let {
            when (it) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
        if (degrees == 0f) raw
        else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(degrees) }, true)
    }?.asImageBitmap()

    Scaffold(
        snackbarHost = { PtmSnackbarHost(snackbarHostState) },
        bottomBar = {
            PtmBottomBar(
                navigation = {
                    ActionButton(
                        icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                        label = stringResource(R.string.retake)
                    ) { onRetake() }
                },
                actions = {
                    ActionButton(
                        icon = rememberVectorPainter(Icons.Outlined.Check),
                        label = stringResource(R.string.use_photo)
                    ) { onConfirm() }
                }
            )
        }
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}