plugins {
    alias(libs.plugins.app.android.library)
}

dependencies {
    api(libs.androidx.datastore.preferences)
    implementation(projects.core.model)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
