package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.PasswordHash
import space.diomentia.ehzdiagnosticstracker.data.Profile
import space.diomentia.ehzdiagnosticstracker.data.ProfileDatabase
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.BorderedDialogContainer
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun AuthorizationScreen(
    onEditProfile: () -> Unit,
    onAuthorized: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    var adminPasswordRequest by remember { mutableStateOf(false) }
    val profiles by ProfileDatabase.Companion.getInstance()
        .profileDao()
        .getProfiles()
        .collectAsStateWithLifecycle(emptyList())

    PtmTheme {
        Scaffold(
            snackbarHost = { PtmSnackbarHost(snackbarHostState) },
            topBar = { PtmTopBar(title = { Text(stringResource(R.string.authorization)) }) },
            bottomBar = {
                PtmBottomBar(
                    actions = {
                        ActionButton(
                            painterResource(R.drawable.ic_manage_accounts),
                            label = "${stringResource(R.string.login_as)} ${stringResource(R.string.administrator)}",
                            tint = if (AppState.adminSession)
                                MaterialTheme.colorScheme.background
                            else
                                MaterialTheme.colorScheme.primary,
                            modifier = Modifier.Companion
                                .background(
                                    color = if (AppState.adminSession)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Companion.Transparent,
                                    shape = RectangleShape
                                )
                        ) { adminPasswordRequest = true }
                        if (AppState.adminSession == true) {
                            ActionButton(
                                rememberVectorPainter(Icons.Outlined.Edit),
                                label = stringResource(R.string.edit_profile)
                            ) {
                                onEditProfile()
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (adminPasswordRequest) AdminAuthorizeDialog { adminPasswordRequest = false }

            var addUserRequest by remember { mutableStateOf(false) }
            if (addUserRequest) {
                AddUserDialog { addUserRequest = false }
            }

            var authorizeRequest by remember { mutableStateOf<Profile?>(null) }
            authorizeRequest?.let { profile ->
                AuthorizeDialog(
                    profile,
                    onDismissRequest = { authorizeRequest = null },
                    onLogin = { password ->
                        val success = AppState.loginProfile(profile, password)
                        coroutineScope?.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                if (success)
                                    "${context.getString(R.string.auth_success)}!"
                                else
                                    "${context.getString(R.string.auth_fail)}..."
                            )
                        }
                        authorizeRequest = null
                        if (success) {
                            onAuthorized()
                        }
                    }
                )
            }

            FullscreenLazyColumn(
                innerPadding,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                modifier = Modifier.Companion.fillMaxSize()
            ) {
                items(profiles) { profile ->
                    ProfileItem(profile) {
                        if (AppState.adminSession) {
                            AppState.profileSession = profile
                        }
                        if (AppState.profileSession == profile) {
                            onAuthorized()
                        } else {
                            authorizeRequest = profile
                        }
                    }
                }
                item {
                    PtmFilledButton(
                        onClick = {
                            if (!AppState.adminSession) {
                                coroutineScope?.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(context.getString(R.string.authorize_as_admin))
                                }
                                return@PtmFilledButton
                            }
                            addUserRequest = true
                        },
                        modifier = Modifier.Companion.padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Companion.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.add_profile)
                            )
                            Text(
                                stringResource(R.string.add_profile),
                                modifier = Modifier.Companion.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorizeDialog(
    profile: Profile,
    onDismissRequest: () -> Unit,
    onLogin: (String) -> Unit
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
                    "${stringResource(R.string.login_as)} ${profile.surname}" +
                            if (profile.name.isNotBlank()) " ${profile.name[0]}." else "",
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Companion.Password,
                        imeAction = ImeAction.Companion.Done
                    ),
                    modifier = Modifier.Companion
                        .padding(vertical = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.Companion.fillMaxWidth()
                ) {
                    PtmFilledButton(
                        enabled = PasswordHash.Companion.checkInput(passwordInput),
                        onClick = { onLogin(passwordInput) }
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
private fun AdminAuthorizeDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current

    var adminPasswordInput by remember { mutableStateOf<String?>(null) }
    adminPasswordInput?.let {
        coroutineScope?.launch {
            val success = AppState.elevateToAdmin(context, password = it)
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                if (success)
                    context.getString(R.string.admin_authorized)
                else
                    "${context.getString(R.string.auth_fail)}..."
            )
        }
        adminPasswordInput = null
        onDismissRequest()
    }
    AuthorizeDialog(
        profile = Profile(
            surname = stringResource(R.string.administrator),
            name = "",
            hashword = ""
        ),
        onDismissRequest = onDismissRequest
    ) { adminPasswordInput = it }
}

@Composable
private fun AddUserDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    var nameInput by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var surnameInput by remember { mutableStateOf("") }
    var surnameError by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                horizontalAlignment = Alignment.Companion.Start,
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "${stringResource(R.string.name)}:",
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
                            fontStyle = FontStyle.Companion.Italic,
                            maxLines = 1,
                            modifier = Modifier.Companion.alpha(.5f)
                        )
                    },
                    isError = nameError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Companion.Next),
                    modifier = Modifier.Companion
                        .padding(vertical = 12.dp)
                        .padding(start = 16.dp)
                )
                Text(
                    "${stringResource(R.string.surname)}:",
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
                            fontStyle = FontStyle.Companion.Italic,
                            maxLines = 1,
                            modifier = Modifier.Companion.alpha(.5f)
                        )
                    },
                    isError = nameError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Companion.Next),
                    modifier = Modifier.Companion
                        .padding(vertical = 12.dp)
                        .padding(start = 16.dp)
                )
                Text(
                    "${stringResource(R.string.password)}:",
                    style = MaterialTheme.typography.titleSmall
                )
                TextField(
                    passwordInput,
                    onValueChange = {
                        passwordError = false
                        passwordInput = it.filter { !it.isWhitespace() }
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.input_password),
                            fontStyle = FontStyle.Companion.Italic,
                            maxLines = 1,
                            modifier = Modifier.Companion.alpha(.5f)
                        )
                    },
                    isError = passwordError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Companion.Password,
                        imeAction = ImeAction.Companion.Done
                    ),
                    modifier = Modifier.Companion
                        .padding(vertical = 12.dp)
                        .padding(start = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.Companion.fillMaxWidth()
                ) {
                    PtmFilledButton(
                        onClick = {
                            val name = nameInput
                            val surname = surnameInput
                            val hashword = PasswordHash.Companion.encrypt(passwordInput)?.hash
                            if (
                                (hashword == null).also { passwordError = it }
                                || (!Profile.Companion.checkName(name)).also { nameError = it }
                                || (!Profile.Companion.checkName(surname)).also {
                                    surnameError = it
                                }
                            ) {
                                coroutineScope?.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        when {
                                            nameError || surnameError -> context.getString(R.string.name_only_letters)
                                            hashword == null -> "${context.getString(R.string.password_symbols)}: ${PasswordHash.Companion.SPECIAL_SYMBOLS}"
                                            else -> context.getString(R.string.wrong_profile_input)
                                        }
                                    )
                                }
                                return@PtmFilledButton
                            }
                            ProfileDatabase.Companion.getInstance(context).profileDao()
                                .createProfile(
                                    Profile(
                                        name = name,
                                        surname = surname,
                                        hashword = hashword!!
                                    )
                                )
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.create))
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
private fun ProfileItem(
    profile: Profile,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.Companion.CenterVertically,
        modifier = Modifier.Companion
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp)
    ) {
        Icon(
            Icons.Outlined.Person,
            contentDescription = null,
            tint = if (AppState.profileSession == profile)
                MaterialTheme.colorScheme.background
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier.Companion
                .padding(12.dp)
                .background(
                    color = if (AppState.profileSession == profile)
                        MaterialTheme.colorScheme.primary
                    else Color.Companion.Transparent,
                    shape = CircleShape
                )
                .size(96.dp)
        )
        Text(
            "${profile.surname} ${profile.name}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.Companion.weight(1f)
        )
    }
}


@Preview
@Composable
private fun AuthorizationScreenPreview() = AuthorizationScreen({}, {})

@Preview
@Composable
private fun AuthorizeDialogPreview() =
    PtmTheme {
        AuthorizeDialog(Profile("Name", "Secondname", ""), {}) {}
    }

@Preview
@Composable
private fun AddUserDialogPreview() =
    PtmTheme { AddUserDialog {} }

@Preview(showBackground = true)
@Composable
private fun ProfileItemPreview() = PtmTheme { ProfileItem(Profile("Name", "Secondname", "")) {} }