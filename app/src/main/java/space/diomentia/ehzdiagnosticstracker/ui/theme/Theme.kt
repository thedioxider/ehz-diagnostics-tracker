package space.diomentia.ehzdiagnosticstracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PtmColorScheme = darkColorScheme(
    primary = blue_aquamarine,
    onPrimary = blue_zodiac,
    secondary = green_haze,
    onSecondary = white,
    tertiary = blue_cerulean,
    onTertiary = blue_zodiac,
    background = blue_zodiac,
    onBackground = white,
    surface = green_haze,
    onSurface = white
)

@Composable
fun PtmTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PtmColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PtmTypography,
        content = content
    )
}