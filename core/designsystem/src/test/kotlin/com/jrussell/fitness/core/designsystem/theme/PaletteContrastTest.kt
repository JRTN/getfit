package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isGreaterThanOrEqualTo
import com.jrussell.fitness.core.model.ColorPalette
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * The spec's accessibility gate: every foreground/background token pair in all six palette
 * instances meets its WCAG contrast requirement, against both the page background and the
 * container color that nav bars, cards and sheets sit on.
 */
@RunWith(Parameterized::class)
class PaletteContrastTest(private val palette: ColorPalette, private val dark: Boolean) {
    private val scheme: ColorScheme = colorSchemeFor(palette, dark)
    private val data: DataColors = dataColorsFor(palette, dark)
    private val requirement: ContrastRequirement = contrastRequirementFor(palette)
    private val instance: String = "$palette/${if (dark) "dark" else "light"}"

    @Test
    fun textTokens_meetTextContrast() {
        val pairs: List<Triple<String, Color, Color>> = listOf(
            Triple("onBackground/background", scheme.onBackground, scheme.background),
            Triple("onBackground/surfaceContainer", scheme.onBackground, scheme.surfaceContainer),
            Triple("onSurface/background", scheme.onSurface, scheme.background),
            Triple("onSurface/surfaceContainer", scheme.onSurface, scheme.surfaceContainer),
            Triple("onSurfaceVariant/background", scheme.onSurfaceVariant, scheme.background),
            Triple("onSurfaceVariant/surfaceContainer", scheme.onSurfaceVariant, scheme.surfaceContainer),
            Triple("primary/background", scheme.primary, scheme.background),
            Triple("onPrimary/primary", scheme.onPrimary, scheme.primary),
            Triple("onSecondaryContainer/secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer),
            Triple("chartAxisLabel/background", data.chartAxisLabel, scheme.background),
            // Material draws error text in `error` (e.g. TextField supporting text) and badge or
            // button text in `onError` on `error`, so both are text pairs.
            Triple("onError/error", scheme.onError, scheme.error),
            Triple("error/background", scheme.error, scheme.background),
            Triple("error/surfaceContainer", scheme.error, scheme.surfaceContainer),
        )
        pairs.forEach { (name: String, foreground: Color, background: Color) ->
            assertThat(contrastRatio(foreground, background), "$instance $name")
                .isGreaterThanOrEqualTo(requirement.text)
        }
    }

    @Test
    fun graphicTokens_meetGraphicContrast() {
        val graphics: List<Pair<String, Color>> =
            data.seriesPalette.mapIndexed { index: Int, color: Color -> "series$index" to color } +
                listOf(
                    "macroProtein" to data.macroProtein,
                    "macroCarbohydrate" to data.macroCarbohydrate,
                    "macroFat" to data.macroFat,
                    "targetMet" to data.targetMet,
                    "targetUnder" to data.targetUnder,
                    "targetOver" to data.targetOver,
                    "deltaPositive" to data.deltaPositive,
                    "deltaNegative" to data.deltaNegative,
                    "projectionLine" to data.projectionLine,
                    "actualLine" to data.actualLine,
                    "outline" to scheme.outline,
                )
        val surfaces: List<Pair<String, Color>> =
            listOf("background" to scheme.background, "surfaceContainer" to scheme.surfaceContainer)
        graphics.forEach { (name: String, color: Color) ->
            surfaces.forEach { (surfaceName: String, surface: Color) ->
                assertThat(contrastRatio(color, surface), "$instance $name/$surfaceName")
                    .isGreaterThanOrEqualTo(requirement.graphic)
            }
        }
    }

    @Test
    fun seriesPalette_hasSixDistinctColors() {
        assertThat(data.seriesPalette, instance).hasSize(SERIES_COUNT)
        assertThat(data.seriesPalette.toSet(), instance).hasSize(SERIES_COUNT)
    }

    companion object {
        private const val SERIES_COUNT: Int = 6

        @JvmStatic
        @Parameterized.Parameters(name = "{0} dark={1}")
        fun instances(): List<Array<Any>> {
            return ColorPalette.entries.flatMap { palette: ColorPalette ->
                listOf(arrayOf<Any>(palette, false), arrayOf<Any>(palette, true))
            }
        }
    }
}
