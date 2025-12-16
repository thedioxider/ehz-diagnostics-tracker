package space.diomentia.ehzdiagnosticstracker.data

import android.content.ContentResolver
import android.net.Uri
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class ReportExporter(
    val reportId: Long,
    private val db: ReportDatabase,
    val reportConfig: ReportConfig? = null
) {
    private val report = db.diagnosticReportDao().getReportByIdOnce(reportId)

    @Serializable
    @Suppress("PropertyName")
    data class ExportReport(
        val timestamp: Long,
        val diagnostic_object: ExportDiagnosticObject,
        val diagnostic_area: ExportDiagnosticArea,
        val diagnostic_point: ExportDiagnosticPoint,
        val diagnostics_type: String,
        val report_type: String,
        val location: Coordinates,
        val inspector: String,
        val validation: String?,
        val data: List<ExportDataEntry>,
        val report_type_name: String? = null
    ) {
        val version = 1

        companion object {
            fun fromDatabaseEntity(
                reportId: Long,
                db: ReportDatabase,
                reportConfig: ReportConfig? = null
            ): ExportReport? {
                val r = db.diagnosticReportDao()
                    .getReportByIdOnce(reportId)
                    ?: return null
                val reportTypeConfig = reportConfig
                    ?.reportTypes
                    ?.find { it.id == r.reportType }
                val data = db.diagnosticReportDao().getReportDataOnce(r.id)
                    .map { ExportDataEntry(it, reportTypeConfig) }
                val diagnosticPoint =
                    db.diagnosticAreaDao().getPointByIdOnce(r.diagnosticPoint) ?: return null
                val diagnosticArea =
                    db.diagnosticAreaDao().getAreaByIdOnce(diagnosticPoint.diagnosticArea)
                        ?: return null
                val diagnosticObject =
                    db.diagnosticAreaDao().getObjectByIdOnce(diagnosticArea.diagnosticObject)
                        ?: return null
                return ExportReport(
                    timestamp = r.timestamp,
                    diagnostic_object = ExportDiagnosticObject(diagnosticObject),
                    diagnostic_area = ExportDiagnosticArea(diagnosticArea),
                    diagnostic_point = ExportDiagnosticPoint(diagnosticPoint),
                    diagnostics_type = r.diagnosticsType,
                    report_type = r.reportType,
                    location = r.location,
                    inspector = r.inspector,
                    validation = r.validation,
                    data = data,
                    report_type_name = reportTypeConfig?.name
                )
            }
        }

        @Serializable
        data class ExportDiagnosticObject(
            val id: String,
            val name: String,
            val additional_info: String? = null
        ) {
            constructor(ref: DiagnosticObject) : this(
                id = ref.id,
                name = ref.name,
                additional_info = ref.additionalInfo
            )
        }

        @Serializable
        data class ExportDiagnosticArea(
            val id: String,
            val name: String,
            val danger_class: String,
            val additional_info: String? = null
        ) {
            constructor(ref: DiagnosticArea) : this(
                id = ref.id,
                name = ref.name,
                danger_class = ref.dangerClass,
                additional_info = ref.additionalInfo
            )
        }

        @Serializable
        data class ExportDiagnosticPoint(
            val id: String,
            val name: String,
            val additional_info: String? = null
        ) {
            constructor(ref: DiagnosticPoint) : this(
                id = ref.id,
                name = ref.inventoryId,
                additional_info = ref.additionalInfo
            )
        }

        @Serializable
        data class ExportDataEntry(
            val characteristic: String,
            val value: String,
            var name: String? = null,
            var datatype: String? = null,
            var unit: String? = null
        ) {
            constructor(ref: ReportDataEntry, reportType: ReportType? = null) : this(
                characteristic = ref.characteristic,
                value = ref.value
            ) {
                val paramConfig = reportType
                    ?.parameters
                    ?.find { it.id == characteristic }
                name = paramConfig?.name
                datatype = paramConfig?.type?.name
                unit = paramConfig?.unit
            }
        }
    }

    fun defaultFilename() = report?.let { "report_${report.timestamp}.json" }

    private fun toJson(): String? {
        val exportReport = ExportReport.fromDatabaseEntity(
            reportId = reportId,
            db = db,
            reportConfig = reportConfig
        ) ?: return null
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .setVersion(exportReport.version.toDouble())
            .create()
        return gson.toJson(exportReport)
    }

    suspend fun exportReportToUri(
        uri: Uri,
        contentResolver: ContentResolver
    ): Boolean = withContext(Dispatchers.IO) {
        val jsonReport = toJson() ?: return@withContext false
        contentResolver.openOutputStream(uri)?.use {
            it.write(jsonReport.toByteArray())
        } ?: return@withContext false
        return@withContext true
    }
}
