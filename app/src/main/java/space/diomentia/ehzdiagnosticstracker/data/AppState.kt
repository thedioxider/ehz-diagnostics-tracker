package space.diomentia.ehzdiagnosticstracker.data

import android.location.Location
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
        var corrosionDiagnosticsType by mutableIntStateOf(0)
        var worksContentsType by mutableStateOf<Int?>(null)
        var location by mutableStateOf<Location?>(null)

        fun reset() {
            id = null
            dateTime = LocalDateTime.now()
            worksContentsType = null
            location = null
        }
    }
}