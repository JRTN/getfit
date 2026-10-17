plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.android.room)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.datetime)

    testImplementation(libs.kotlinx.coroutines.test)
}
