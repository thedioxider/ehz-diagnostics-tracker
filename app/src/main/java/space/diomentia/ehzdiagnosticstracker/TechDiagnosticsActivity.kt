package space.diomentia.ehzdiagnosticstracker

import android.app.Activity.RESULT_CANCELED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DoneOutline
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

private enum class State {
    Configuration,
    Editor;

    companion object {
        fun canOpenEditor(): Boolean {
            return with(AppState.CurrentDiagnosticSession) {
                id != null && worksContentsType != null
            }
        }
    }
}

private val LocalState = compositionLocalOf<MutableState<State>> { error("no local state") }

class TechDiagnosticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        val snackbarHostState = SnackbarHostState()
        val areaId = AppState.CurrentDiagnosticSession.id
        if (areaId == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        setContent {
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalState provides remember {
                    mutableStateOf(
                        if (State.canOpenEditor()) State.Editor else State.Configuration
                    )
                }
            ) {
                Content()
            }
        }
    }
}

@Preview
@Composable
private fun Content() {
    val activity = LocalActivity.current

    PtmTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            label = stringResource(R.string.back)
                        ) {
                            activity?.run {
                                setResult(RESULT_CANCELED)
                                finish()
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                        end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                    )
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(innerPadding.calculateTopPadding()))

                Text(
                    "${stringResource(R.string.area)} ${AppState.CurrentDiagnosticSession.id}",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 12.dp)
                )

                AnimatedVisibility(LocalState.current.value == State.Configuration) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.corrosion_diagnostics_type),
                            textAlign = TextAlign.Start,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(top = 8.dp)
                        )
                        CorrosionTypeSelector(
                            modifier = Modifier.padding(8.dp)
                        )

                        WorksContentsList(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }

                ApplyButton()

                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(innerPadding.calculateBottomPadding() + 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CorrosionTypeSelector(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = stringArrayResource(R.array.corrosion_diagnostics_types)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            value = options[AppState.CurrentDiagnosticSession.corrosionDiagnosticsType],
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(
            expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        AppState.CurrentDiagnosticSession.corrosionDiagnosticsType = i
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun WorksContentsList(
    modifier: Modifier = Modifier
) {
    var state by LocalState.current
    val worksContentsTechEvaluationOptions =
        stringArrayResource(R.array.works_contents_tech_evaluation_list)
    val worksContentsOptions = stringArrayResource(R.array.works_contents_list)
    val chooseOption = { type: Int ->
        AppState.CurrentDiagnosticSession.worksContentsType = type
        state = State.Editor
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = modifier
    ) {
        Text(
            stringResource(R.string.works_contents),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(8.dp)
        )
        Text(
            "${stringResource(R.string.works_contents_tech_evaluation)}:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(start = 16.dp)
                .padding(vertical = 4.dp, horizontal = 8.dp)
        )
        worksContentsTechEvaluationOptions.forEachIndexed { i, option ->
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { chooseOption(i) }
                    .padding(start = 48.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    if (AppState.CurrentDiagnosticSession.worksContentsType == i)
                        Icons.Outlined.RadioButtonChecked
                    else
                        Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = stringResource(R.string.works_contents),
                    modifier = Modifier
                        .height(18.dp)
                        .indication(interactionSource, ripple(bounded = false))
                )
                Text(
                    option,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .padding(horizontal = 12.dp)
                )
            }
        }
        worksContentsOptions.forEachIndexed { i, option ->
            val interactionSource = remember { MutableInteractionSource() }
            val mType = worksContentsTechEvaluationOptions.size + i
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { chooseOption(mType) }
                    .padding(start = 16.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    if (AppState.CurrentDiagnosticSession.worksContentsType == mType)
                        Icons.Outlined.RadioButtonChecked
                    else
                        Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = stringResource(R.string.works_contents),
                    modifier = Modifier
                        .height(18.dp)
                        .indication(interactionSource, ripple(bounded = false))
                )
                Text(
                    option,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun ApplyButton(
    modifier: Modifier = Modifier
) {
    var state by LocalState.current
    val enabled = when (state) {
        State.Configuration -> State.canOpenEditor()
        State.Editor -> true
    }
    PtmTheme {
        PtmFilledButton(
            enabled = enabled,
            onClick = {
                state = when (state) {
                    State.Configuration -> State.Editor
                    State.Editor -> State.Configuration
                }
            },
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (state) {
                        State.Configuration -> Icons.Outlined.DoneOutline
                        State.Editor -> Icons.Outlined.Settings
                    },
                    contentDescription = stringResource(R.string.apply),
                    modifier = Modifier
                        .padding(4.dp)
                        .height(18.dp)
                )
                Text(
                    when (state) {
                        State.Configuration -> stringResource(R.string.apply)
                        State.Editor -> stringResource(R.string.edit_report_type)
                    },
                    modifier = Modifier
                        .padding(4.dp)
                )
            }
        }
    }
}