import com.android.build.api.dsl.ApplicationExtension
import com.jrussell.fitness.buildlogic.BASE_NAMESPACE
import com.jrussell.fitness.buildlogic.TARGET_SDK
import com.jrussell.fitness.buildlogic.configureAndroidCommon
import com.jrussell.fitness.buildlogic.configureReleaseSigning
import com.jrussell.fitness.buildlogic.gitCommitCount
import com.jrussell.fitness.buildlogic.libs
import com.jrussell.fitness.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin `app.android.application`: configures the installable app module, `:app`.
 *
 * Applying it:
 * - applies the Android Gradle Plugin's application plugin and the Compose compiler plugin;
 * - sets SDK levels, JVM 17, namespace and `applicationId` `com.jrussell.fitness`;
 * - sets `versionCode` from the git commit count and `versionName` by hand ([VERSION_NAME]);
 * - **debug** builds get the `.debug` suffix, so `com.jrussell.fitness.debug` installs beside
 *   the release app with its own separate database;
 * - **release** builds are shrunk and obfuscated by R8 from the first release onwards, and are
 *   signed with the release key when this machine has it (see `configureReleaseSigning`).
 * - unit tests see Android resources, for Robolectric Compose tests of the app shell.
 *
 * ```
 * plugins {
 *     alias(libs.plugins.app.android.application)
 * }
 * ```
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    /** Called by Gradle once when `:app` applies `app.android.application`. */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.quality")
            pluginManager.apply(libs.pluginId("android-application"))
            pluginManager.apply(libs.pluginId("compose-compiler"))

            extensions.configure<ApplicationExtension> {
                configureAndroidCommon(this, namespace = BASE_NAMESPACE)
                // Robolectric Compose tests (the app shell's navigation) need merged resources and the
                // test activity from ui-test-manifest, as library modules already have.
                testOptions.unitTests.isIncludeAndroidResources = true

                defaultConfig {
                    applicationId = BASE_NAMESPACE
                    targetSdk = TARGET_SDK
                    versionCode = gitCommitCount()
                    versionName = VERSION_NAME
                }

                buildTypes {
                    getByName("debug") {
                        applicationIdSuffix = ".debug"
                    }
                    getByName("release") {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )
                    }
                }

                configureReleaseSigning(this)

                buildFeatures {
                    compose = true
                }
            }
        }
    }

    private companion object {
        /**
         * Semantic version shown to the user. Bump it by hand in the commit that gets tagged
         * `v<VERSION_NAME>` on `master` (see "Versioning and branching" in docs/initial_design.md):
         * the minor version for each newly useful capability, the patch version for fixes, and
         * 1.0.0 when the original design is complete.
         */
        const val VERSION_NAME: String = "0.0.1"
    }
}
