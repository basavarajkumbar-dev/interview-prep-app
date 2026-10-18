pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://chaquo.com/maven") }
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS: plugins such as Chaquopy may add their own repositories
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Chaquopy (embedded Python for Coding Practice)
        maven { url = uri("https://chaquo.com/maven") }
    }
}

rootProject.name = "InterviewPrep"

include(":app")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":feature:home")
include(":feature:learn")
include(":feature:quiz")
include(":feature:flashcards")
include(":feature:interview")
include(":feature:coding")
include(":feature:progress")
include(":feature:settings")
