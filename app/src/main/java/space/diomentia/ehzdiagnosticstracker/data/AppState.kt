package space.diomentia.ehzdiagnosticstracker.data

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
import com.charleskorn.kaml.InvalidPropertyValueException
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import space.diomentia.ehzdiagnosticstracker.R
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

@Stable
object AppState {
    const val REPORT_CONFIG_FILENAME = "report.conf.yaml"
    const val CUSTOMER_INFO_FILENAME = "customer.yaml"

    var dataDir: File? = null
    val reportConfig: MutableState<ReportConfig?> by lazy {
        val dataDir = dataDir ?: error("no data directory passed")
        File(dataDir, REPORT_CONFIG_FILENAME).let {
            if (!it.isFile) {
                return@lazy mutableStateOf(null)
            }
            return@lazy mutableStateOf(Yaml.default.decodeFromString(it.readText()))
        }
    }
    val customerInfo: MutableState<CustomerInfo?> by lazy {
        val dataDir = dataDir ?: error("no data directory passes")
        File(dataDir, CUSTOMER_INFO_FILENAME).let {
            if (!it.isFile) {
                return@lazy mutableStateOf(null)
            }
            return@lazy mutableStateOf(Yaml.default.decodeFromString(it.readText()))
        }
    }

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
                    onResult(Yaml.default.decodeFromString<T>(String(iStream.readBytes())))
                    FileOutputStream(File(context.dataDir, filename)).use { oStream ->
                        iStream.copyTo(oStream)
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
                    }
                }
            }
        }
    }
}