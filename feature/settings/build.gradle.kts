plugins {
    alias(libs.plugins.app.android.feature)
}

dependencies {
    // The system file pickers (Storage Access Framework) for backup and restore.
    implementation(libs.androidx.activity.compose)
}
