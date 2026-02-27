package space.diomentia.ehzdiagnosticstracker.data

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.MainScope

val LocalCoroutineScope = staticCompositionLocalOf { MainScope() }
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }
val LocalLocationProvider = compositionLocalOf<FusedLocationProviderClient?> { null }
val LocalDiagnosticSession =
    compositionLocalOf { mutableStateOf<DiagnosticSession>(throw error("no diagnostic session")) }
val LocalReportSession =
    compositionLocalOf { mutableStateOf<ReportSession>(throw error("no report session")) }