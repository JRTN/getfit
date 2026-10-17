package com.jrussell.fitness.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Design rule: "no composable outside `:core:designsystem` uses a `Color(...)` literal".
 *
 * Reports any `Color(...)` constructor call in a file whose package is not under
 * `com.jrussell.fitness.core.designsystem`. Colours must come from `MaterialTheme.colorScheme`
 * or `GetFitTheme.dataColors`, so every palette (including colour-vision-safe and high
 * contrast) applies everywhere. Constants such as `Color.Unspecified` are not calls and are allowed.
 */
class NoColorLiteralOutsideDesignSystem(config: Config) : Rule(config, DESCRIPTION) {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee: KtNameReferenceExpression? = expression.calleeExpression as? KtNameReferenceExpression
        if (callee?.getReferencedName() == COLOR && !isInDesignSystem(expression)) {
            report(Finding(Entity.from(expression), MESSAGE))
        }
    }

    private fun isInDesignSystem(expression: KtCallExpression): Boolean {
        val packageName: String = expression.containingKtFile.packageFqName.asString()
        return packageName == DESIGN_SYSTEM_PACKAGE || packageName.startsWith("$DESIGN_SYSTEM_PACKAGE.")
    }

    private companion object {
        const val COLOR: String = "Color"
        const val DESIGN_SYSTEM_PACKAGE: String = "com.jrussell.fitness.core.designsystem"
        const val DESCRIPTION: String = "Color literals are only allowed in :core:designsystem."
        const val MESSAGE: String =
            "Colours come from GetFitTheme / MaterialTheme; define new colours in :core:designsystem."
    }
}
