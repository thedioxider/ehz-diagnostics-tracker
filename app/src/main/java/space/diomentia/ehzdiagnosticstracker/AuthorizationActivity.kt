package space.diomentia.ehzdiagnosticstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.screens.AuthorizationScreen
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge

class AuthorizationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        setContent {
            CompositionLocalProvider(
                LocalCoroutineScope provides rememberCoroutineScope(),
                LocalSnackbarHostState provides SnackbarHostState()
            ) {
                AuthorizationScreen()
            }
        }
    }
}