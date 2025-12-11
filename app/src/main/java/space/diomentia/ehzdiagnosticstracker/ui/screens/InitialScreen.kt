package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.SideArrowContainer
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun InitialScreen(
    onAuthorize: () -> Unit
) {
    LocalContext.current

    PtmTheme {
        Scaffold(
            topBar = { PtmTopBar(title = { Text(stringResource(R.string.app_name)) }) }
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                    modifier = Modifier.Companion.weight(1f)
                ) {
                    SideArrowContainer(toRight = false) {
                        Column(
                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                            modifier = Modifier.Companion.padding(24.dp)
                        ) {
                            Text(
                                stringResource(R.string.developer_name),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Companion.Center,
                                modifier = Modifier.Companion
                                    .fillMaxWidth(.8f)
                            )
                            Image(
                                painterResource(R.drawable.logo_ptm),
                                contentDescription = stringResource(R.string.developer_name),
                                modifier = Modifier.Companion
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp)
                                    .height(200.dp)
                            )
                        }
                    }
                    PtmFilledButton(
                        onClick = onAuthorize
                    ) {
                        Text(
                            stringResource(R.string.authorize),
                            fontSize = 18.sp,
                            modifier = Modifier.Companion.padding(6.dp)
                        )
                    }
                }
                Text(
                    "${stringResource(R.string.version)}: ${
                        AppState.packageInfo?.versionName ?: stringResource(
                            R.string.unknown
                        )
                    }",
                    fontWeight = FontWeight.Companion.SemiBold,
                    modifier = Modifier.Companion
                        .padding(8.dp)
                        .alpha(.5f)
                )
            }
        }
    }
}

@Preview
@Composable
private fun InitScreenPreview() = InitialScreen {}