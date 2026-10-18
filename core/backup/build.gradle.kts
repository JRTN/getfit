plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.hilt)
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.sqlite.framework)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
