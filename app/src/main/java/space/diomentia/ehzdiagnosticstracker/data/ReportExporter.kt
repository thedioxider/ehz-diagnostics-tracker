package space.diomentia.ehzdiagnosticstracker.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Serializable
private data class ExportedReport(
    val version: Int = 2,
    val id: String,
    val inspector: String,
    val route: ExportedRoute,
    val section: ExportedSection,
    val start_time: Long,
    val end_time: Long,
    val sites: List<ExportedSite>
) {
    companion object {
        fun fromFullReport(it: FullReport) = ExportedReport(
            id = it.info.id,
            route = ExportedRoute.fromRoute(it.route),
            section = ExportedSection.fromSection(it.section),
            inspector = it.info.inspectorName,
            start_time = it.info.startTime,
            end_time = it.info.endTime
                ?: throw IllegalArgumentException("Unfinished report with no end time provided"),
            sites = it.sites.map { site -> ExportedSite.fromSiteWithFields(site) }
        )
    }
}

@Serializable
private data class ExportedRoute(
    val id: String,
    val name: String,
    val length_km: Int? = null
) {
    companion object {
        fun fromRoute(it: Route) = ExportedRoute(it.id, it.name, it.lengthKm)
    }
}

@Serializable
private data class ExportedSection(
    val id: String,
    val begin_km: Int,
    val end_km: Int
) {
    companion object {
        fun fromSection(it: Section) = ExportedSection(it.id, it.beginKm, it.endKm)
    }
}

@Serializable
private data class ExportedSite(
    val id: String,
    val timestamp: Long,
    val position_km: Double? = null,
    val location: Coordinates? = null,
    val validation_id: String? = null,
    val fields: List<ExportedField>?
) {
    companion object {
        fun fromSiteWithFields(it: SiteWithFields) = ExportedSite(
            id = it.info.id,
            position_km = it.info.positionKm,
            location = it.info.location,
            timestamp = it.info.timestamp,
            validation_id = it.info.validationId,
            fields = if (!it.info.locationOnly)
                it.fields.map { field -> ExportedField.fromReportField(field) }
            else null
        )
    }
}

@Serializable
private data class ExportedField(
    val spec_id: String,
    val name: String,
    val type: String,
    val value: String,
    val unit: String? = null
) {
    companion object {
        fun fromReportField(it: ReportField) = ExportedField(
            spec_id = it.specId,
            name = it.name,
            type = it.type,
            value = it.value,
            unit = it.unit
        )
    }
}


class ReportExporter(
    val context: Context,
    val reportIds: List<String>
) {
    private val dao = ReportDatabase.getInstance(context).reportDao()

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yy-MM-dd_HH-mm-ss")

        fun defaultFilename() = "reports_${LocalDateTime.now().format(dateFormatter)}.json"

        suspend fun exportToUri(
            content: String,
            uri: Uri,
            contentResolver: ContentResolver
        ): Boolean = withContext(Dispatchers.IO) {
            contentResolver.openOutputStream(uri)?.use {
                it.write(content.toByteArray())
            } ?: return@withContext false
            return@withContext true
        }
    }

    fun toJson(): String = Json.default.encodeToString(
        dao.getFullReportsByIdsOnce(reportIds)
            .map { ExportedReport.fromFullReport(it) }
    )

    @Composable
    fun exportToFileRequest(
        content: String,
        mimeType: String = "text/plain",
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        val context = LocalContext.current
        val ioCoroutineScope = rememberCoroutineScope { Dispatchers.IO }

        val exportFileLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(mimeType)
        ) { uri ->
            uri?.let {
                ioCoroutineScope.launch {
                    if (
                        exportToUri(
                            content = content,
                            uri = uri,
                            contentResolver = context.contentResolver
                        )
                    ) {
                        onSuccess()
                    } else {
                        onFailure()
                    }
                }
            }
        }

        val filename = defaultFilename()
        LaunchedEffect(this) {
            exportFileLauncher.launch(filename)
        }
    }
}

@Composable
fun rememberReportExporter(reportIds: List<String>) = ReportExporter(
    LocalContext.current,
    reportIds
)