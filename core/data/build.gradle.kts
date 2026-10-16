plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.hilt)
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.database)
    implementation(projects.core.reference)
    implementation(projects.core.datastore)
    implementation(projects.core.healthconnect)
    implementation(projects.core.backup)
}
