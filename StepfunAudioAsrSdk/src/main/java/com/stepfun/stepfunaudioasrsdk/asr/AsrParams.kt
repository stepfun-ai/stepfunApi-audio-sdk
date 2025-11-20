package com.stepfun.stepfunaudioasrsdk.asr

import com.stepfun.stepfunaudiocoresdk.audio.common.config.AsrAudioFormat
import com.stepfun.stepfunaudiocoresdk.audio.common.config.AsrModel
import com.stepfun.stepfunaudiocoresdk.audio.common.config.AsrResponseFormat
import java.io.File

data class AsrParams(
    val file: File,
    val model: AsrModel = AsrModel.STEP_ASR,
    val responseFormat: AsrResponseFormat,

    val hotWords: List<String>? = null
) {
    class Builder {
        private var file: File? = null
        private var model: AsrModel = AsrModel.STEP_ASR
        private var responseFormat: AsrResponseFormat = AsrResponseFormat.JSON
        private var hotWords: List<String>? = null

        fun file(file: File) = apply { this.file = file }

        fun model(model: AsrModel) = apply { this.model = model }

        fun responseFormat(format: AsrResponseFormat) = apply { this.responseFormat = format }

        fun hotWords(hotWords: List<String>) = apply { this.hotWords = hotWords }

        fun build(): AsrParams {
            val audioFile = file ?: throw IllegalArgumentException("必须设置音频文件")

            if (!audioFile.exists()) {
                throw IllegalArgumentException("音频文件不存在: ${audioFile.absolutePath}")
            }

            val maxSize = 100 * 1024 * 1024 // 100MB

            if (audioFile.length() > maxSize) {
                throw IllegalArgumentException("音频文件过大，最大支持100MB: ${audioFile.absolutePath}")
            }


            if (!AsrAudioFormat.isSupported(audioFile.extension.lowercase())) {
                throw IllegalArgumentException("不支持的音频格式: ${audioFile.extension} , 支持的格式有: ${AsrAudioFormat.values().joinToString { it.extension }}")
            }
            return AsrParams(
                file = audioFile,
                model = model,
                responseFormat = responseFormat,
                hotWords = hotWords
            )
        }
    }

    internal fun validate() : AsrError? {
        if (!file.exists()) {
            return AsrError(
                code = AsrError.ERROR_FILE_NOT_FOUND,
                message = "音频文件不存在: ${file.absolutePath}"
            )
        }
        val maxSize = 100 * 1024 * 1024 // 100MB
        if (file.length() > maxSize) {
            return AsrError(
                code = AsrError.ERROR_FILE_TOO_LARGE,
                message = "音频文件过大，最大支持100MB: ${file.absolutePath}"
            )
        }
        return null
    }
}