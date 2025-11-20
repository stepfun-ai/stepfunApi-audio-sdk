package com.stepfun.stepfunaudiocoresdk.audio.common.config

/**
 * ASR 配置类
 */
data class AsrConfig(
    // 默认模型
    val defaultModel: AsrModel = AsrModel.STEP_ASR,

    // 默认响应格式
    val defaultResponseFormat: AsrResponseFormat = AsrResponseFormat.JSON,

    // 最大文件大小（字节）
    val maxFileSize: Long = 100 * 1024 * 1024, // 100MB

    // 是否启用缓存
    val enableCache: Boolean = false,

    // 缓存目录
    val cacheDir: String? = null
)

/**
 * ASR 模型枚举
 */
enum class AsrModel(val modelId: String) {
    STEP_ASR("step-asr")
}

/**
 * ASR 响应格式
 */
enum class AsrResponseFormat(val format: String) {
    JSON("json"),
    TEXT("text"),
    SRT("srt"),
    VTT("vtt")
}

/**
 * 支持的音频格式
 */
enum class AsrAudioFormat(val extension: String) {
    FLAC("flac"),
    MP3("mp3"),
    MP4("mp4"),
    MPEG("mpeg"),
    MPGA("mpga"),
    M4A("m4a"),
    OGG("ogg"),
    WAV("wav"),
    WEBM("webm"),
    AAC("aac"),
    OPUS("opus");

    companion object {
        fun isSupported(extension: String): Boolean {
            return values().any { it.extension.equals(extension, ignoreCase = true) }
        }

        fun getAllExtensions(): List<String> {
            return values().map { it.extension }
        }
    }
}