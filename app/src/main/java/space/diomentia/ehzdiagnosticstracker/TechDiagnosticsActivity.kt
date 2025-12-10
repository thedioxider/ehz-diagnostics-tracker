package space.diomentia.ehzdiagnosticstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import com.google.android.gms.location.LocationServices
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalLocationProvider
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.screens.ReportEditorScreen
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge

class TechDiagnosticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        val snackbarHostState = SnackbarHostState()
        val pointId = AppState.CurrentDiagnosticSession.diagnosticPoint
        if (pointId == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalLocationProvider provides LocationServices.getFusedLocationProviderClient(this)
            ) {
                ReportEditorScreen()
            }
        }
    }
}


