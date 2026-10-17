package com.jrussell.fitness.detekt

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Test

class NoDestructiveMigrationTest {
    private val rule: NoDestructiveMigration = NoDestructiveMigration(Config.empty)

    @Test
    fun fallbackToDestructiveMigration_isReported() {
        val code: String = """
            package com.jrussell.fitness.core.database

            fun configure(builder: Builder) {
                builder.fallbackToDestructiveMigration(dropAllTables = true)
            }
        """.trimIndent()

        assertThat(rule.lint(code)).hasSize(1)
    }

    @Test
    fun onDowngradeVariant_isReported() {
        val code: String = """
            package com.jrussell.fitness.core.database

            fun configure(builder: Builder) {
                builder.fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            }
        """.trimIndent()

        assertThat(rule.lint(code)).hasSize(1)
    }

    @Test
    fun addMigrations_isAllowed() {
        val code: String = """
            package com.jrussell.fitness.core.database

            fun configure(builder: Builder) {
                builder.addMigrations(MIGRATION_1_2)
            }
        """.trimIndent()

        assertThat(rule.lint(code)).isEmpty()
    }
}
