package space.diomentia.ehzdiagnosticstracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import space.diomentia.ehzdiagnosticstracker.ui.DialogField
import space.diomentia.ehzdiagnosticstracker.ui.EditFieldsDialog
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButtonColors
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.rememberSnacker
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
    val profiles by ProfileDatabase.getInstance()
        .profileDao()
        .getProfiles()
        .collectAsStateWithLifecycle(emptyList())

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
                        modifier = Modifier
                            .background(
                                color = if (AppState.adminSession)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Transparent,
                                shape = RectangleShape
                            )
                    ) { adminPasswordRequest = true }
                    if (AppState.adminSession) {
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
        if (adminPasswordRequest) AuthorizeDialog(null) { adminPasswordRequest = false }

        var addUserRequest by remember { mutableStateOf(false) }
        if (addUserRequest) {
            AddProfileDialog { addUserRequest = false }
        }

        var authorizeRequest by remember { mutableStateOf<Profile?>(null) }
        authorizeRequest?.let {
            AuthorizeDialog(authorizeRequest, onAuthorized) { authorizeRequest = null }
        }

        FullscreenLazyColumn(
            innerPadding,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
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
                    enabled = true,
                    colors = PtmFilledButtonColors.let {
                        if (AppState.adminSession) it
                        else it.copy(
                            containerColor = it.disabledContainerColor,
                            contentColor = it.disabledContentColor
                        )
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.add_profile)
                        )
                        Text(
                            stringResource(R.string.add_profile),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorizeDialog(
    profile: Profile?,
    onAuthorized: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snacker = rememberSnacker()

    EditFieldsDialog(
        fields = listOf(
            R.string.password to DialogField(
                stringResource(R.string.password),
                keyboardType = KeyboardType.Password
            )
        ),
        onAction = { v ->
            val password = v[R.string.password]!!
            if (profile != null) {
                val success = AppState.loginProfile(profile, password)
                snacker.showMessage(
                    context.getString(
                        if (success) R.string.auth_success else R.string.auth_fail
                    )
                )
                if (success) onAuthorized()
            } else {
                coroutineScope?.launch {
                    val success = AppState.elevateToAdmin(context, password = password)
                    snacker.showMessage(
                        context.getString(
                            if (success) R.string.admin_authorized else R.string.auth_fail
                        )
                    )
                }
            }
            onDismissRequest()
        },
        actionLabel = { Text(stringResource(R.string.login)) },
        onCancel = onDismissRequest,
        label = {
            Text(
                "${stringResource(R.string.login_as)} " +
                        (profile?.abbreviated ?: stringResource(R.string.administrator))
            )
        }
    )
}

@Composable
private fun AddProfileDialog(onDismissRequest: () -> Unit) {
    val context = LocalContext.current

    EditFieldsDialog(
        fields = listOf(
            R.string.name to DialogField(
                stringResource(R.string.name),
                checker = { Profile.checkName(it) },
                supportingText = stringResource(R.string.only_letters)
            ),
            R.string.surname to DialogField(
                stringResource(R.string.surname),
                checker = { Profile.checkSurname(it) },
                supportingText = stringResource(R.string.only_letters)
            ),
            R.string.password to DialogField(
                stringResource(R.string.password),
                checker = { PasswordHash.checkInput(it) },
                supportingText = stringResource(R.string.password_symbols),
                keyboardType = KeyboardType.Password
            )
        ),
        onAction = { v ->
            ProfileDatabase.getInstance(context).profileDao()
                .createProfile(
                    Profile(
                        name = v[R.string.name]!!,
                        surname = v[R.string.surname]!!,
                        hashword = PasswordHash.encrypt(v[R.string.password]!!)?.hash
                            ?: return@EditFieldsDialog
                    )
                )
            onDismissRequest()
        },
        actionLabel = { Text(stringResource(R.string.create)) },
        label = { Text(stringResource(R.string.create_profile)) },
        onCancel = onDismissRequest
    )
}

@Composable
private fun ProfileItem(
    profile: Profile,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
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
            modifier = Modifier
                .padding(12.dp)
                .background(
                    color = if (AppState.profileSession == profile)
                        MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    shape = CircleShape
                )
                .size(96.dp)
        )
        Text(
            "${profile.surname} ${profile.name}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
    }
}


@Preview
@Composable
private fun AuthorizationScreenPreview() = PtmTheme { AuthorizationScreen({}, {}) }

@Preview
@Composable
private fun AuthorizeDialogPreview() = PtmTheme {
    AuthorizationScreenPreview()
    AuthorizeDialog(Profile("Name", "Secondname", ""), {}) {}
}

@Preview
@Composable
private fun UserDialogPreview() = PtmTheme {
    AuthorizationScreenPreview()
    AddProfileDialog {}
}

@Preview(showBackground = true)
@Composable
private fun ProfileItemPreview() = PtmTheme { ProfileItem(Profile("Name", "Secondname", "")) {} }