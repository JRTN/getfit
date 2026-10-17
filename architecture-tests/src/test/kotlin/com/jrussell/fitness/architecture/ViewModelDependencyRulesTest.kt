package com.jrussell.fitness.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.KoParameterDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * Design rule: "ViewModels depend only on interfaces from `:core:domain`". Feature ViewModels
 * receive repository interfaces through Hilt and never see storage or implementation classes.
 */
class ViewModelDependencyRulesTest {
    @Test
    fun featureViewModels_dependOnlyOnDomainInterfaces() {
        Konsist.scopeFromProduction()
            .classes()
            .filter { declaration: KoClassDeclaration ->
                declaration.name.endsWith("ViewModel") &&
                    declaration.resideInPackage("com.jrussell.fitness.feature..")
            }
            .assertTrue(strict = true) { viewModel: KoClassDeclaration ->
                val parameters: List<KoParameterDeclaration> = viewModel.primaryConstructor?.parameters.orEmpty()
                parameters.all { parameter: KoParameterDeclaration ->
                    val domainInterface: KoInterfaceDeclaration? =
                        parameter.type.sourceDeclaration?.asInterfaceDeclaration()
                    domainInterface?.fullyQualifiedName?.startsWith("$DOMAIN_PACKAGE.") == true
                }
            }
    }

    private companion object {
        const val DOMAIN_PACKAGE: String = "com.jrussell.fitness.core.domain"
    }
}
