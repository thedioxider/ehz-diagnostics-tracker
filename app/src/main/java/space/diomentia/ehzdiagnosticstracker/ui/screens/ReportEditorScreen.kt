package space.diomentia.ehzdiagnosticstracker.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.DecimalFormatSymbols
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalLocationProvider
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ParameterType
import space.diomentia.ehzdiagnosticstracker.data.ReportConfig
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.ReportParameter
import space.diomentia.ehzdiagnosticstracker.data.ReportType
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledIconButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.RequestConfigScreen
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.time.format.DateTimeFormatter

private enum class UiState {
    Configuration,
    Editor;

    companion object {
        fun canOpenEditor(): Boolean {
            return with(AppState.CurrentDiagnosticSession) {
                diagnosticPoint != null && reportType != null
            }
        }
    }
}

private val LocalUiState =
    compositionLocalOf<MutableState<UiState>> { mutableStateOf(UiState.Configuration) }

@Composable
fun ReportEditorScreen() {
    val activity = LocalActivity.current
    val snackbarHostState = LocalSnackbarHostState.current
    val diagnosticObject by AppState.CurrentDiagnosticSession.diagnosticObject?.let { id ->
        ReportDatabase.Companion.getInstance()
            .diagnosticAreaDao()
            .getObjectById(id)
            .collectAsStateWithLifecycle(null)
    } ?: remember { mutableStateOf(null) }
    val diagnosticArea by AppState.CurrentDiagnosticSession.diagnosticArea?.let { id ->
        ReportDatabase.Companion.getInstance()
            .diagnosticAreaDao()
            .getAreaById(id)
            .collectAsStateWithLifecycle(null)
    } ?: remember { mutableStateOf(null) }
    val diagnosticPoint by AppState.CurrentDiagnosticSession.diagnosticPoint?.let { diagnosticPoint ->
        ReportDatabase.Companion.getInstance()
            .diagnosticAreaDao()
            .getPointById(diagnosticPoint)
            .collectAsStateWithLifecycle(null)
    } ?: remember { mutableStateOf(null) }

    CompositionLocalProvider(
        LocalUiState provides remember { mutableStateOf(if (UiState.canOpenEditor()) UiState.Editor else UiState.Configuration) }
    ) {
        var state by LocalUiState.current
        if (AppState.CurrentDiagnosticSession.diagnosticPoint == null) activity?.finish()
        if (AppState.CurrentDiagnosticSession.reportType == null) state = UiState.Configuration
        PtmTheme {
            Scaffold(
                topBar = {
                    PtmTopBar(
                        title = {
                            Text(stringResource(R.string.technical_diagnostics))
                        }
                    )
                },
                bottomBar = {
                    PtmBottomBar(
                        navigation = {
                            ActionButton(
                                icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                                label = stringResource(R.string.back)
                            ) {
                                activity?.run {
                                    setResult(Activity.RESULT_CANCELED)
                                    finish()
                                }
                            }
                        }
                    )
                },
                snackbarHost = { PtmSnackbarHost(snackbarHostState) },
                modifier = Modifier.Companion.fillMaxSize()
            ) { innerPadding ->
                if (AppState.reportConfig.value == null) {
                    val configChooserLauncher =
                        AppState.localYamlConfigCopyLauncher<ReportConfig>(AppState.REPORT_CONFIG_FILENAME) {
                            AppState.reportConfig.value = it
                        }
                    RequestConfigScreen(
                        label = stringResource(R.string.import_report_config),
                        modifier = Modifier.Companion
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        configChooserLauncher.launch(arrayOf("application/*"))
                    }
                    return@Scaffold
                }
                ScrollableColumn(
                    innerPadding,
                    modifier = Modifier.Companion.padding(horizontal = 16.dp)
                ) {
                    Text(
                        "${stringResource(R.string.point)} ${
                            diagnosticPoint?.point?.inventoryId ?: stringResource(R.string.unknown)
                        }",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 36.sp,
                        textAlign = TextAlign.Companion.Center,
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    )
                    Text(
                        diagnosticObject?.diagnosticObject?.name
                            ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Companion.Center,
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                    )
                    Text(
                        "${stringResource(R.string.area)} ${
                            diagnosticArea?.diagnosticArea?.name ?: stringResource(
                                R.string.unknown
                            )
                        }",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Companion.Center,
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                    )
                    Text(
                        "${stringResource(R.string.report_by)} " + AppState.profileSession?.let { "${it.surname} ${it.name[0]}." },
                        textAlign = TextAlign.Companion.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.Companion.padding(top = 8.dp)
                    )
                    Text(
                        AppState.CurrentDiagnosticSession.dateTime
                            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.Companion
                            .alpha(.5f)
                            .padding(bottom = 16.dp)
                    )

                    AnimatedVisibility(state == UiState.Editor) {
                        Text(
                            AppState.CurrentDiagnosticSession.reportType?.let { reportType ->
                                AppState.reportConfig.value?.reportTypes[reportType]?.name
                            } ?: stringResource(R.string.unknown),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp)
                        )
                    }

                    AnimatedVisibility(state == UiState.Configuration) {
                        ReportConfiguration(Modifier.Companion.fillMaxWidth())
                    }

                    ApplyButton()

                    AnimatedVisibility(state == UiState.Editor)
                    {
                        ReportEditor(
                            Modifier.Companion
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                    }

                    Spacer(Modifier.Companion.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ReportTypesList(
    modifier: Modifier = Modifier
) {
    val reportConfig = AppState.reportConfig.value
    if (reportConfig == null) {
        Text(
            stringResource(R.string.no_report_configuration),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Companion.Center,
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(8.dp)
        )
        return
    }

    var state by LocalUiState.current

    Column(
        horizontalAlignment = Alignment.Companion.Start,
        verticalArrangement = Arrangement.Top,
        modifier = modifier
    ) {
        Text(
            stringResource(R.string.works_contents),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.Companion.padding(vertical = 8.dp)
        )

        reportConfig.reportTypes.forEachIndexed { i, rClass ->
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                verticalAlignment = Alignment.Companion.CenterVertically,
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        AppState.CurrentDiagnosticSession.reportType = i
                        state = UiState.Editor
                    }
                    .padding(start = 16.dp)
                    .padding(vertical = 12.dp)
            ) {
                Icon(
                    if (AppState.CurrentDiagnosticSession.reportType == i)
                        painterResource(R.drawable.ic_radio_button_checked)
                    else
                        painterResource(R.drawable.ic_radio_button_unchecked),
                    contentDescription = stringResource(R.string.works_contents),
                    modifier = Modifier.Companion
                        .height(18.dp)
                        .indication(interactionSource, ripple(bounded = false))
                )
                Text(
                    rClass.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.Companion
                        .weight(1f, fill = true)
                        .padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ReportConfiguration(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        modifier = modifier
    ) {
        ReportTypesList(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

@Composable
private fun ApplyButton(
    modifier: Modifier = Modifier
) {
    var state by LocalUiState.current
    val enabled = when (state) {
        UiState.Configuration -> UiState.canOpenEditor()
        UiState.Editor -> true
    }
    PtmTheme {
        PtmFilledButton(
            enabled = enabled,
            onClick = {
                state = when (state) {
                    UiState.Configuration -> UiState.Editor
                    UiState.Editor -> UiState.Configuration
                }
            },
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Icon(
                    when (state) {
                        UiState.Configuration -> painterResource(R.drawable.ic_done_outline)
                        UiState.Editor -> rememberVectorPainter(Icons.Outlined.Settings)
                    },
                    contentDescription = stringResource(R.string.apply),
                    modifier = Modifier.Companion
                        .padding(4.dp)
                        .height(18.dp)
                )
                Text(
                    when (state) {
                        UiState.Configuration -> stringResource(R.string.apply)
                        UiState.Editor -> stringResource(R.string.edit_report_type)
                    },
                    modifier = Modifier.Companion
                        .padding(4.dp)
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun TechDiagnosticsScreenPreview() {
    AppState.reportConfig.value = ReportConfig(
        listOf(
            ReportType(
                "foo0",
                "Test", listOf(
                    ReportParameter(
                        "bar0",
                        "Very long and wordy parameter name with like lots of words in it " +
                                "and that's why it doesn't even fit the screen",
                        ParameterType.Bool
                    ),
                    ReportParameter(
                        "bar1",
                        "Some other parameter",
                        ParameterType.Decimal,
                        "kV"
                    )
                )
            )
        )
    )
    AppState.CurrentDiagnosticSession.reportType = 0
    LocalUiState.current.value = UiState.Editor

    PtmTheme {
        ReportEditorScreen()
    }
}

private val LocalReportType = compositionLocalOf { 0 }

@Composable
private fun ReportEditor(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var diagnosticType by remember {
        mutableIntStateOf(
            AppState.CurrentDiagnosticSession.reportType ?: 0
        )
    }
    diagnosticType = AppState.CurrentDiagnosticSession.reportType ?: diagnosticType

    CompositionLocalProvider(
        LocalCoroutineScope provides rememberCoroutineScope(),
        LocalReportType provides diagnosticType,
        LocalLocationProvider provides LocationServices.getFusedLocationProviderClient(context)
    ) {
        PtmTheme {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
            ) {
                Text(
                    stringResource(R.string.location),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                LocationField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 24.dp)
                        .padding(start = 32.dp, end = 16.dp)
                )

                AppState.reportConfig.value?.reportTypes[diagnosticType]?.parameters?.forEachIndexed { i, parameter ->
                    val value = remember {
                        AppState.CurrentDiagnosticSession.data[parameter.id]
                            ?: when (parameter.type) {
                                ParameterType.Text, ParameterType.Photo -> mutableStateOf<String?>(
                                    null
                                )

                                ParameterType.Integer -> mutableStateOf<Int?>(null)
                                ParameterType.Decimal -> mutableStateOf<Float?>(null)
                                ParameterType.Bool -> mutableStateOf<Boolean?>(null)
                            }.also {
                                AppState.CurrentDiagnosticSession.data[parameter.id] = it
                            }
                    }
                    ReportField(
                        parameter,
                        value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }

                SaveButton(Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun LocationField(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    val locationProvider = LocalLocationProvider.current

    var isSyncingLocation by remember { mutableStateOf(true) }
    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isSyncingLocation = true
    }
    LaunchedEffect(isSyncingLocation) syncLocation@{
        if (!isSyncingLocation) return@syncLocation
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            isSyncingLocation = false
            return@syncLocation
        }
        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setMaxUpdateAgeMillis(15000L)
            .build()
        locationProvider?.getCurrentLocation(currentLocationRequest, null)
            ?.addOnSuccessListener {
                AppState.CurrentDiagnosticSession.location = it
            }
            ?.addOnFailureListener {
                coroutineScope?.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        "${context.getString(R.string.location)}: ${
                            context.getString(
                                R.string.something_wrong
                            )
                        }..."
                    )
                }
            }
            ?.addOnCompleteListener { isSyncingLocation = false }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val location = AppState.CurrentDiagnosticSession.location
        PtmFilledButton(
            enabled = location != null,
            onClick = {
                location?.let {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "geo:0,0?q=${it.latitude},${it.longitude}".toUri()
                        )
                    )
                }
            },
            modifier = Modifier.weight(1f, fill = true)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = stringResource(R.string.location),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(R.string.show_area_on_map_short))
            }
        }
        PtmFilledIconButton(
            onClick = { isSyncingLocation = true },
        ) {
            Icon(
                Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(.8f)
                    .aspectRatio(1f)
            )
        }
    }
}

@Composable
private fun ReportField(
    parameter: ReportParameter,
    // FIXME: using * type seems to be a pretty bad solution.
    //   need to come up with something more failsafe and neat.
    //   but so far it is used only locally in this file, so it should be ok.
    value: MutableState<out Any?>,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                parameter.name,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            )
            if (parameter.type == ParameterType.Bool) {
                @Suppress("UNCHECKED_CAST")
                var v by value as MutableState<Boolean?>
                FilledTonalIconButton(
                    onClick = { v = v != true },
                    shape = RectangleShape,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .border(BorderStroke(6.dp, MaterialTheme.colorScheme.primary))
                ) {
                    Icon(
                        if (v == true) Icons.Outlined.Check else Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(.8f)
                            .aspectRatio(1f)
                            .alpha(if (v != null) 1f else 0f)
                    )
                }
            }
        }

        @Composable
        fun ParamField(
            value: String,
            keyboardType: KeyboardType = KeyboardType.Companion.Text,
            onValueChange: (String) -> Unit
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 16.dp)
            ) {
                TextField(
                    value,
                    onValueChange = onValueChange,
                    placeholder = {
                        Text(
                            stringResource(R.string.input_value),
                            fontStyle = FontStyle.Companion.Italic,
                            maxLines = 1,
                            modifier = Modifier.alpha(.5f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Companion.Next
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (parameter.unit != null) {
                    Text(
                        parameter.unit,
                        fontWeight = FontWeight.Companion.SemiBold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
        when (parameter.type) {
            ParameterType.Text, ParameterType.Photo -> {
                @Suppress("UNCHECKED_CAST")
                var v by value as MutableState<String?>
                ParamField(value = v ?: "") { v = it }
            }

            ParameterType.Integer -> {
                @Suppress("UNCHECKED_CAST")
                var v by value as MutableState<Int?>
                var text by remember { mutableStateOf(v?.toString() ?: "") }
                ParamField(
                    value = text,
                    keyboardType = KeyboardType.Companion.Number
                ) {
                    text = it.filter { it.isDigit() }
                    v = text.toIntOrNull()
                }
            }

            ParameterType.Decimal -> {
                @Suppress("UNCHECKED_CAST")
                var v by value as MutableState<Float?>
                var text by remember { mutableStateOf(v?.toString() ?: "") }
                ParamField(
                    value = text,
                    keyboardType = KeyboardType.Companion.Decimal
                ) {
                    text =
                        it.filter {
                            it.isDigit() || it == DecimalFormatSymbols.getInstance().decimalSeparator
                        }
                    v = text.toFloatOrNull()
                }
            }

            ParameterType.Bool -> Unit
        }
    }
}

@Composable
private fun SaveButton(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val reportConfig = AppState.reportConfig.value ?: return
    val derivedReportType by LocalReportType.current.let { remember { derivedStateOf { it } } }
    remember(derivedReportType) {
        (reportConfig.reportTypes[derivedReportType].parameters ?: emptyList())
            .asSequence()
            .map { it.id }
            .toSet()
    }
    val enabled = with(AppState.CurrentDiagnosticSession) {
        diagnosticPoint != null
                && reportType != null
                && location != null
    }

    PtmFilledButton(
        enabled = enabled,
        onClick = {
            AppState.CurrentDiagnosticSession.reportToDb(context)
            activity?.run {
                setResult(Activity.RESULT_OK)
                finish()
            }
        },
        modifier = modifier
    ) {
        Icon(
            painterResource(R.drawable.ic_save),
            contentDescription = stringResource(R.string.save),
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(stringResource(R.string.save))
    }
}


@Preview(showBackground = true)
@Composable
private fun ReportEditorPreview() {
    AppState.reportConfig.value = ReportConfig(
        listOf(
            ReportType(
                "foo0",
                "Test", listOf(
                    ReportParameter(
                        "bar0",
                        "Very long and wordy parameter name with like lots of words in it " +
                                "and that's why it doesn't even fit the screen",
                        ParameterType.Bool
                    ),
                    ReportParameter(
                        "bar1",
                        "Some other parameter",
                        ParameterType.Decimal,
                        "kV"
                    )
                )
            )
        )
    )
    AppState.CurrentDiagnosticSession.reportType = 0

    PtmTheme {
        ReportEditor()
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportFieldPreview() {
    PtmTheme {
        ReportField(
            ReportParameter(
                "foo0",
                "Very long and wordy parameter name with like lots of words in it " +
                        "and that's why it doesn't even fit the screen",
                ParameterType.Decimal,
                unit = "kV"
            ),
            remember { mutableStateOf<Float?>(1f) }
        )
    }
}