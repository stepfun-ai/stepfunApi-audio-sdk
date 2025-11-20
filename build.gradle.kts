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
                        // 动态生成 Group ID，例如: com.stepfun.stepfunaudiocoresdk
                        groupId = "com.stepfun.${project.name.lowercase()}"
                        // 使用模块名作为 Artifact ID
                        artifactId = project.name
                        // 统一版本号，也可以从 gradle.properties 读取
                        version = "1.0.0"

                        // 确保 release 组件存在
                        val releaseComponent = components.findByName("release")
                        if (releaseComponent != null) {
                            from(releaseComponent)
                        }
                    }
                }
            }
        }
    }
}