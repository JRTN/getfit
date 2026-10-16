plugins {
    alias(libs.plugins.app.jvm.library)
}

dependencies {
    testImplementation(libs.konsist)
}

// Konsist reads every module's source files straight from disk, so Gradle must treat them as
// inputs; otherwise a new violation elsewhere leaves this test task UP-TO-DATE (and green).
tasks.withType<Test>().configureEach {
    inputs.files(
        rootProject.fileTree(rootDir) {
            include("**/src/**/*.kt")
            exclude("**/build/**", "build-logic/**", ".gradle/**", ".superpowers/**")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("konsistScannedSources")
}
