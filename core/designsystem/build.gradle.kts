plugins {
    alias(libs.plugins.app.android.library.compose)
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.compose.material3)
}
