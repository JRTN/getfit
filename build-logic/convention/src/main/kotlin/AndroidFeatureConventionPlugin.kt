import com.jrussell.fitness.buildlogic.libs
import com.jrussell.fitness.buildlogic.library
import com.jrussell.fitness.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin `app.android.feature`: a user-facing feature module (`:feature:today`,
 * `:feature:nutrition`, `:feature:training`, `:feature:body`, `:feature:trends`,
 * `:feature:settings`).
 *
 * Applying it:
 * - applies `app.android.library.compose` (Android library with Compose) and `app.hilt`
 *   (so ViewModels can receive repositories by injection);
 * - applies kotlinx.serialization, which Navigation 3 uses to save and restore a feature's
 *   navigation keys (`@Serializable data object TodayKey : NavKey`);
 * - adds the dependencies every feature needs, which also enforces the design's dependency rule:
 *   a feature may depend only on `:core:model`, `:core:domain`, `:core:ui` and
 *   `:core:designsystem`, never on storage modules or on another feature. Cross-feature
 *   navigation is wired up in `:app`;
 * - adds the test stack for ViewModels and screens: `:core:testing` fakes, Turbine,
 *   coroutine test dispatchers, Robolectric and Compose UI test. (Each module with Robolectric
 *   tests also needs `src/test/resources/robolectric.properties` pinning `sdk=36`.)
 *
 * A feature module's build file is just:
 * ```
 * plugins {
 *     alias(libs.plugins.app.android.feature)
 * }
 * ```
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    /** Called by Gradle once when a module applies `app.android.feature`. */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.android.library.compose")
            pluginManager.apply("app.hilt")
            pluginManager.apply(libs.pluginId("kotlin-serialization"))

            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:designsystem"))

                add("implementation", libs.library("androidx-compose-material3"))
                add("implementation", libs.library("androidx-navigation3-runtime"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("kotlinx-serialization-json"))

                // ViewModel tests (fakes, Turbine, coroutine test dispatchers) and Robolectric
                // Compose tests for semantics (roles, selection, content descriptions).
                add("testImplementation", project(":core:testing"))
                add("testImplementation", libs.library("turbine"))
                add("testImplementation", libs.library("kotlinx-coroutines-test"))
                add("testImplementation", libs.library("robolectric"))
                add("testImplementation", platform(libs.library("androidx-compose-bom")))
                add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
                add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
            }
        }
    }
}
