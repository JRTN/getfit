package com.jrussell.fitness.detekt

import dev.detekt.api.Config
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * GetFit's own detekt rules, rule set id `getfit`. detekt discovers this class through
 * `META-INF/services/dev.detekt.api.RuleSetProvider`; rules are switched on in
 * `config/detekt/detekt.yml`. M1b adds the dispatcher and destructive-migration rules.
 */
class GetFitRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId(RULE_SET_ID)

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            mapOf(
                RuleName("NoColorLiteralOutsideDesignSystem") to { config: Config ->
                    NoColorLiteralOutsideDesignSystem(config) as Rule
                },
            ),
        )
    }

    private companion object {
        const val RULE_SET_ID: String = "getfit"
    }
}
