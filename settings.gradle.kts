pluginManagement {
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

rootProject.name = "MyDailyNote"
include(":app")
include(":core:data")
include(":core:ui")
include(":feature:home")
include(":feature:group")
include(":feature:settings")
include(":baselineprofile")
