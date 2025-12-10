package space.diomentia.ehzdiagnosticstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.ui.screens.InitScreen
import space.diomentia.ehzdiagnosticstracker.ui.screens.OutOfServiceScreen
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import java.time.LocalDate

class InitActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.init(this)
        setupEdgeToEdge(this)
        setContent {
            if (LocalDate.now() < LocalDate.of(2026, 1, 1)) {
                InitScreen()
            } else {
                OutOfServiceScreen()
            }
        }
    }
}