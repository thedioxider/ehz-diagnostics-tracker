package space.diomentia.ehzdiagnosticstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.SideArrowContainer
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.time.LocalDate

class InitActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.init(this)
        setupEdgeToEdge(this)
        setContent {
            if (LocalDate.now() < LocalDate.of(2026, 1, 1)) {
                Content()
            } else {
                NowYouPayUs()
            }
        }
    }
}

@Composable
private fun Content() {
    val context = LocalContext.current
    val activity = LocalActivity.current

    PtmTheme {
        Scaffold(
            topBar = { PtmTopBar(title = { Text(stringResource(R.string.app_name)) }) }
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    SideArrowContainer(toRight = false) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                stringResource(R.string.developer_name),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth(.8f)
                            )
                            Image(
                                painterResource(R.drawable.logo_ptm),
                                contentDescription = stringResource(R.string.developer_name),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp)
                                    .height(200.dp)
                            )
                        }
                    }
                    PtmFilledButton(
                        onClick = {
                            activity?.run {
                                startActivity(Intent(context, AuthorizationActivity::class.java))
                                finish()
                            }
                        }
                    ) {
                        Text(
                            stringResource(R.string.authorize),
                            fontSize = 18.sp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
                Text(
                    "${stringResource(R.string.version)}: ${
                        AppState.packageInfo?.versionName ?: stringResource(
                            R.string.unknown
                        )
                    }",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(8.dp)
                        .alpha(.5f)
                )
            }
        }
    }
}

@Composable
private fun NowYouPayUs() {
    PtmTheme {
        Scaffold { innerPadding ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text(
                    "${stringResource(R.string.now_you_pay_us)}\n:(",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}


@Preview
@Composable
private fun ContentPreview() = Content()

@Preview
@Composable
private fun NowYouPayUsPreview() = NowYouPayUs()