package space.diomentia.ehzdiagnosticstracker

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.data.AppPreferences
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.PasswordHash
import space.diomentia.ehzdiagnosticstracker.data.Profile
import space.diomentia.ehzdiagnosticstracker.data.ProfileDatabase
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.BorderedDialogContainer
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.SettingsOption
import space.diomentia.ehzdiagnosticstracker.ui.SettingsOptionsDivider
import space.diomentia.ehzdiagnosticstracker.ui.setupEdgeToEdge
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

class EditProfileActivity : ComponentActivity() {
    companion object {
        const val RESULT_PROFILE_DELETED = 2
    }

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
    val inspectionMode = LocalInspectionMode.current
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
                            PasswordHash.Companion.encrypt(password)
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
                    }
                )
            }

            var profilePasswordChangeRequest by remember { mutableStateOf<Profile?>(null) }
            profilePasswordChangeRequest?.let { profile ->
                ProfilePasswordChangeDialog(
                    profile,
                    onApply = { password ->
                        val hashword = PasswordHash.encrypt(password)
                        if (hashword == null) return@ProfilePasswordChangeDialog
                        ProfileDatabase.getInstance(context)
                            .profileDao()
                            .updateProfile(profile.copy(hashword = hashword.hash))
                        coroutineScope?.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(context.getString(R.string.profile_saved))
                        }
                    },
                    onDismissRequest = { profilePasswordChangeRequest = null }
                )
            }

            var profileDeleteRequest by remember { mutableStateOf<Profile?>(null) }
            profileDeleteRequest?.let { profile ->
                DeleteProfileDialog(profile) { profileDeleteRequest = null }
            }

            ScrollableColumn(
                innerPadding,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val profile = AppState.profileSession
                    ?: if (inspectionMode)
                        Profile("Name", "Secondname", "")
                    else null
                profile?.let { profile ->
                    Text(
                        "${profile.surname} ${profile.name}",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    var nameInput by remember { mutableStateOf(profile.name) }
                    var nameError by remember { mutableStateOf(false) }
                    var surnameInput by remember { mutableStateOf(profile.surname) }
                    var surnameError by remember { mutableStateOf(false) }
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            stringResource(R.string.name),
                            style = MaterialTheme.typography.titleSmall
                        )
                        TextField(
                            nameInput,
                            onValueChange = {
                                nameError = false
                                nameInput = it
                            },
                            placeholder = {
                                Text(
                                    stringResource(R.string.input_name),
                                    fontStyle = FontStyle.Italic,
                                    maxLines = 1,
                                    modifier = Modifier.alpha(.5f)
                                )
                            },
                            isError = nameError,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .padding(start = 16.dp)
                        )
                        Text(
                            stringResource(R.string.surname),
                            style = MaterialTheme.typography.titleSmall
                        )
                        TextField(
                            surnameInput,
                            onValueChange = {
                                surnameError = false
                                surnameInput = it
                            },
                            placeholder = {
                                Text(
                                    stringResource(R.string.input_surname),
                                    fontStyle = FontStyle.Italic,
                                    maxLines = 1,
                                    modifier = Modifier.alpha(.5f)
                                )
                            },
                            isError = surnameError,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .padding(start = 16.dp)
                        )
                        SettingsOption(
                            label = stringResource(R.string.change_password),
                            secondaryLabel = stringResource(R.string.change_password_description)
                        ) { profilePasswordChangeRequest = profile }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        PtmFilledButton(
                            onClick = {
                                val name = nameInput
                                if (Profile.checkName(name).also { nameError = it })
                                    return@PtmFilledButton
                                ProfileDatabase.getInstance(context)
                                    .profileDao()
                                    .updateProfile(profile.copy(name = name))
                                coroutineScope?.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(context.getString(R.string.profile_saved))
                                }
                            }
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        PtmFilledButton(
                            onClick = {
                                profileDeleteRequest = profile
                            }
                        ) {
                            Text(stringResource(R.string.delete_profile))
                        }
                    }
                }

                if (AppState.profileSession != null && AppState.adminSession) {
                    SettingsOptionsDivider()
                }

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
private fun ProfilePasswordChangeDialog(
    profile: Profile,
    onDismissRequest: () -> Unit,
    onApply: (String) -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                horizontalAlignment = Alignment.Companion.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "${stringResource(R.string.change_password)}:\n${profile.name}",
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
                            fontStyle = FontStyle.Companion.Italic,
                            maxLines = 1,
                            modifier = Modifier.alpha(.5f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Companion.Password),
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PtmFilledButton(
                        enabled = PasswordHash.checkInput(passwordInput),
                        onClick = {
                            onApply(passwordInput)
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.login))
                    }
                    PtmFilledButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.Companion.padding(start = 8.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
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
                horizontalAlignment = Alignment.Companion.Start,
                modifier = Modifier.Companion
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
                            fontStyle = FontStyle.Companion.Italic,
                            maxLines = 1,
                            modifier = Modifier.Companion.alpha(.5f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Companion.Password),
                    modifier = Modifier.Companion
                        .padding(vertical = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.Companion.fillMaxWidth()
                ) {
                    PtmFilledButton(
                        enabled = PasswordHash.Companion.checkInput(passwordInput),
                        onClick = {
                            onApply(passwordInput)
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.login))
                    }
                    PtmFilledButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.Companion.padding(start = 8.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteProfileDialog(
    profile: Profile,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    "${stringResource(R.string.delete_profile)}:\n${profile.name}?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                PtmFilledButton(
                    onClick = {
                        ProfileDatabase.getInstance(context)
                            .profileDao()
                            .deleteProfile(profile)
                        onDismissRequest()
                        activity?.run {
                            setResult(EditProfileActivity.RESULT_PROFILE_DELETED)
                            finish()
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
                PtmFilledButton(
                    onClick = onDismissRequest
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}


@Preview
@Composable
private fun ContentPreview() = Content()