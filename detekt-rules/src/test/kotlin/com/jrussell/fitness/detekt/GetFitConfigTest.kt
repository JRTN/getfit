package com.jrussell.fitness.detekt

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import java.io.File
import org.junit.jupiter.api.Test

/**
 * detekt 2.0.0-alpha.6 does not validate a plugin rule set's config section, so a misspelled rule
 * name in `config/detekt/getfit.yml` would silently leave that rule switched off. This test is
 * that validation: the file must name exactly the rules [GetFitRuleSetProvider] registers, and
 * activate each one.
 */
class GetFitConfigTest {
    private val lines: List<String> = File("../config/detekt/getfit.yml").readLines()

    @Test
    fun getfitYml_namesExactlyTheProvidedRules() {
        val configured: Set<String> = lines
            .mapNotNull { line: String -> RULE_LINE.matchEntire(line)?.groupValues?.get(1) }
            .toSet()
        val provided: Set<String> = GetFitRuleSetProvider().instance().rules.keys.map { name -> name.value }.toSet()

        assertThat(configured).isEqualTo(provided)
    }

    @Test
    fun getfitYml_activatesEveryRule() {
        lines.forEachIndexed { index: Int, line: String ->
            if (RULE_LINE.matches(line)) {
                assertThat(lines.getOrNull(index + 1) == "    active: true", "${line.trim()} is active").isTrue()
            }
        }
    }

    private companion object {
        val RULE_LINE: Regex = Regex("""^  (\w+):$""")
    }
}
