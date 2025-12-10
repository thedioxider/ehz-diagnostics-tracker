package space.diomentia.ehzdiagnosticstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.screens.DiagnosticPointsScreen
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge

val LocalAreaProvider = compositionLocalOf<String> { error("no area provided") }

class DiagnosticPointsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        setContent {
            val diagnosticArea = AppState.CurrentDiagnosticSession.diagnosticArea
            if (diagnosticArea == null) {
                finish()
                return@setContent
            }
            CompositionLocalProvider(
                LocalSnackbarHostState provides remember { SnackbarHostState() },
                LocalAreaProvider provides diagnosticArea
            ) {
                DiagnosticPointsScreen()
            }
        }
    }
}


