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
            // M0 only: feature modules are still empty, so the standard :core:* dependencies the
            // feature convention adds are unused. Restore "fail" in M1.
            onUnusedDependencies {
                severity("warn")
            }
        }
    }
}
