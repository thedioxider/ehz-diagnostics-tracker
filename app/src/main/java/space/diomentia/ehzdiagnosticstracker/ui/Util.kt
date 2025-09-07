package space.diomentia.ehzdiagnosticstracker.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import space.diomentia.ehzdiagnosticstracker.ui.theme.blue_zodiac

fun setupEdgeToEdge(activity: ComponentActivity) {
    activity.enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(blue_zodiac.copy(alpha = .25f).toArgb()),
        navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
    )
}

@Composable
fun PtmSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(snackbarHostState) {
        val interactionSource = remember { MutableInteractionSource() }
        Snackbar(
            it,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        )
    }
}

@Composable
fun getWindowSize(): Size {
    return LocalConfiguration.current.let {
        Size(
            with(LocalDensity.current) { it.screenWidthDp.dp.toPx() },
            with(LocalDensity.current) { it.screenHeightDp.dp.toPx() }
        )
    }
}