package space.diomentia.ehzdiagnosticstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.screens.DiagnosticMenuScreen
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge

class ToolMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        val snackbarHostState = SnackbarHostState()
        setContent {
            BackHandler {
                startActivity(Intent(this, DiagnosticObjectChooseActivity::class.java))
            }

            CompositionLocalProvider(
                LocalCoroutineScope provides rememberCoroutineScope(),
                LocalSnackbarHostState provides snackbarHostState
            ) {
                DiagnosticMenuScreen()
            }
        }
    }
}