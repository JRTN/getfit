package com.jrussell.fitness.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoAnnotationDeclaration
import com.lemonappdev.konsist.api.declaration.KoFunctionDeclaration
import com.lemonappdev.konsist.api.declaration.KoParameterDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * Design rule: "DAOs take `ownerId` as a parameter on every query." Every `@Query` in the
 * owner-scoped DAO package must take an `ownerId`. The owner-less exceptions (the revision
 * counter, dataset versions, and the profile lookup that discovers the owner) live in package
 * `core.database.infrastructure` instead.
 */
class DaoOwnerScopingRulesTest {
    @Test
    fun ownerScopedDaos_takeOwnerIdOnEveryQuery() {
        Konsist.scopeFromProduction()
            .functions()
            .filter { function: KoFunctionDeclaration ->
                function.resideInPackage("$OWNER_SCOPED_DAO_PACKAGE..") &&
                    function.annotations.any { annotation: KoAnnotationDeclaration -> annotation.name == "Query" }
            }
            .assertTrue(strict = true) { query: KoFunctionDeclaration ->
                query.parameters.any { parameter: KoParameterDeclaration -> parameter.name == "ownerId" }
            }
    }

    private companion object {
        const val OWNER_SCOPED_DAO_PACKAGE: String = "com.jrussell.fitness.core.database.dao"
    }
}
