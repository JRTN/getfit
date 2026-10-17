pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "fitness"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":architecture-tests")
include(":detekt-rules")

include(":feature:today")
include(":feature:nutrition")
include(":feature:training")
include(":feature:body")
include(":feature:trends")
include(":feature:settings")

include(":core:model")
include(":core:domain")
include(":core:projection")
include(":core:importers")
include(":core:database")
include(":core:reference")
include(":core:datastore")
include(":core:data")
include(":core:healthconnect")
include(":core:sync")
include(":core:backup")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")
