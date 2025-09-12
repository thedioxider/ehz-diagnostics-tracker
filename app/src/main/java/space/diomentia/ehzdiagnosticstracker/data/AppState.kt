package space.diomentia.ehzdiagnosticstracker.data

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
object AppState {
    object CurrentDiagnosticSession {
        var id by mutableStateOf<Int?>(null)
        var corrosionDiagnosticsType by mutableIntStateOf(0)
        var worksContentsType by mutableStateOf<Int?>(null)

        fun reset() {
            id = null
            worksContentsType = null
        }
    }
}