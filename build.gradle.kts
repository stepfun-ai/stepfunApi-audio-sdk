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

// 使用 JitPack 传递的 group 和 version 参数
val jitpackGroupId = project.findProperty("group")?.toString()
    ?: "com.github.stepfun-ai.stepfunApi-audio-sdk"
val jitpackVersion = project.findProperty("version")?.toString() ?: "1.0.0"

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
                        version = jitpackVersion

                        // 确保 release 组件存在
                        val releaseComponent = components.findByName("release")
                        if (releaseComponent != null) {
                            from(releaseComponent)
                        }

                        // 修正 POM 中的项目依赖坐标
                        pom.withXml {
                            val dependenciesNode = asNode().children().find {
                                (it as? groovy.util.Node)?.name()?.toString()?.contains("dependencies") == true
                            } as? groovy.util.Node

                            dependenciesNode?.children()?.forEach { depNode ->
                                val node = depNode as? groovy.util.Node
                                node?.children()?.forEach { child ->
                                    val childNode = child as? groovy.util.Node
                                    val nodeName = childNode?.name()?.toString() ?: ""

                                    // 替换项目依赖的 groupId
                                    if (nodeName.contains("groupId")) {
                                        val groupIdValue = childNode?.value()?.toString() ?: ""
                                        if (groupIdValue.contains("StepfunAudio") ||
                                            groupIdValue.contains("stepfunaudio")) {
                                            childNode?.setValue(jitpackGroupId)
                                        }
                                    }

                                    // 替换项目依赖的 version
                                    if (nodeName.contains("version")) {
                                        val versionValue = childNode?.value()?.toString() ?: ""
                                        if (versionValue == "unspecified" || versionValue == "1.0.0") {
                                            childNode?.setValue(jitpackVersion)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
