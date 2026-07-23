pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // vendored mididriver release, used by the embedded runtime for midi and tones
        maven { url = uri("mididriver/maven") }
        google()
        mavenCentral()
        // pngj, used by the embedded j2me runtime
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ANPASIDE"
include(":app")
include(":j2me")
include(":dexlib")
