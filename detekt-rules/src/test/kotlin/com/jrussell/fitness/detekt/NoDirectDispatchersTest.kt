package com.jrussell.fitness.detekt

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class NoDirectDispatchersTest {
    private val rule: NoDirectDispatchers = NoDirectDispatchers(Config.empty)

    @Test
    fun dispatchersIo_inFeaturePackage_isReported() {
        val code: String = """
            package com.jrussell.fitness.feature.today

            import kotlinx.coroutines.CoroutineDispatcher
            import kotlinx.coroutines.Dispatchers

            val io: CoroutineDispatcher = Dispatchers.IO
        """.trimIndent()

        assertThat(rule.lint(code)).hasSize(1)
    }

    @Test
    fun dispatchersDefault_inDataPackage_isReported() {
        val code: String = """
            package com.jrussell.fitness.core.data

            import kotlinx.coroutines.CoroutineDispatcher
            import kotlinx.coroutines.Dispatchers

            val cpu: CoroutineDispatcher = Dispatchers.Default
        """.trimIndent()

        assertThat(rule.lint(code)).hasSize(1)
    }

    @Test
    fun fullyQualifiedDispatchersIo_isReported() {
        val code: String = """
            package com.jrussell.fitness.core.data

            val io: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO
        """.trimIndent()

        assertThat(rule.lint(code)).hasSize(1)
    }

    @Test
    fun dispatchers_inAppDiPackage_isAllowed() {
        val code: String = """
            package com.jrussell.fitness.di

            import kotlinx.coroutines.CoroutineDispatcher
            import kotlinx.coroutines.Dispatchers

            val io: CoroutineDispatcher = Dispatchers.IO
            val cpu: CoroutineDispatcher = Dispatchers.Default
        """.trimIndent()

        assertThat(rule.lint(code)).isEmpty()
    }

    @Test
    fun setMain_isAllowed() {
        val code: String = """
            package com.jrussell.fitness.core.testing

            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.test.StandardTestDispatcher
            import kotlinx.coroutines.test.resetMain
            import kotlinx.coroutines.test.setMain

            fun swap() {
                Dispatchers.setMain(StandardTestDispatcher())
                Dispatchers.resetMain()
            }
        """.trimIndent()

        assertThat(rule.lint(code)).isEmpty()
    }
}
