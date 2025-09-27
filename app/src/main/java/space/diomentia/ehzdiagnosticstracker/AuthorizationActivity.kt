package space.diomentia.ehzdiagnosticstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
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
import space.diomentia.ehzdiagnosticstracker.data.AppPreferences
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.EditProfileActivity
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.PasswordHash
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.BorderedDialogContainer
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

class AuthorizationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)
        setContent {
            CompositionLocalProvider(
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
    val snackbarHostState = LocalSnackbarHostState.current
    var adminPasswordRequest by remember { mutableStateOf(false) }

    PtmTheme {
        Scaffold(
            snackbarHost = { PtmSnackbarHost(snackbarHostState) },
            topBar = { PtmTopBar(title = { Text(stringResource(R.string.authorization)) }) },
            bottomBar = {
                PtmBottomBar(
                    actions = {
                        ActionButton(
                            painterResource(R.drawable.ic_manage_accounts),
                            label = "${stringResource(R.string.login_as)} ${stringResource(R.string.administrator)}"
                        ) { adminPasswordRequest = true }
                        if (AppState.adminSession == true) {
                            ActionButton(
                                rememberVectorPainter(Icons.Outlined.Edit),
                                label = stringResource(R.string.edit_profile)
                            ) {
                                context.startActivity(
                                    Intent(
                                        context,
                                        EditProfileActivity::class.java
                                    )
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                val adminHashword by AppPreferences.rememberAdminHashword()
                Text(adminHashword.hash)
            }
            AdminAuthorizeDialog(adminPasswordRequest) { adminPasswordRequest = false }
        }
    }
}

@Composable
private fun AuthorizeDialog(
    name: String,
    onDismissRequest: () -> Unit,
    onLogin: (String) -> Unit
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
                    "${stringResource(R.string.login_as)} $name",
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
                        onClick = { onLogin(passwordInput) }
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

@Composable
private fun AdminAuthorizeDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current

    var adminPasswordInput by remember { mutableStateOf<String?>(null) }
    adminPasswordInput?.let {
        coroutineScope.launch {
            val success = AppState.elevateToAdmin(context, password = it)
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                if (success)
                    "${context.getString(R.string.auth_success)}!"
                else
                    "${context.getString(R.string.auth_fail)}..."
            )
        }
        adminPasswordInput = null
        onDismissRequest()
    }
    if (showDialog) {
        AuthorizeDialog(
            name = stringResource(R.string.administrator),
            onDismissRequest = onDismissRequest
        ) { adminPasswordInput = it }
    }
}


@Preview
@Composable
private fun ContentPreview() = Content()

@Preview
@Composable
private fun AuthorizeDialogPreview() =
    PtmTheme { AuthorizeDialog(stringResource(R.string.administrator), {}) {} }