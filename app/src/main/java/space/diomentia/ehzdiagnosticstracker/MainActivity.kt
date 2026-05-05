package space.diomentia.ehzdiagnosticstracker

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalLocationProvider

class MainActivity : PtmActivity() {
    private lateinit var locationProvider: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        AppState.init(this)
        locationProvider = LocationServices.getFusedLocationProviderClient(this)
        super.onCreate(savedInstanceState)
    }

    override fun extraProviders(): Array<ProvidedValue<*>> = arrayOf(
        LocalLocationProvider provides locationProvider
    )

    @Composable
    override fun Screen() = AppNavigation()
}