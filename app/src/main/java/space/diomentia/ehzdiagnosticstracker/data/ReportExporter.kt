package space.diomentia.ehzdiagnosticstracker.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.diomentia.ehzdiagnosticstracker.api.toDto
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class ReportExporter(
    val context: Context,
    val reportIds: List<String>
) {
    private val dao = ReportDatabase.getInstance(context).reportDao()

    enum class Mime(val type: String) {
        Plain("text/plain"),
        Json("application/json")
    }

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

    fun toJson(): String {
        return Json.default.encodeToString(
            dao.getFullReportsByIdsOnce(reportIds).map { it.toDto() }
        )
    }

    @Composable
    fun exportToFileAction(
        content: String,
        mime: Mime = Mime.Plain,
        onCancel: () -> Unit,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        val context = LocalContext.current
        val ioCoroutineScope = rememberCoroutineScope { Dispatchers.IO }

        val exportFileLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(mime.type)
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
            onCancel()
        }

        val filename = defaultFilename()
        LaunchedEffect(this) {
            exportFileLauncher.launch(filename)
        }
    }
}

@Composable
fun rememberReportExporter(reportIds: List<String>): ReportExporter {
    val context = LocalContext.current
    return remember(reportIds) {
        ReportExporter(context, reportIds)
    }
}
