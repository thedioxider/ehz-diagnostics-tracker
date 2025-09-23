package space.diomentia.ehzdiagnosticstracker.data

import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.charleskorn.kaml.InvalidPropertyValueException
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import space.diomentia.ehzdiagnosticstracker.R
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset

@Stable
object AppState {
    const val REPORT_CONFIG_FILENAME = "report.conf.yaml"
    const val CUSTOMER_INFO_FILENAME = "customer.yaml"
    const val ORGANIZATION_INFO_FILENAME = "organization.yaml"

    var inspectionMode = true
    var dataDir: File? = null
    var reportDb: ReportDatabase? = null

    private inline fun <reified T> lazyFileFetch(filename: String): Lazy<MutableState<T?>> = lazy {
        val dataDir = dataDir
            ?: if (!inspectionMode)
                error("no data directory passes")
            else
                return@lazy mutableStateOf(null)
        File(dataDir, filename).let {
            if (!it.isFile) {
                return@lazy mutableStateOf(null)
            }
            return@lazy mutableStateOf(Yaml.default.decodeFromString(it.readText()))
        }
    }

    val reportConfig: MutableState<ReportConfig?> by lazyFileFetch(REPORT_CONFIG_FILENAME)
    val customerInfo: MutableState<OrganizationInfo?> by lazyFileFetch(CUSTOMER_INFO_FILENAME)
    val organizationInfo: MutableState<OrganizationInfo?> by lazyFileFetch(
        ORGANIZATION_INFO_FILENAME
    )

    object CurrentDiagnosticSession {
        var id by mutableStateOf<Int?>(null)
        var dateTime by mutableStateOf<LocalDateTime>(LocalDateTime.now())
        var diagnosticsType by mutableIntStateOf(0)
        var reportType by mutableStateOf<Int?>(null)
        var location by mutableStateOf<Location?>(null)
        var data = mutableMapOf<String, MutableState<out Any?>>()

        fun reset() {
            id = null
            dateTime = LocalDateTime.now()
            reportType = null
            location = null
            data.forEach { (_, v) -> (v as MutableState<Any?>).value = null }
        }

        fun insertToDb(): Boolean {
            val reportDb = reportDb
            val reportConfig = reportConfig.value
            val id = id
            val reportType = reportType
            val location = location
            val data = data
            if (id == null
                || reportType == null
                || location == null
                || reportConfig == null
                || reportDb == null
            ) return false
            val paramSet = (reportConfig.reportTypes[reportType].parameters ?: emptyList())
                .asSequence()
                .map { it.id }
                .toSet()
            val report = DiagnosticReport(
                data.all { (k, v) ->
                    k !in paramSet || v.value != null
                },
                id.toString(),
                dateTime.toEpochSecond(ZoneOffset.UTC),
                diagnosticsType,
                reportConfig.reportTypes[reportType].id,
                Coordinates(location.latitude, location.altitude)
            )
            val reportId = reportDb.diagnosticReportDao().insertReport(report)
            val dataEntries = data
                .filter { (_, v) -> v.value != null }
                .map { (k, v) ->
                    ReportDataEntry(
                        report = reportId,
                        characteristic = k,
                        value = v.value.toString()
                    )
                }
                .toTypedArray()
            reportDb.diagnosticReportDao().insertData(*dataEntries)
            reset()
            return true
        }
    }

    fun init(context: Context) {
        inspectionMode = false
        dataDir = context.dataDir
        reportDb = Room.databaseBuilder(context, ReportDatabase::class.java, REPORT_DATABASE_NAME)
            // TODO: get rid of this crutch
            .allowMainThreadQueries()
            .build()
    }

    @Composable
    inline fun <reified T> localYamlConfigCopyLauncher(
        filename: String,
        crossinline onResult: (T) -> Unit
    ): ManagedActivityResultLauncher<Array<String>, Uri?> {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = LocalSnackbarHostState.current

        return rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) {
            it ?: return@rememberLauncherForActivityResult
            context.contentResolver.openInputStream(it)?.use { iStream ->
                try {
                    val bytes = iStream.readBytes()
                    onResult(Yaml.default.decodeFromString<T>(String(bytes)))
                    FileOutputStream(File(context.dataDir, filename)).use { oStream ->
                        oStream.write(bytes)
                    }
                } catch (e: Exception) {
                    when (e) {
                        is InvalidPropertyValueException, is IllegalArgumentException ->
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    "${context.getString(R.string.something_wrong)}:\n${e.message}"
                                )
                            }

                        else -> throw e
                    }
                }
            }
        }
    }
}