package com.jrussell.fitness.detekt

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.api.Finding
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class NoColorLiteralOutsideDesignSystemTest {
    private val rule: NoColorLiteralOutsideDesignSystem = NoColorLiteralOutsideDesignSystem(Config.empty)

    @Test
    fun colorCall_inFeaturePackage_isReported() {
        val code: String = """
            package com.jrussell.fitness.feature.today

            import androidx.compose.ui.graphics.Color

            val warning: Color = Color(0xFFB42318)
        """.trimIndent()

        val findings: List<Finding> = rule.lint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun colorCall_inDesignSystemPackage_isAllowed() {
        val code: String = """
            package com.jrussell.fitness.core.designsystem.theme

            import androidx.compose.ui.graphics.Color

            internal val Accent: Color = Color(0xFF6941C6)
        """.trimIndent()

        assertThat(rule.lint(code)).isEmpty()
    }

    @Test
    fun colorConstantsAndThemeReads_areAllowed() {
        val code: String = """
            package com.jrussell.fitness.feature.today

            import androidx.compose.ui.graphics.Color

            val none: Color = Color.Unspecified
            val clear: Color = Color.Transparent
        """.trimIndent()

        assertThat(rule.lint(code)).isEmpty()
    }

    @Test
    fun androidGraphicsColorConstant_isAllowed() {
        val code: String = """
            package com.jrussell.fitness

            val transparent: Int = android.graphics.Color.TRANSPARENT
        """.trimIndent()

        assertThat(rule.lint(code)).isEmpty()
    }

    @Test
    fun importedAndroidColorTransparent_isAllowed() {
        val code: String = """
            package com.jrussell.fitness

            import android.graphics.Color

            val transparent: Int = Color.TRANSPARENT
        """.trimIndent()

        assertThat(rule.lint(code)).isEmpty()
    }

    @Test
    fun namedColorConstant_isReported() {
        val code: String = """
            package com.jrussell.fitness.feature.today

            import androidx.compose.ui.graphics.Color

            val warning: Color = Color.Red
        """.trimIndent()

        assertThat(rule.lint(code)).hasSize(1)
    }

    @Test
    fun hslCall_isReported() {
        val code: String = """
            package com.jrussell.fitness.feature.today

            import androidx.compose.ui.graphics.Color

            val accent: Color = Color.hsl(270f, 0.5f, 0.5f)
        """.trimIndent()

        assertThat(rule.lint(code)).hasSize(1)
    }

    @Test
    fun androidParseColor_isReported() {
        val code: String = """
            package com.jrussell.fitness.feature.today

            val accent: Int = android.graphics.Color.parseColor("#6941C6")
        """.trimIndent()

        assertThat(rule.lint(code)).hasSize(1)
    }

    @Test
    fun importAlias_isReported() {
        val code: String = """
            package com.jrussell.fitness.feature.today

            import androidx.compose.ui.graphics.Color as Tint

            val accent: Tint = Tint.Unspecified
        """.trimIndent()

        assertThat(rule.lint(code)).hasSize(1)
    }
}
