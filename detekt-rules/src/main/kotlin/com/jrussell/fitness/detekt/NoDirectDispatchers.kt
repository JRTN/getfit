package com.jrussell.fitness.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Code convention 6: "no code references `Dispatchers.IO` directly, so tests run deterministically."
 *
 * Reports `Dispatchers.IO`, `.Default`, `.Main` and `.Unconfined` (plain or fully qualified)
 * anywhere except package `com.jrussell.fitness.di`, where `:app` provides them behind the
 * `@IoDispatcher` / `@DefaultDispatcher` qualifiers. Calls such as `Dispatchers.setMain(...)`
 * (used by test rules) are not dispatcher references and are allowed.
 */
class NoDirectDispatchers(config: Config) : Rule(config, DESCRIPTION) {
    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        val selector: KtNameReferenceExpression? = expression.selectorExpression as? KtNameReferenceExpression
        val isDispatcherReference: Boolean = expression.receiverExpression.text in RECEIVERS &&
            selector?.getReferencedName() in DISPATCHERS
        if (isDispatcherReference && expression.containingKtFile.packageFqName.asString() != ALLOWED_PACKAGE) {
            report(Finding(Entity.from(expression), MESSAGE))
        }
    }

    private companion object {
        val RECEIVERS: Set<String> = setOf("Dispatchers", "kotlinx.coroutines.Dispatchers")
        val DISPATCHERS: Set<String> = setOf("IO", "Default", "Main", "Unconfined")
        const val ALLOWED_PACKAGE: String = "com.jrussell.fitness.di"
        const val DESCRIPTION: String = "Dispatchers are injected through qualifiers, never referenced directly."
        const val MESSAGE: String = "Inject @IoDispatcher or @DefaultDispatcher instead of naming Dispatchers.*."
    }
}
