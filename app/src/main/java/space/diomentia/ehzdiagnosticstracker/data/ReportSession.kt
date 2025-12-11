package space.diomentia.ehzdiagnosticstracker.data

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import space.diomentia.ehzdiagnosticstracker.data.AppState.profileSession
import space.diomentia.ehzdiagnosticstracker.data.AppState.reportConfig
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class ReportSession(
    val diagnosticSession: DiagnosticSession,
    val diagnosticPoint: DiagnosticPoint,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val reportType: Int? = null,
    val location: Coordinates? = null,
    val validationId: String? = null,
    val data: MutableMap<String, MutableState<String>> = mutableMapOf(),
    private val reportId: Long? = null,
    val inspector: String? = null
) {
    companion object {
        fun fromExisting(
            context: Context,
            diagnosticSession: DiagnosticSession,
            reportId: Long
        ): ReportSession? {
            val report = ReportDatabase.getInstance(context).diagnosticReportDao()
                .getReportByIdOnce(reportId)
            if (report == null) return null
            val point = ReportDatabase.getInstance(context).diagnosticAreaDao()
                .getPointByIdOnce(report.report.diagnosticPoint)
            if (point == null) return null
            return ReportSession(
                diagnosticSession = diagnosticSession,
                diagnosticPoint = point.point,
                dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(report.report.timestamp),
                    ZoneId.systemDefault()
                ),
                reportType = reportConfig.value?.reportTypes
                    ?.indexOfFirst { it.id == report.report.reportType }
                    ?.let { if (it != -1) it else null },
                location = report.report.location,
                validationId = report.report.validation,
                data = mutableMapOf(
                    *report.data.map { it.characteristic to mutableStateOf(it.value) }
                        .toTypedArray()
                ),
                reportId = report.report.id,
                inspector = report.report.inspector
            )
        }
    }

    fun reportToDb(context: Context): Boolean {
        val reportDb = ReportDatabase.getInstance(context)
        val reportConfig = reportConfig.value
        val diagnosticType = reportType
        val location = location
        val inspector = profileSession
        val data = data
        if (diagnosticType == null
            || location == null
            || reportConfig == null
            || inspector == null
        ) return false
        (reportConfig.reportTypes[diagnosticType].parameters ?: emptyList())
            .asSequence()
            .map { it.id }
            .toSet()
        var report = DiagnosticReport(
            diagnosticPoint = diagnosticPoint.id,
            timestamp = dateTime.atZone(ZoneId.systemDefault()).toEpochSecond(),
            diagnosticsType = diagnosticSession.diagnosticType.toString(),
            reportType = reportConfig.reportTypes[diagnosticType].id,
            location = Coordinates(location.latitude, location.longitude),
            inspector = this.inspector ?: "${inspector.surname} ${inspector.name}",
            validation = validationId
        )
        val newReportId = if (reportId != null) {
            report = report.copy(id = reportId)
            reportDb.diagnosticReportDao().updateReport(report)
            reportId
        } else {
            reportDb.diagnosticReportDao().insertReport(report)
        }
        val dataEntries = data
            .filter { (_, v) -> v.value.isNotBlank() }
            .map { (k, v) ->
                ReportDataEntry(
                    report = newReportId,
                    characteristic = k,
                    value = v.value.toString()
                )
            }
            .toTypedArray()
        reportDb.diagnosticReportDao().insertData(*dataEntries)
        reportDb.diagnosticAreaDao().updateDiagnosticArea(
            diagnosticSession.diagnosticArea.copy(
                lastLocation = Coordinates(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
        )
        return true
    }
}