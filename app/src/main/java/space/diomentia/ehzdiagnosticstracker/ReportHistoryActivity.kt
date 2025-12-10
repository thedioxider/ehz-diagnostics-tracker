package space.diomentia.ehzdiagnosticstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.screens.ReportHistoryScreen
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge

class ReportHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        val snackbarHostState = SnackbarHostState()

        setContent {
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState
            ) {
                ReportHistoryScreen()
            }
        }
    }
}


