plugins {
    alias(libs.plugins.app.android.library)
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.sqlite)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.sqlite.framework)
}
