import com.jrussell.fitness.buildlogic.libs
import com.jrussell.fitness.buildlogic.library
import com.jrussell.fitness.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin `app.hilt`: enables Hilt dependency injection in an Android module.
 *
 * Used by `:app`, `:core:data` and every `:feature:*` module (via `app.android.feature`).
 *
 * Applying it:
 * - applies KSP, the annotation processor that generates Hilt's code (the design forbids kapt);
 * - applies the Hilt Gradle plugin, which rewrites `@AndroidEntryPoint` and `@HiltAndroidApp`
 *   classes to extend Hilt's generated base classes;
 * - adds the Hilt runtime and its KSP code generator.
 *
 * Apply it after an Android plugin:
 * ```
 * plugins {
 *     alias(libs.plugins.app.android.library)
 *     alias(libs.plugins.app.hilt)
 * }
 * ```
 */
class HiltConventionPlugin : Plugin<Project> {
    /** Called by Gradle once when a module applies `app.hilt`. */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ksp"))
            pluginManager.apply(libs.pluginId("hilt"))

            dependencies {
                add("implementation", libs.library("hilt-android"))
                add("ksp", libs.library("hilt-compiler"))
            }
        }
    }
}
