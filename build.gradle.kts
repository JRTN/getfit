plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.dependency.analysis) apply false
}

apply(plugin = libs.plugins.dependency.analysis.get().pluginId)

configure<com.autonomousapps.DependencyAnalysisExtension> {
    issues {
        all {
            onAny {
                severity("fail")
            }
            // Nutrition and Training are still placeholder screens, so some of the standard :core:*
            // dependencies the feature convention adds are unused there. Restore "fail" once they have code.
            onUnusedDependencies {
                severity("warn")
            }
        }
    }
}
