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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StepfunAudioSdk"
include(":app")

include(":StepfunAudioCoreSdk")
include(":StepfunAudioTtsSdk")
include(":StepfunAudioAsrSdk")
include(":StepfunAudioSdk")
