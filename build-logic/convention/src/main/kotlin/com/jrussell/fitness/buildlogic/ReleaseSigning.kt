package com.jrussell.fitness.buildlogic

import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project

/** Gradle property naming the release keystore file (absolute path). */
const val STORE_FILE_PROPERTY: String = "getfit.release.storeFile"

/** Gradle property holding the keystore password. */
const val STORE_PASSWORD_PROPERTY: String = "getfit.release.storePassword"

/** Gradle property naming the signing key inside the keystore. */
const val KEY_ALIAS_PROPERTY: String = "getfit.release.keyAlias"

/** Gradle property holding the signing key's password. */
const val KEY_PASSWORD_PROPERTY: String = "getfit.release.keyPassword"

/**
 * Signs release builds with the app's permanent release key, if this machine has it.
 *
 * The four `getfit.release.*` properties are read as Gradle properties. They live in the
 * developer's own `~/.gradle/gradle.properties`, outside the repository, so neither the keystore
 * nor its passwords are ever committed. CI can supply the same names from secrets later
 * (`ORG_GRADLE_PROJECT_getfit.release.storeFile`, ...).
 *
 * Values are trimmed: `.properties` files keep trailing whitespace, which editors easily add.
 *
 * When any property is missing (a fresh clone, or CI today), release builds are produced
 * unsigned. They can't be installed, but they still build, so R8 and resource shrinking keep
 * being exercised.
 *
 * The key must never be lost: Android only installs an update over an app signed with the same
 * key, so losing it means uninstalling, which deletes all of the app's data.
 */
fun Project.configureReleaseSigning(extension: ApplicationExtension) {
    val storeFilePath: String? = providers.gradleProperty(STORE_FILE_PROPERTY).orNull?.trim()
    val storePassword: String? = providers.gradleProperty(STORE_PASSWORD_PROPERTY).orNull?.trim()
    val keyAlias: String? = providers.gradleProperty(KEY_ALIAS_PROPERTY).orNull?.trim()
    val keyPassword: String? = providers.gradleProperty(KEY_PASSWORD_PROPERTY).orNull?.trim()
    if (storeFilePath == null || storePassword == null || keyAlias == null || keyPassword == null) {
        return
    }

    val release: ApkSigningConfig = extension.signingConfigs.create("release") {
        this.storeFile = file(storeFilePath)
        this.storePassword = storePassword
        this.keyAlias = keyAlias
        this.keyPassword = keyPassword
    }
    extension.buildTypes.getByName("release").signingConfig = release
}
