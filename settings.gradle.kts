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
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            metadataSources {
                mavenPom()
                artifact()
                ignoreGradleMetadataRedirection()
            }
            content {
                includeGroup("com.github.FaceOnLive")
                includeGroup("com.github.philburk")
                includeGroup("com.github.racra")
                includeGroup("com.github.tdlibx")
                includeGroup("com.github.TeamNewPipe")
            }
        }
        maven("https://storage.googleapis.com/snap-kit-build/maven")
    }
}

rootProject.name = "PixelMusic"
include(":app")
include(":baselineprofile")
