plugins {
    alias(libs.plugins.app.android.application)
    alias(libs.plugins.app.hilt)
}

dependencies {
    implementation(projects.feature.today)
    implementation(projects.feature.nutrition)
    implementation(projects.feature.training)
    implementation(projects.feature.body)
    implementation(projects.feature.trends)
    implementation(projects.feature.settings)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.sync)
    implementation(projects.core.model)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
