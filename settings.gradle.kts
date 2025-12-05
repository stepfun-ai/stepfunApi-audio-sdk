pluginManagement {
    repositories {
        google ()
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
