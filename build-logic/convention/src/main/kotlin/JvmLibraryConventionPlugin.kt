import com.jrussell.fitness.buildlogic.libs
import com.jrussell.fitness.buildlogic.library
import com.jrussell.fitness.buildlogic.pluginId
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Convention plugin `app.jvm.library`: configures a pure-Kotlin (JVM) library module.
 *
 * Used by the modules that must never depend on Android — `:core:model`, `:core:domain`,
 * `:core:projection`, `:core:importers` — plus `:core:testing`. Keeping them on plain
 * Kotlin/JVM is what lets them become Kotlin Multiplatform modules later without rewrites;
 * at that point this plugin is replaced by an `app.kmp.library` plugin.
 *
 * Applying it to a module:
 * - applies the Kotlin/JVM Gradle plugin;
 * - compiles Java and Kotlin to JVM 17 bytecode (the same target as the Android modules);
 * - adds the shared test stack — JUnit 5 (Jupiter) and AssertK — and runs tests on the
 *   JUnit Platform.
 *
 * A module's build file then needs only:
 * ```
 * plugins {
 *     alias(libs.plugins.app.jvm.library)
 * }
 * ```
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    /**
     * Called by Gradle once when a module applies `app.jvm.library`.
     *
     * @param target the module (Gradle project) being configured.
     */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.quality")
            pluginManager.apply(libs.pluginId("kotlin-jvm"))

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }

            // Gradle 9 no longer supplies the JUnit Platform launcher implicitly, hence testRuntimeOnly.
            dependencies {
                add("testImplementation", platform(libs.library("junit5-bom")))
                add("testImplementation", libs.library("junit5-jupiter"))
                add("testImplementation", libs.library("assertk"))
                add("testRuntimeOnly", libs.library("junit5-platform-launcher"))
            }

            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
        }
    }
}
