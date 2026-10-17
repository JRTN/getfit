package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import org.junit.Test

class TypographyAndShapeTest {
    @Test
    fun everyTypographyStyle_usesInterWithTabularFigures() {
        allStyles(GetFitTypography).forEach { (name: String, style: TextStyle) ->
            assertThat(style.fontFamily, name).isEqualTo(InterFontFamily)
            assertThat(style.fontFeatureSettings, name).isEqualTo("tnum")
        }
    }

    @Test
    fun everyShapeCorner_isBetween2And4Dp() {
        val shapes: List<Pair<String, CornerBasedShape>> = listOf(
            "extraSmall" to GetFitShapes.extraSmall,
            "small" to GetFitShapes.small,
            "medium" to GetFitShapes.medium,
            "large" to GetFitShapes.large,
            "extraLarge" to GetFitShapes.extraLarge,
        )
        // At density 1, one dp is one pixel, so corner sizes read directly in dp.
        val density: Density = Density(1f)
        val size: Size = Size(width = 1_000f, height = 1_000f)
        shapes.forEach { (name: String, shape: CornerBasedShape) ->
            val corners: List<CornerSize> = listOf(shape.topStart, shape.topEnd, shape.bottomEnd, shape.bottomStart)
            corners.forEach { corner: CornerSize ->
                assertThat(corner.toPx(size, density), name).isBetween(2f, 4f)
            }
        }
    }

    private fun allStyles(typography: Typography): List<Pair<String, TextStyle>> {
        return listOf(
            "displayLarge" to typography.displayLarge,
            "displayMedium" to typography.displayMedium,
            "displaySmall" to typography.displaySmall,
            "headlineLarge" to typography.headlineLarge,
            "headlineMedium" to typography.headlineMedium,
            "headlineSmall" to typography.headlineSmall,
            "titleLarge" to typography.titleLarge,
            "titleMedium" to typography.titleMedium,
            "titleSmall" to typography.titleSmall,
            "bodyLarge" to typography.bodyLarge,
            "bodyMedium" to typography.bodyMedium,
            "bodySmall" to typography.bodySmall,
            "labelLarge" to typography.labelLarge,
            "labelMedium" to typography.labelMedium,
            "labelSmall" to typography.labelSmall,
        )
    }
}
