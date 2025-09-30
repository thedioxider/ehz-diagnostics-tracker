package space.diomentia.ehzdiagnosticstracker.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.Resources
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.charleskorn.kaml.InvalidPropertyValueException
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppPreferences.getPreference
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneId

@Stable
object AppState {
    const val REPORT_CONFIG_FILENAME = "report.conf.yaml"

    var inspectionMode = true
    var dataDir: File? = null
    var resources: Resources? = null
    var reportDb: ReportDatabase? = null
    var profileDb: ProfileDatabase? = null
    var packageInfo: PackageInfo? = null

    fun init(context: Context) {
        inspectionMode = false
        dataDir = context.dataDir
        resources = context.resources
        reportDb = Room.databaseBuilder(context, ReportDatabase::class.java, REPORT_DATABASE_NAME)
            // TODO: get rid of this crutch
            .allowMainThreadQueries()
            .build()
        profileDb =
            Room.databaseBuilder(context, ProfileDatabase::class.java, PROFILE_DATABASE_NAME)
                .allowMainThreadQueries()
                .build()
        packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        val initialAreaEntries = Yaml.default.decodeFromString<InitialAreaEntries>(
            String(
                resources?.openRawResource(R.raw.initial_area_entries)?.readBytes() ?: ByteArray(0)
            )
        )
        initialAreaEntries.diagnosticObjects.forEach {
            reportDb
                ?.diagnosticAreaDao()
                ?.insertDiagnosticObject(it)
        }
        initialAreaEntries.diagnosticAreas.forEach {
            reportDb
                ?.diagnosticAreaDao()
                ?.insertDiagnosticArea(it)
        }
    }

    var adminSession by mutableStateOf(false)
        private set

    suspend fun elevateToAdmin(context: Context, password: String): Boolean {
        val encrypted = PasswordHash.encrypt(password) ?: return false
        val correct = PasswordHash.fromHash(
            context.getPreference(
                AppPreferences.KEY_ADMIN_HASHWORD,
                AppPreferences.DEFAULT_ADMIN_HASHWORD
            )
        )
        return (encrypted == correct).also { adminSession = it }
    }

    var profileSession by mutableStateOf<Profile?>(null)
        private set

    fun loginProfile(profile: Profile, password: String) =
        (PasswordHash.encrypt(password) == PasswordHash.fromHash(profile.hashword)).also {
            if (it) profileSession = profile else profileSession = null
        }

    var diagnosticObject by mutableStateOf<DiagnosticObjectRelative?>(null)

    private inline fun <reified T> lazyFileFetch(
        filename: String,
        resource: Int? = null
    ): Lazy<MutableState<T?>> = lazy {
        val dataDir = dataDir
            ?: if (!inspectionMode)
                error("no data directory passed")
            else
                return@lazy mutableStateOf(null)
        val resources = resources
            ?: if (!inspectionMode)
                error("no resources passed")
            else
                return@lazy mutableStateOf(null)
        File(dataDir, filename).let {
            if (!it.isFile) {
                return@lazy mutableStateOf(
                    resource?.let {
                        Yaml.default.decodeFromString(
                            String(
                                resources.openRawResource(it).readBytes()
                            )
                        )
                    }
                )
            }
            return@lazy mutableStateOf(Yaml.default.decodeFromString(it.readText()))
        }
    }

    val reportConfig: MutableState<ReportConfig?> by lazyFileFetch(
        REPORT_CONFIG_FILENAME,
        R.raw.report_conf
    )

    object CurrentDiagnosticSession {
        var id by mutableStateOf<String?>(null)
        var dateTime by mutableStateOf<LocalDateTime>(LocalDateTime.now())
        var reportType by mutableIntStateOf(0)
        var diagnosticType by mutableStateOf<Int?>(null)
        var location by mutableStateOf<Location?>(null)
        var data = mutableMapOf<String, MutableState<out Any?>>()

        fun reset() {
            id = null
            dateTime = LocalDateTime.now()
            diagnosticType = null
            location = null
            data.forEach { (_, v) -> (v as MutableState<Any?>).value = null }
        }

        fun insertToDb(): Boolean {
            val reportDb = reportDb
            val reportConfig = reportConfig.value
            val id = id
            val diagnosticType = diagnosticType
            val location = location
            val inspector = profileSession
            val data = data
            if (id == null
                || diagnosticType == null
                || location == null
                || reportConfig == null
                || reportDb == null
                || inspector == null
            ) return false
            val paramSet = (reportConfig.diagnosticTypes[diagnosticType].parameters ?: emptyList())
                .asSequence()
                .map { it.id }
                .toSet()
            val report = DiagnosticReport(
                data.all { (k, v) ->
                    k !in paramSet || v.value != null
                },
                id.toString(),
                dateTime.atZone(ZoneId.systemDefault()).toEpochSecond(),
                this@CurrentDiagnosticSession.reportType.toString(),
                reportConfig.diagnosticTypes[diagnosticType].id,
                Coordinates(location.latitude, location.altitude),
                inspector.name
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

    @Composable
    inline fun <reified T> localYamlConfigCopyLauncher(
        filename: String,
        crossinline onResult: (T) -> Unit
    ): ManagedActivityResultLauncher<Array<String>, Uri?> {
        val context = LocalContext.current
        val coroutineScope = LocalCoroutineScope.current
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
                            coroutineScope?.launch {
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