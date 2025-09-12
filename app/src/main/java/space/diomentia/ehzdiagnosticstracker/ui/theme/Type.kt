package space.diomentia.ehzdiagnosticstracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import space.diomentia.ehzdiagnosticstracker.R

@OptIn(ExperimentalTextApi::class)
val MontserratFF = FontFamily(
    Font(
        R.font.montserrat,
        weight = FontWeight.W100,
        variationSettings = FontVariation.Settings(FontVariation.weight(100))
    ),
    Font(
        R.font.montserrat,
        weight = FontWeight.W200,
        variationSettings = FontVariation.Settings(FontVariation.weight(200))
    ),
    Font(
        R.font.montserrat,
        weight = FontWeight.W300,
        variationSettings = FontVariation.Settings(FontVariation.weight(300))
    ),
    Font(
        R.font.montserrat,
        weight = FontWeight.W400,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.montserrat,
        weight = FontWeight.W500,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.montserrat,
        weight = FontWeight.W600,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.montserrat,
        weight = FontWeight.W700,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        R.font.montserrat,
        weight = FontWeight.W800,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    ),
    Font(
        R.font.montserrat,
        weight = FontWeight.W900,
        variationSettings = FontVariation.Settings(FontVariation.weight(900))
    )
)

@OptIn(ExperimentalTextApi::class)
val MontserratItalicFF = FontFamily(
    Font(
        R.font.montserrat_italic,
        weight = FontWeight.W100,
        variationSettings = FontVariation.Settings(FontVariation.weight(100))
    ),
    Font(
        R.font.montserrat_italic,
        weight = FontWeight.W200,
        variationSettings = FontVariation.Settings(FontVariation.weight(200))
    ),
    Font(
        R.font.montserrat_italic,
        weight = FontWeight.W300,
        variationSettings = FontVariation.Settings(FontVariation.weight(300))
    ),
    Font(
        R.font.montserrat_italic,
        weight = FontWeight.W400,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.montserrat_italic,
        weight = FontWeight.W500,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.montserrat_italic,
        weight = FontWeight.W600,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.montserrat_italic,
        weight = FontWeight.W700,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        R.font.montserrat_italic,
        weight = FontWeight.W800,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    ),
    Font(
        R.font.montserrat_italic,
        weight = FontWeight.W900,
        variationSettings = FontVariation.Settings(FontVariation.weight(900))
    )
)

val defaultTypo = Typography()
val PtmTypography = Typography(
    bodyLarge = defaultTypo.bodyLarge.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = defaultTypo.bodyMedium.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = defaultTypo.bodySmall.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.Thin
    ),
    titleLarge = defaultTypo.titleLarge.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = defaultTypo.titleMedium.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.Bold,
    ),
    titleSmall = defaultTypo.titleSmall.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.SemiBold
    ),
    headlineLarge = defaultTypo.headlineLarge.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.ExtraBold
    ),
    headlineMedium = defaultTypo.headlineMedium.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.ExtraBold,
    ),
    headlineSmall = defaultTypo.headlineSmall.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.Bold,
    ),
    labelLarge = defaultTypo.labelLarge.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.Bold,
    ),
    labelMedium = defaultTypo.labelMedium.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.Bold,
    ),
    labelSmall = defaultTypo.labelSmall.copy(
        fontFamily = MontserratFF,
        fontWeight = FontWeight.SemiBold,
    ),
    displayLarge = defaultTypo.displayLarge.copy(
        fontFamily = MontserratFF
    ),
    displayMedium = defaultTypo.displayMedium.copy(
        fontFamily = MontserratFF
    ),
    displaySmall = defaultTypo.displaySmall.copy(
        fontFamily = MontserratFF
    )
)