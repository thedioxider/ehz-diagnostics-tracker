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
val LocalReportSession =
    compositionLocalOf { mutableStateOf<ReportSession>(throw error("no diagnostic session")) }
val LocalLegacyReportSession =
    compositionLocalOf { mutableStateOf<LegacyReportSession>(throw error("no report session")) }
val LocalSessionManager =
    compositionLocalOf<SessionManager> { error("no session manager") }