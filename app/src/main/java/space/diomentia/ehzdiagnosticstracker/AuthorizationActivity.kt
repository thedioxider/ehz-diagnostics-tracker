package space.diomentia.ehzdiagnosticstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.PasswordHash
import space.diomentia.ehzdiagnosticstracker.data.Profile
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.BorderedDialogContainer
import space.diomentia.ehzdiagnosticstracker.ui.FullscreenLazyColumn
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
                LocalCoroutineScope provides rememberCoroutineScope(),
                LocalSnackbarHostState provides SnackbarHostState()
            ) {
                Content()
            }
        }
    }
}

@Composable
private fun Content(
    profiles: MutableState<List<Profile>> = mutableStateOf(emptyList())
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    var adminPasswordRequest by remember { mutableStateOf(false) }
    val profiles by AppState.profileDb
        ?.profileDao()
        ?.getProfiles()
        ?.collectAsStateWithLifecycle(emptyList()) ?: profiles

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
                            val editorLauncher = rememberLauncherForActivityResult(
                                ActivityResultContracts.StartActivityForResult()
                            ) { result ->
                                if (result.resultCode == EditProfileActivity.RESULT_PROFILE_DELETED) {
                                    coroutineScope?.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(context.getString(R.string.profile_deleted))
                                    }
                                }
                            }
                            ActionButton(
                                rememberVectorPainter(Icons.Outlined.Edit),
                                label = stringResource(R.string.edit_profile)
                            ) {
                                editorLauncher.launch(
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
            if (adminPasswordRequest) AdminAuthorizeDialog { adminPasswordRequest = false }

            var addUserRequest by remember { mutableStateOf(false) }
            if (addUserRequest) {
                AddUserDialog { addUserRequest = false }
            }

            var authorizeRequest by remember { mutableStateOf<Profile?>(null) }
            authorizeRequest?.let { profile ->
                AuthorizeDialog(
                    profile.name,
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
                        activity?.run {
                            startActivity(
                                Intent(
                                    context,
                                    DiagnosticObjectChooseActivity::class.java
                                )
                            )
                            finish()
                        }
                    }
                )
            }

            FullscreenLazyColumn(
                innerPadding,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                items(profiles) { profile ->
                    ProfileItem(profile) {
                        if (AppState.profileSession != profile) {
                            authorizeRequest = profile
                        } else {
                            activity?.run {
                                startActivity(
                                    Intent(
                                        context,
                                        DiagnosticObjectChooseActivity::class.java
                                    )
                                )
                                finish()
                            }
                        }
                    }
                }
                item {
                    AnimatedVisibility(AppState.adminSession) {
                        PtmFilledButton(
                            onClick = { addUserRequest = true },
                            modifier = Modifier.padding(8.dp)
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
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
                    "${context.getString(R.string.auth_success)}!"
                else
                    "${context.getString(R.string.auth_fail)}..."
            )
        }
        adminPasswordInput = null
        onDismissRequest()
    }
    AuthorizeDialog(
        name = stringResource(R.string.administrator),
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
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
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
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.alpha(.5f)
                        )
                    },
                    isError = nameError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
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
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.alpha(.5f)
                        )
                    },
                    isError = passwordError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .padding(start = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PtmFilledButton(
                        onClick = {
                            val name = nameInput
                            val hashword = PasswordHash.encrypt(passwordInput)?.hash
                            if (
                                (hashword == null).also { passwordError = it }
                                || (!Profile.checkName(name)).also { nameError = it }
                            ) {
                                coroutineScope?.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(context.getString(R.string.wrong_profile_input))
                                }
                                return@PtmFilledButton
                            }
                            AppState.profileDb?.profileDao()?.run {
                                createProfile(
                                    Profile(
                                        name = name,
                                        hashword = hashword!!
                                    )
                                )
                            }
                            onDismissRequest()
                        }
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
                    else MaterialTheme.colorScheme.background,
                    shape = CircleShape
                )
                .size(96.dp)
        )
        Text(
            profile.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
    }
}


@Preview
@Composable
private fun ContentPreview() = Content(remember {
    mutableStateOf(
        listOf(
            Profile("Name Secondname", "")
        )
    )
})

@Preview
@Composable
private fun AuthorizeDialogPreview() =
    PtmTheme { AuthorizeDialog(stringResource(R.string.administrator), {}) {} }

@Preview(showBackground = true)
@Composable
private fun ProfileItemPreview() = PtmTheme { ProfileItem(Profile("Name Secondname", "")) {} }