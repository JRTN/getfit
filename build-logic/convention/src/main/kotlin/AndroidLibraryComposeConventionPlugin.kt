import com.android.build.api.dsl.LibraryExtension
import com.jrussell.fitness.buildlogic.libs
import com.jrussell.fitness.buildlogic.library
import com.jrussell.fitness.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin `app.android.library.compose`: an Android library that contains Jetpack
 * Compose UI.
 *
 * Used directly by `:core:designsystem` and `:core:ui`, and indirectly (through
 * `app.android.feature`) by every `:feature:*` module.
 *
 * Applying it:
 * - applies `app.android.library` (SDK levels, namespace, test stack);
 * - applies the Kotlin Compose compiler plugin and turns on the `compose` build feature;
 * - adds the Compose BOM, which pins every `androidx.compose.*` artifact to one tested set of
 *   versions, plus Compose UI and `@Preview` support (the preview renderer in debug only).
 *
 * ```
 * plugins {
 *     alias(libs.plugins.app.android.library.compose)
 * }
 * ```
 */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    /** Called by Gradle once when a module applies `app.android.library.compose`. */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.android.library")
            pluginManager.apply(libs.pluginId("compose-compiler"))

            extensions.configure<LibraryExtension> {
                buildFeatures.compose = true
            }

            dependencies {
                val composeBom = platform(libs.library("androidx-compose-bom"))
                add("implementation", composeBom)
                add("implementation", libs.library("androidx-compose-ui"))
                add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
                add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
            }
        }
    }
}
