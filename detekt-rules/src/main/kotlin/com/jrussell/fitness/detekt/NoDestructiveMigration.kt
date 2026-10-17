package com.jrussell.fitness.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Data architecture: `user.db` is "migrated: yes, explicit migrations only".
 *
 * Reports every call whose name starts with `fallbackToDestructiveMigration` (including the
 * `OnDowngrade` and `From` variants), anywhere. A destructive fallback silently deletes the
 * user's only copy of their data when a migration is missing.
 */
class NoDestructiveMigration(config: Config) : Rule(config, DESCRIPTION) {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee: KtNameReferenceExpression? = expression.calleeExpression as? KtNameReferenceExpression
        if (callee?.getReferencedName()?.startsWith(FORBIDDEN_PREFIX) == true) {
            report(Finding(Entity.from(expression), MESSAGE))
        }
    }

    private companion object {
        const val FORBIDDEN_PREFIX: String = "fallbackToDestructiveMigration"
        const val DESCRIPTION: String = "Room databases never fall back to destructive migration."
        const val MESSAGE: String = "Write an explicit Migration instead; a destructive fallback deletes user data."
    }
}
