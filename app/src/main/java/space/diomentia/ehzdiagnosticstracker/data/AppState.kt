package space.diomentia.ehzdiagnosticstracker.data

import android.content.Context
import android.location.Location
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.time.LocalDateTime

const val REPORT_CONFIG_FILENAME = "report.conf.yaml"

@Stable
object AppState {
    var reportConfig by mutableStateOf<ReportConfig?>(null)

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

    fun loadReportConfig(context: Context): Boolean {
        File(context.dataDir, REPORT_CONFIG_FILENAME).let {
            if (!it.isFile) {
                return false
            }
            reportConfig = ReportConfig.parseYaml(it.readText())
            return true
        }
    }
}