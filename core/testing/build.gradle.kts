plugins {
    alias(libs.plugins.app.jvm.library)
}

dependencies {
    api(projects.core.domain)
    api(libs.kotlinx.coroutines.test)
    api(libs.junit4)
}
