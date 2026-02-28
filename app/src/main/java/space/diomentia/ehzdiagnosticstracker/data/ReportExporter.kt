package space.diomentia.ehzdiagnosticstracker.data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ReportExporter(
    val reportId: Long,
    private val db: ReportDatabase,
    val reportConfig: ReportConfig? = null
) {
    private val report = db.diagnosticReportDao().getReportByIdOnce(reportId)

    @Serializable
    data class ExportReport(
        @SerialName("timestamp") val timestamp: Long,
        @SerialName("diagnostic_object") val diagnosticObject: ExportRoute,
        @SerialName("diagnostic_area") val diagnosticArea: ExportSection,
        @SerialName("diagnostic_point") val diagnosticPoint: ExportSite,
        @SerialName("diagnostics_type") val diagnosticsType: String,
        @SerialName("report_type") val reportType: String,
        @SerialName("location") val location: Coordinates,
        @SerialName("inspector") val inspector: String,
        @SerialName("validation") val validation: String?,
        @SerialName("data") val data: List<ExportDataEntry>,
        @SerialName("report_type_name") val reportTypeName: String? = null
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
                    ?.templates
                    ?.find { it.id == r.reportType }
                val data = db.diagnosticReportDao().getReportDataOnce(r.id)
                    .map { ExportDataEntry(it, reportTypeConfig) }
                val diagnosticPoint =
                    db.routeDao().getPointByIdOnce(r.diagnosticPoint) ?: return null
                val diagnosticArea =
                    db.routeDao().getSectionByIdOnce(diagnosticPoint.sectionId)
                        ?: return null
                val diagnosticObject =
                    db.routeDao().getRouteByIdOnce(diagnosticArea.routeId)
                        ?: return null
                return ExportReport(
                    timestamp = r.timestamp,
                    diagnosticObject = ExportRoute(diagnosticObject),
                    diagnosticArea = ExportSection(diagnosticArea),
                    diagnosticPoint = ExportSite(diagnosticPoint),
                    diagnosticsType = r.diagnosticsType,
                    reportType = r.reportType,
                    location = r.location,
                    inspector = r.inspector,
                    validation = r.validationId,
                    data = data,
                    reportTypeName = reportTypeConfig?.name
                )
            }
        }

        @Serializable
        data class ExportRoute(
            @SerialName("id") val id: String,
            @SerialName("name") val name: String,
            @SerialName("additional_info") val additionalInfo: String? = null
        ) {
            constructor(ref: Route) : this(
                id = ref.id,
                name = ref.name,
                additionalInfo = ref.additionalInfo
            )
        }

        @Serializable
        data class ExportSection(
            @SerialName("id") val id: String,
            @SerialName("name") val name: String,
            @SerialName("danger_class") val dangerClass: String,
            @SerialName("additional_info") val additionalInfo: String? = null
        ) {
            constructor(ref: Section) : this(
                id = ref.id,
                name = ref.name,
                dangerClass = ref.dangerClass,
                additionalInfo = ref.additionalInfo
            )
        }

        @Serializable
        data class ExportSite(
            @SerialName("id") val id: String,
            @SerialName("name") val name: String,
            @SerialName("additional_info") val additionalInfo: String? = null
        ) {
            constructor(ref: DiagnosticPoint) : this(
                id = ref.id,
                name = ref.inventoryId,
                additionalInfo = ref.additionalInfo
            )
        }

        @Serializable
        data class ExportDataEntry(
            @SerialName("characteristic") val characteristic: String,
            @SerialName("value") val value: String,
            @SerialName("name") var name: String? = null,
            @SerialName("datatype") var datatype: String? = null,
            @SerialName("unit") var unit: String? = null
        ) {
            constructor(ref: ReportDataEntry, reportTemplate: ReportTemplate? = null) : this(
                characteristic = ref.characteristic,
                value = ref.value
            ) {
                val paramConfig = reportTemplate
                    ?.specIds
                    ?.find { it.id == characteristic }
                name = paramConfig?.name
                datatype = paramConfig?.type?.datatype
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
        return Json.default.encodeToString(exportReport)
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
