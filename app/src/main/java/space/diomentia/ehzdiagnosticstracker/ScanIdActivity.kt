package space.diomentia.ehzdiagnosticstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import space.diomentia.ehzdiagnosticstracker.ui.screens.ScanIdQrScreen
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge

class ScanIdActivity : ComponentActivity() {
    companion object {
        const val QR_DATA = "qr_data"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        setContent {
            ScanIdQrScreen()
        }
    }
}


