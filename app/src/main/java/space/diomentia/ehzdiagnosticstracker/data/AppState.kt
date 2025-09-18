package space.diomentia.ehzdiagnosticstracker.data

import android.location.Location
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDateTime

const val REPORT_CONFIG_FILENAME = "report.conf"

@Stable
object AppState {
    var reportConfig: ReportConfig? = null

    object CurrentDiagnosticSession {
        var id by mutableStateOf<Int?>(null)
        var dateTime by mutableStateOf<LocalDateTime>(LocalDateTime.now())
        var diagnosticsType by mutableIntStateOf(0)
        var reportType by mutableStateOf<Int?>(null)
        var location by mutableStateOf<Location?>(null)
        var data = mutableMapOf<String, MutableState<*>>()

        fun reset() {
            id = null
            dateTime = LocalDateTime.now()
            reportType = null
            location = null
            data.clear()
        }
    }
}