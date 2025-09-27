package space.diomentia.ehzdiagnosticstracker

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.icu.text.DecimalFormatSymbols
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalLocationProvider
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ParameterType
import space.diomentia.ehzdiagnosticstracker.data.ReportConfig
import space.diomentia.ehzdiagnosticstracker.data.ReportParameter
import space.diomentia.ehzdiagnosticstracker.data.ReportType
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledIconButton
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme


private val LocalReportType = compositionLocalOf { 0 }

@Composable
fun ReportEditor(
    modifier: Modifier = Modifier
) {
    var diagnosticType by remember {
        mutableIntStateOf(
            AppState.CurrentDiagnosticSession.diagnosticType ?: 0
        )
    }
    diagnosticType = AppState.CurrentDiagnosticSession.diagnosticType ?: diagnosticType

    CompositionLocalProvider(
        LocalCoroutineScope provides rememberCoroutineScope(),
        LocalReportType provides diagnosticType
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

                AppState.reportConfig.value?.diagnosticTypes[diagnosticType]?.parameters?.forEachIndexed { i, parameter ->
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
        Text(
            AppState.CurrentDiagnosticSession.location?.let {
                "${it.latitude} ${it.altitude}"
            } ?: stringResource(R.string.unknown),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f, fill = true)
        )
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
            keyboardType: KeyboardType = KeyboardType.Text,
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
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.alpha(.5f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.weight(1f)
                )
                if (parameter.unit != null) {
                    Text(
                        parameter.unit,
                        fontWeight = FontWeight.SemiBold,
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
                    keyboardType = KeyboardType.Number
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
                    keyboardType = KeyboardType.Decimal
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
    val activity = LocalActivity.current
    val reportConfig = AppState.reportConfig.value ?: return
    val derivedReportType by LocalReportType.current.let { remember { derivedStateOf { it } } }
    remember(derivedReportType) {
        (reportConfig.diagnosticTypes[derivedReportType].parameters ?: emptyList())
            .asSequence()
            .map { it.id }
            .toSet()
    }
    val enabled = with(AppState.CurrentDiagnosticSession) {
        id != null
                && diagnosticType != null
                && location != null
        // && data.all { (k, v) ->
        //     k !in paramSet || v.value != null
        // }
    }

    PtmFilledButton(
        enabled = enabled,
        onClick = {
            AppState.CurrentDiagnosticSession.insertToDb()
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
    AppState.CurrentDiagnosticSession.diagnosticType = 0

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