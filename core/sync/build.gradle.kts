plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.hilt)
}

dependencies {
    testImplementation(libs.kotlinx.coroutines.test)
}
