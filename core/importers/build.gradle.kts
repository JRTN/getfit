plugins {
    alias(libs.plugins.app.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
}
