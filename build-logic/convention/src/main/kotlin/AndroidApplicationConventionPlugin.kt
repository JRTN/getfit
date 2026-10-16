import com.android.build.api.dsl.ApplicationExtension
import com.jrussell.fitness.buildlogic.BASE_NAMESPACE
import com.jrussell.fitness.buildlogic.TARGET_SDK
import com.jrussell.fitness.buildlogic.configureAndroidCommon
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
 * - **release** builds are shrunk and obfuscated by R8 from the first release onwards.
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

                defaultConfig {
                    applicationId = BASE_NAMESPACE
                    targetSdk = TARGET_SDK
                    versionCode = gitCommitCount().get()
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

                buildFeatures {
                    compose = true
                }
            }
        }
    }

    private companion object {
        /** Semantic version shown to the user; bump by hand for each release. */
        const val VERSION_NAME: String = "0.1.0"
    }
}
