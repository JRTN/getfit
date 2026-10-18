plugins {
    alias(libs.plugins.app.android.library.compose)
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(libs.vico.compose.m3)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
