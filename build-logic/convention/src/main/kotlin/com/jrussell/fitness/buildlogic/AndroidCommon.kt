package com.jrussell.fitness.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Every module's namespace starts with this; it is also the release `applicationId`. */
const val BASE_NAMESPACE: String = "com.jrussell.fitness"

/** API level the app is compiled against (decision #2 in the design doc, amended 2026-10-16). */
const val COMPILE_SDK: Int = 37

/** Lowest API level the app installs on: Android 14, the first with Health Connect built in. */
const val MIN_SDK: Int = 34

/** API level whose runtime behaviors the app opts into. */
const val TARGET_SDK: Int = 37

/**
 * Settings shared by every Android module, application and library alike: SDK levels,
 * Java/Kotlin bytecode target (17), the namespace derived from the module path, and the JVM
 * flags local unit tests need.
 *
 * @param extension the module's `android { }` block.
 * @param namespace defaults to [BASE_NAMESPACE] plus the module path with ':' replaced by '.',
 *   so `:feature:nutrition` becomes `com.jrussell.fitness.feature.nutrition`.
 */
fun Project.configureAndroidCommon(
    extension: CommonExtension,
    namespace: String = BASE_NAMESPACE + path.replace(':', '.'),
) {
    extension.namespace = namespace
    extension.compileSdk = COMPILE_SDK
    extension.defaultConfig.minSdk = MIN_SDK
    extension.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    extension.compileOptions.targetCompatibility = JavaVersion.VERSION_17

    // AGP 9 compiles Kotlin itself ("built-in Kotlin"); this is the extension it exposes.
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Local unit tests run on whatever JDK runs Gradle (Android Studio's JBR 25 locally).
    // Robolectric reads FileDescriptor internals through jdk.internal.access, which newer JDKs
    // no longer export; without this every Robolectric test fails during setup.
    tasks.withType<Test>().configureEach {
        jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
    }
}
