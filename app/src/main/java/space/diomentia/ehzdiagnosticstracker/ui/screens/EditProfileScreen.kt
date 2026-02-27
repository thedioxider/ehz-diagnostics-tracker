package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppPreferences
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.PasswordHash
import space.diomentia.ehzdiagnosticstracker.data.Profile
import space.diomentia.ehzdiagnosticstracker.data.ProfileDatabase
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.DeleteDialog
import space.diomentia.ehzdiagnosticstracker.ui.DialogField
import space.diomentia.ehzdiagnosticstracker.ui.EditFieldsDialog
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.SettingsOption
import space.diomentia.ehzdiagnosticstracker.ui.SettingsOptionsDivider
import space.diomentia.ehzdiagnosticstracker.ui.rememberSnacker
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun EditProfileScreen(
    onBack: () -> Unit
) {
    val inspectionMode = LocalInspectionMode.current
    val context = LocalContext.current
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
                            onBack()
                        }
                    }
                )
            }
        ) { innerPadding ->
            var adminPasswordChangeRequest by remember { mutableStateOf(false) }
            var adminHashword by AppPreferences.rememberAdminHashword()
            if (adminPasswordChangeRequest) {
                PasswordChangeDialog(
                    profile = null,
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
                    },
                    onDismissRequest = { adminPasswordChangeRequest = false }
                )
            }

            var profilePasswordChangeRequest by remember { mutableStateOf<Profile?>(null) }
            profilePasswordChangeRequest?.let { profile ->
                PasswordChangeDialog(
                    profile,
                    onApply = { password ->
                        val hashword = PasswordHash.encrypt(password)
                        if (hashword == null) return@PasswordChangeDialog
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
                DeleteProfileDialog(profile, onDelete = onBack) { profileDeleteRequest = null }
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
                                nameInput = it.filter { c -> c.isLetter() }
                            },
                            placeholder = {
                                Text(
                                    stringResource(R.string.name),
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
                                surnameInput = it.filter { c -> c.isLetter() }
                            },
                            placeholder = {
                                Text(
                                    stringResource(R.string.surname),
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
private fun PasswordChangeDialog(
    profile: Profile?,
    onApply: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    EditFieldsDialog(
        fields = listOf(
            R.string.password to DialogField(
                name = stringResource(R.string.password),
                checker = { !PasswordHash.checkInput(it) },  // Negate: checker expects true=error
                supportingText = stringResource(R.string.password_symbols),
                keyboardType = KeyboardType.Password
            )
        ),
        onAction = { values ->
            val password = values[R.string.password]!!
            onApply(password)
            onDismissRequest()
        },
        actionLabel = { Text(stringResource(R.string.save)) },
        label = {
            Text(
                if (profile != null)
                    "${stringResource(R.string.change_password)}:\n${profile.name}"
                else
                    stringResource(R.string.change_admin_password)
            )
        },
        onCancel = onDismissRequest
    )
}

@Composable
private fun DeleteProfileDialog(
    profile: Profile,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val snacker = rememberSnacker()

    DeleteDialog(
        onDelete = {
            ProfileDatabase.getInstance(context)
                .profileDao()
                .deleteProfile(profile)
            snacker.showMessage(context.getString(R.string.profile_deleted))
            onDismissRequest()
            onDelete()
        },
        label = { Text("${stringResource(R.string.delete_profile)}?") },
        onCancel = onDismissRequest,
        name = "${profile.surname} ${profile.name}",
        details = ""
    )
}


@Preview
@Composable
private fun EditProfileScreenPreview() = EditProfileScreen {}

@Preview
@Composable
private fun PasswordChangeDialogPreview() = PtmTheme {
    EditProfileScreenPreview()
    PasswordChangeDialog(
        profile = Profile("Demetrius", "Ryabov", ""),
        onApply = {},
        onDismissRequest = {}
    )
}

@Preview
@Composable
private fun AdminPasswordChangeDialogPreview() = PtmTheme {
    EditProfileScreenPreview()
    PasswordChangeDialog(
        profile = null,
        onApply = {},
        onDismissRequest = {}
    )
}
