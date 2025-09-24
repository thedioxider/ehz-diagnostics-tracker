package space.diomentia.ehzdiagnosticstracker

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.OrganizationInfo
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.RequestConfigScreen
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

class CustomerInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        val snackbarHostState = SnackbarHostState()
        setContent {
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState
            ) {
                Content()
            }
        }
    }
}

@Composable
private fun Content() {
    val activity = LocalActivity.current
    val snackbarHostState = LocalSnackbarHostState.current

    PtmTheme {
        Scaffold(
            topBar = { PtmTopBar { Text(stringResource(R.string.customer)) } },
            bottomBar = {
                PtmBottomBar(navigation = {
                    ActionButton(rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack)) {
                        activity?.run {
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                    }
                })
            },
            snackbarHost = { PtmSnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            val customerInfo = AppState.customerInfo.value

            if (customerInfo == null) {
                val customerInfoChooser =
                    AppState.localYamlConfigCopyLauncher<OrganizationInfo>(AppState.CUSTOMER_INFO_FILENAME) {
                        AppState.customerInfo.value = it
                    }
                RequestConfigScreen(
                    label = stringResource(R.string.customer_info_description),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    customerInfoChooser.launch(arrayOf("application/*"))
                }
                return@Scaffold
            }

            @Composable
            fun Item(label: String, info: String) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        info,
                        modifier = Modifier.padding(start = 16.dp, top = 6.dp)
                    )
                }
            }

            ScrollableColumn(
                innerPadding,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Item(stringResource(R.string.organization_full_name), customerInfo.organizationName)
                Item(stringResource(R.string.organization_head), customerInfo.head)
                Item(stringResource(R.string.legal_address), customerInfo.legalAddress)
                Item(stringResource(R.string.location), customerInfo.location)
                Item(stringResource(R.string.phone), customerInfo.phone)
                Item(stringResource(R.string.email), customerInfo.email)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Preview
@Composable
private fun ContentPreview() {
    AppState.customerInfo.value = OrganizationInfo(
        organizationName = "SomeName",
        head = "Person X.X.",
        legalAddress = "N city, Some st., building 5",
        location = "some location",
        phone = "+7(777)777-77-77",
        email = "email@company.org"
    )

    Content()
}