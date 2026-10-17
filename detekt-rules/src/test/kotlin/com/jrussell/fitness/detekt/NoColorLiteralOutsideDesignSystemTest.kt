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
}
