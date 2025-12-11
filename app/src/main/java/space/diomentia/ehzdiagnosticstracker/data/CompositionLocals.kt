package space.diomentia.ehzdiagnosticstracker.data

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.CoroutineScope

val LocalCoroutineScope = staticCompositionLocalOf<CoroutineScope?> { null }
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }
val LocalLocationProvider = compositionLocalOf<FusedLocationProviderClient?> { null }