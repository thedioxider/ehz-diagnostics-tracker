package space.diomentia.ehzdiagnosticstracker.data

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
            diagnosticSession: DiagnosticSession,
            reportId: Long,
            reportDb: ReportDatabase
        ): ReportSession? {
            val report = reportDb.diagnosticReportDao()
                .getReportByIdOnce(reportId)
            if (report == null) return null
            val point = reportDb.diagnosticAreaDao()
                .getPointByIdOnce(report.diagnosticPoint)
            if (point == null) return null
            val data = reportDb.diagnosticReportDao()
                .getReportDataOnce(report.id)
            return ReportSession(
                diagnosticSession = diagnosticSession,
                diagnosticPoint = point,
                dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(report.timestamp),
                    ZoneId.systemDefault()
                ),
                reportType = reportConfig.value?.reportTypes
                    ?.indexOfFirst { it.id == report.reportType }
                    ?.let { if (it != -1) it else null },
                location = report.location,
                validationId = report.validationId,
                data = mutableMapOf(
                    *data.map { it.characteristic to mutableStateOf(it.value) }
                        .toTypedArray()
                ),
                reportId = report.id,
                inspector = report.inspector
            )
        }
    }

    fun reportToDb(reportDb: ReportDatabase): Long? {
        val reportConfig = reportConfig.value
        val diagnosticType = reportType
        val location = location
        val inspector = profileSession
        val data = data
        if (diagnosticType == null
            || location == null
            || reportConfig == null
            || inspector == null
        ) return null
        val typeConfig = reportConfig.reportTypes[diagnosticType]
        var report = DiagnosticReport(
            diagnosticPoint = diagnosticPoint.id,
            timestamp = dateTime.atZone(ZoneId.systemDefault()).toEpochSecond(),
            diagnosticsType = diagnosticSession.diagnosticType.toString(),
            reportType = reportConfig.reportTypes[diagnosticType].id,
            location = Coordinates(location.latitude, location.longitude),
            inspector = this.inspector ?: "${inspector.surname} ${inspector.name}",
            validationId = validationId
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
                    value = v.value.toString(),
                    datatype = typeConfig.parameters?.find { it.id == k }?.type?.datatype
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
        return newReportId
    }
}