package space.diomentia.ehzdiagnosticstracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat

fun Context.checkCameraPermissions() = ActivityCompat.checkSelfPermission(
    this,
    Manifest.permission.CAMERA
) == PackageManager.PERMISSION_GRANTED

@SuppressLint("ComposableNaming")
@Composable
fun askCameraPermissions(
    onFailure: () -> Unit = {}
): Boolean {
    val context = LocalContext.current
    var permissionsGranted by remember {
        mutableStateOf(
            context.checkCameraPermissions()
        )
    }
    if (!permissionsGranted) {
        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { success ->
            if (success) {
                permissionsGranted = true
            } else {
                onFailure()
            }
        }
        LaunchedEffect(cameraPermissionLauncher) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    return permissionsGranted
}