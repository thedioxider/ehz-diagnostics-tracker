package space.diomentia.ehzdiagnosticstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.screens.DiagnosticObjectsScreen
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge

class DiagnosticObjectChooseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        setContent {
            CompositionLocalProvider(
                LocalSnackbarHostState provides remember { SnackbarHostState() }
            ) {
                DiagnosticObjectsScreen()
            }
        }
    }
}


