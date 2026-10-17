import com.android.build.api.dsl.CommonExtension
import com.jrussell.fitness.buildlogic.libs
import com.jrussell.fitness.buildlogic.pluginId
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/**
 * Convention plugin `app.quality`: static analysis for one module. Every other convention
 * plugin applies it, so module build files never mention it.
 *
 * Applying it adds three checks, all of which `./gradlew check` runs (locally and in CI):
 * - **ktlint**: formatting (indentation, imports, spacing) in the Android Studio style set in
 *   `.editorconfig`. `./gradlew ktlintFormat` fixes most findings automatically.
 * - **detekt**: code smells and complexity (long methods, magic numbers, swallowed exceptions),
 *   using detekt's defaults plus the overrides in `config/detekt/detekt.yml`.
 *   Includes GetFit's own rules from `:detekt-rules` (e.g. no `Color(` outside the design system).
 * - **Android Lint** (Android modules only): Android-specific problems such as missing
 *   translations, accessibility issues and wrong API-level usage. Any error fails the build.
 *
 * Dependency analysis (`./gradlew buildHealth`) is applied once in the root build file instead,
 * because it looks at all modules together.
 */
class QualityConventionPlugin : Plugin<Project> {
    /** Called by Gradle once when a module applies `app.quality`. */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ktlint"))
            pluginManager.apply(libs.pluginId("detekt"))

            extensions.configure<DetektExtension> {
                config.from(rootProject.file("config/detekt/detekt.yml"))
                buildUponDefaultConfig.set(true)
                parallel.set(true)
            }

            // GetFit's own rules (rule set "getfit"), built from :detekt-rules. That module
            // cannot load its own rules while it is being compiled, so it is skipped.
            if (path != DETEKT_RULES_PROJECT) {
                dependencies.add("detektPlugins", dependencies.project(mapOf("path" to DETEKT_RULES_PROJECT)))
            }

            pluginManager.withPlugin(libs.pluginId("android-application")) {
                configureLint(this@with)
            }
            pluginManager.withPlugin(libs.pluginId("android-library")) {
                configureLint(this@with)
            }
        }
    }

    private fun configureLint(project: Project) {
        val android: CommonExtension = project.extensions.getByType<CommonExtension>()
        android.lint.apply {
            abortOnError = true
            checkDependencies = false
            // Version-freshness checks: upgrades are deliberate, made through the version catalog.
            disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
        }
    }
}

private const val DETEKT_RULES_PROJECT: String = ":detekt-rules"
