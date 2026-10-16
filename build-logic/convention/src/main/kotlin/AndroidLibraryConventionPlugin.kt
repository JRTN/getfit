import com.android.build.api.dsl.LibraryExtension
import com.jrussell.fitness.buildlogic.configureAndroidCommon
import com.jrussell.fitness.buildlogic.libs
import com.jrussell.fitness.buildlogic.library
import com.jrussell.fitness.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin `app.android.library`: configures an Android library module.
 *
 * Used directly by the Android `:core:*` modules that have no UI (`:core:database`,
 * `:core:data`, `:core:healthconnect`, ...) and indirectly, through
 * `app.android.library.compose` and `app.android.feature`, by every UI module.
 *
 * Applying it to a module:
 * - applies the Android Gradle Plugin's library plugin, which also compiles Kotlin. AGP 9 has
 *   built-in Kotlin, so `org.jetbrains.kotlin.android` must NOT be applied as well;
 * - sets SDK levels, JVM 17 and the namespace from the module path (see `configureAndroidCommon`);
 * - lets local unit tests see Android resources (needed by Robolectric);
 * - adds the Android-module test stack: JUnit 4 and AssertK.
 *
 * ```
 * plugins {
 *     alias(libs.plugins.app.android.library)
 * }
 * ```
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    /** Called by Gradle once when a module applies `app.android.library`. */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.quality")
            pluginManager.apply(libs.pluginId("android-library"))

            extensions.configure<LibraryExtension> {
                configureAndroidCommon(this)
                testOptions.unitTests.isIncludeAndroidResources = true
            }

            dependencies {
                add("testImplementation", libs.library("junit4"))
                add("testImplementation", libs.library("assertk"))
            }
        }
    }
}
