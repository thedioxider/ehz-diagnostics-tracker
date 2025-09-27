package space.diomentia.ehzdiagnosticstracker.data

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.SettingsOption
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.BorderedDialogContainer
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        setContent {
            CompositionLocalProvider(
                LocalCoroutineScope provides rememberCoroutineScope(),
                LocalSnackbarHostState provides SnackbarHostState()
            ) {
                Content()
            }
        }
    }
}

@Composable
private fun Content() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current

    PtmTheme {
        Scaffold(
            snackbarHost = { PtmSnackbarHost(snackbarHostState) },
            topBar = { PtmTopBar(title = { Text(stringResource(R.string.edit_profile)) }) },
            bottomBar = {
                PtmBottomBar(
                    navigation = {
                        ActionButton(
                            rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                            label = stringResource(R.string.back)
                        ) {
                            activity?.run {
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            var adminPasswordChangeRequest by remember { mutableStateOf(false) }
            var adminHashword by AppPreferences.rememberAdminHashword()
            if (adminPasswordChangeRequest) {
                AdminPasswordChangeDialog(
                    onDismissRequest = { adminPasswordChangeRequest = false },
                    onApply = { password ->
                        val success =
                            PasswordHash.encrypt(password)
                                ?.let { adminHashword = it } != null
                        coroutineScope?.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                if (success)
                                    "${context.getString(R.string.change_password_success)}!"
                                else
                                    "${context.getString(R.string.change_password_fail)}..."
                            )
                        }
                        adminPasswordChangeRequest = false
                    }
                )
            }
            ScrollableColumn(innerPadding) {
                if (AppState.adminSession) {
                    SettingsOption(
                        label = stringResource(R.string.change_admin_password),
                        icon = painterResource(R.drawable.ic_manage_accounts)
                    ) { adminPasswordChangeRequest = true }
                }
            }
        }
    }
}

@Composable
private fun AdminPasswordChangeDialog(
    onDismissRequest: () -> Unit,
    onApply: (String) -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.change_admin_password),
                    style = MaterialTheme.typography.titleMedium
                )
                TextField(
                    passwordInput,
                    onValueChange = { input ->
                        passwordInput = input.filter { !it.isWhitespace() }
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.input_password),
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.alpha(.5f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PtmFilledButton(
                        enabled = PasswordHash.checkInput(passwordInput),
                        onClick = { onApply(passwordInput) }
                    ) {
                        Text(stringResource(R.string.login))
                    }
                    PtmFilledButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}


@Preview
@Composable
private fun ContentPreview() = Content()