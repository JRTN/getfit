package com.jrussell.fitness.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Design rule: "no composable outside `:core:designsystem` uses a `Color(...)` literal".
 *
 * Colors must come from `MaterialTheme.colorScheme` or `GetFitTheme.dataColors`, so every
 * palette (including color-vision-safe and high contrast) applies everywhere. Outside package
 * `com.jrussell.fitness.core.designsystem` this reports:
 * - `Color(...)` constructor calls;
 * - members of Compose's `Color` other than `Color.Unspecified` and `Color.Transparent`
 *   (named constants such as `Color.Red`, and factories such as `Color.hsl`/`Color.hsv`);
 * - members of `android.graphics.Color` other than `TRANSPARENT` (e.g. `parseColor`, `rgb`);
 * - aliased imports of either `Color` class, which would hide the calls above.
 *
 * The rule reads syntax only, so a bare `Color` receiver may be either class; `TRANSPARENT` is
 * allowed on it because Compose's `Color` has no such member.
 */
class NoColorLiteralOutsideDesignSystem(config: Config) : Rule(config, DESCRIPTION) {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee: KtNameReferenceExpression? = expression.calleeExpression as? KtNameReferenceExpression
        if (callee?.getReferencedName() == COLOR && !isInDesignSystem(expression)) {
            report(Finding(Entity.from(expression), MESSAGE))
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        val allowedMembers: Set<String> = allowedMembersFor(expression.receiverExpression.text) ?: return
        val member: String? = memberName(expression.selectorExpression)
        if (member != null && member !in allowedMembers && !isInDesignSystem(expression)) {
            report(Finding(Entity.from(expression), MESSAGE))
        }
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val importedName: String? = importDirective.importedFqName?.asString()
        if (importDirective.aliasName != null && importedName in COLOR_CLASSES && !isInDesignSystem(importDirective)) {
            report(Finding(Entity.from(importDirective), MESSAGE))
        }
    }

    private fun allowedMembersFor(receiver: String): Set<String>? {
        return when (receiver) {
            COLOR, COMPOSE_COLOR -> COMPOSE_ALLOWED_MEMBERS
            ANDROID_COLOR -> ANDROID_ALLOWED_MEMBERS
            else -> null
        }
    }

    private fun memberName(selector: KtExpression?): String? {
        return when (selector) {
            is KtNameReferenceExpression -> selector.getReferencedName()
            is KtCallExpression -> (selector.calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
            else -> null
        }
    }

    private fun isInDesignSystem(element: KtElement): Boolean {
        val packageName: String = element.containingKtFile.packageFqName.asString()
        return packageName == DESIGN_SYSTEM_PACKAGE || packageName.startsWith("$DESIGN_SYSTEM_PACKAGE.")
    }

    private companion object {
        const val COLOR: String = "Color"
        const val COMPOSE_COLOR: String = "androidx.compose.ui.graphics.Color"
        const val ANDROID_COLOR: String = "android.graphics.Color"
        val COLOR_CLASSES: Set<String> = setOf(COMPOSE_COLOR, ANDROID_COLOR)
        val COMPOSE_ALLOWED_MEMBERS: Set<String> = setOf("Unspecified", "Transparent", "TRANSPARENT")
        val ANDROID_ALLOWED_MEMBERS: Set<String> = setOf("TRANSPARENT")
        const val DESIGN_SYSTEM_PACKAGE: String = "com.jrussell.fitness.core.designsystem"
        const val DESCRIPTION: String = "Color literals are only allowed in :core:designsystem."
        const val MESSAGE: String =
            "Colors come from GetFitTheme / MaterialTheme; define new colors in :core:designsystem."
    }
}
