package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jrussell.fitness.core.designsystem.R

/** Inter 4.1, bundled (SIL Open Font License; text in assets/licenses/inter-OFL.txt). */
val InterFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** OpenType feature that gives every digit the same width, so numbers line up in columns. */
private const val TABULAR_FIGURES: String = "tnum"

private val MaterialDefaults: Typography = Typography()

/**
 * GetFit's type scale: Material 3's roles in Inter with tabular figures everywhere, tightened
 * for a dense, numeric UI.
 */
val GetFitTypography: Typography = Typography(
    displayLarge = MaterialDefaults.displayLarge.inInter(),
    displayMedium = MaterialDefaults.displayMedium.inInter(),
    displaySmall = MaterialDefaults.displaySmall.inInter(),
    headlineLarge = MaterialDefaults.headlineLarge.inInter(),
    headlineMedium = MaterialDefaults.headlineMedium.inInter(),
    headlineSmall = MaterialDefaults.headlineSmall.inInter(),
    titleLarge = MaterialDefaults.titleLarge.inInter(size = 20, lineHeight = 26, weight = FontWeight.SemiBold),
    titleMedium = MaterialDefaults.titleMedium.inInter(size = 16, lineHeight = 22, weight = FontWeight.SemiBold),
    titleSmall = MaterialDefaults.titleSmall.inInter(size = 14, lineHeight = 20, weight = FontWeight.Medium),
    bodyLarge = MaterialDefaults.bodyLarge.inInter(size = 15, lineHeight = 22, weight = FontWeight.Normal),
    bodyMedium = MaterialDefaults.bodyMedium.inInter(size = 14, lineHeight = 20, weight = FontWeight.Normal),
    bodySmall = MaterialDefaults.bodySmall.inInter(size = 12, lineHeight = 16, weight = FontWeight.Normal),
    labelLarge = MaterialDefaults.labelLarge.inInter(size = 14, lineHeight = 20, weight = FontWeight.Medium),
    labelMedium = MaterialDefaults.labelMedium.inInter(size = 12, lineHeight = 16, weight = FontWeight.Medium),
    labelSmall = MaterialDefaults.labelSmall.inInter(size = 11, lineHeight = 16, weight = FontWeight.Medium),
)

private fun TextStyle.inInter(): TextStyle {
    return copy(fontFamily = InterFontFamily, fontFeatureSettings = TABULAR_FIGURES)
}

private fun TextStyle.inInter(size: Int, lineHeight: Int, weight: FontWeight): TextStyle {
    return inInter().copy(fontSize = size.sp, lineHeight = lineHeight.sp, fontWeight = weight)
}
