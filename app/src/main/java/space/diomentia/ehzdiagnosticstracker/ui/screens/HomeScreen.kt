package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmOutlinedButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.clickableWithLabel
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun HomeScreen(
    onNewReport: () -> Unit,
    onHistory: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit
) {
    Scaffold(
        topBar = { PtmTopBar { Text(stringResource(R.string.app_name)) } },
        bottomBar = {
            PtmBottomBar(
                navigation = {
                    ActionButton(
                        icon = painterResource(R.drawable.ic_person),
                        label = stringResource(R.string.profile)
                    ) { onProfile() }
                },
                actions = {
                    ActionButton(
                        icon = rememberVectorPainter(Icons.Outlined.Settings),
                        label = stringResource(R.string.settings)
                    ) { onSettings() }
                }
            )
        },
        snackbarHost = { PtmSnackbarHost(LocalSnackbarHostState.current) }
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 40.dp)
        ) {
            HomeActionButton(
                label = stringResource(R.string.start_diagnostics),
                icon = painterResource(R.drawable.ic_contract_edit),
                modifier = Modifier.fillMaxWidth(),
                onClick = onNewReport
            )
            HomeActionButton(
                label = stringResource(R.string.diagnostics_history),
                icon = painterResource(R.drawable.ic_history),
                modifier = Modifier.fillMaxWidth(),
                onClick = onHistory
            )
            HomeActionButton(
                label = stringResource(R.string.export_reports),
                icon = painterResource(R.drawable.ic_file_export),
                modifier = Modifier.fillMaxWidth(),
                onClick = onExport
            )
        }
    }
}

@Composable
private fun HomeActionButton(
    label: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickableWithLabel(
            label = label,
            interactionSource = interactionSource,
            indication = null,
            action = onClick
        )
    ) {
        PtmOutlinedButton(
            roundedCorners = true,
            modifier = Modifier
                .height(100.dp)
                .aspectRatio(1f)
                .indication(interactionSource, ripple())
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.fillMaxSize())
        }
        Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Preview
@Composable
private fun HomeScreenPreview() = PtmTheme { HomeScreen({}, {}, {}, {}, {}) }
