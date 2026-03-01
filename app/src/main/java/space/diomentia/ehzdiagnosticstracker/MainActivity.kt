package space.diomentia.ehzdiagnosticstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalLocationProvider
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.init(this)
        setupEdgeToEdge(this)
        val locationProvider = LocationServices.getFusedLocationProviderClient(this)
        setContent {
            CompositionLocalProvider(
                LocalCoroutineScope provides lifecycleScope,
                LocalSnackbarHostState provides SnackbarHostState(),
                LocalLocationProvider provides locationProvider
            ) {
                PtmTheme {
                    AppNavigation()
                }
            }
        }
    }
}