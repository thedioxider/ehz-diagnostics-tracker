package space.diomentia.ehzdiagnosticstracker.data

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }