plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.android.room)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.datetime)
    // GetFitDatabase extends RoomDatabase, so modules that hold it (:app, :core:data) need Room types.
    api(libs.room.runtime)

    testImplementation(libs.kotlinx.coroutines.test)
}
