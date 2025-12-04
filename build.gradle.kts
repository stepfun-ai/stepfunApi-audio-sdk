// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
}

val publishableModules = setOf(
    "StepfunAudioSdk",
    "StepfunAudioCoreSdk",
    "StepfunAudioAsrSdk",
    "StepfunAudioTtsSdk",
)

val jitpackGroupId = "com.github.stepfun-ai.stepfunApi-audio-sdk"

subprojects {
    if (project.name in publishableModules) {
        apply(plugin = "maven-publish")

        plugins.withId("com.android.library") {
            extensions.configure<com.android.build.gradle.LibraryExtension> {
                publishing {
                    singleVariant("release") {
                        withSourcesJar()
                    }
                }
            }
        }

        afterEvaluate {
            extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("release") {
                        groupId = jitpackGroupId
                        artifactId = project.name
                        version = project.findProperty("VERSION_NAME")?.toString() ?: "1.0.0"

                        // 确保 release 组件存在
                        val releaseComponent = components.findByName("release")
                        if (releaseComponent != null) {
                            from(releaseComponent)
                        }
                        
                        pom.withXml {
                            val dependencies = asNode().appendNode("dependencies")
                            configurations.findByName("api")?.dependencies?.forEach { dep ->
                                if (dep is ProjectDependency) {
                                    val depNode = dependencies.appendNode("dependency")
                                    depNode.appendNode("groupId", jitpackGroupId)
                                    depNode.appendNode("artifactId", dep.name)
                                    depNode.appendNode("version", version)
                                    depNode.appendNode("scope", "compile")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}