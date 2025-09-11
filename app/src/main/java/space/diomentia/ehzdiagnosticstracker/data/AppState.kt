package space.diomentia.ehzdiagnosticstracker.data

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
object AppState {
    object currentDiagnosticArea {
        var id by mutableStateOf<Int?>(null)
    }
}