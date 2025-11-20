package com.stepfun.stepfunaudioasrsdk.asr

import com.stepfun.stepfunaudiocoresdk.audio.common.config.AsrResponseFormat
import com.stepfun.stepfunaudiocoresdk.audio.core.SpeechCoreSdk
import java.io.File

object Asr {

    private var asrClient: AsrClient? = null

    private fun getClient(): AsrClient {
        if (SpeechCoreSdk.isInitialized().not()) {
            throw IllegalStateException("SpeechCoreSdk not initialized. Call init() first.")
        }
        if (asrClient == null) {
            asrClient = AsrClient()
        }
        return asrClient!!
    }

    /**
     * 识别音频文件（使用默认配置）
     *
     * @param file 音频文件
     * @param callback 识别结果回调
     */
    fun transcribe(
        file: File,
        callback: AsrCallback
    ) {
        val params = AsrParams.Builder()
            .file(file)
            .responseFormat(AsrResponseFormat.JSON)
            .build()

        getClient().transcribe(params, callback)
    }

    /**
     * 识别音频文件（使用自定义参数）
     *
     * @param params ASR参数
     * @param callback 识别结果回调
     */
    fun transcribe(
        params: AsrParams,
        callback: AsrCallback
    ) {
        getClient().transcribe(params, callback)
    }

    /**
     * 识别音频文件（协程异步方式）
     *
     * @param params ASR参数
     * @return 识别结果
     */
    suspend fun transcribeAsync(params: AsrParams): AsrResult {
        return getClient().transcribeAsync(params)
    }

    /**
     * 快速识别（使用默认配置）
     *
     * @param file 音频文件
     * @param hotwords 热词列表（可选）
     * @param callback 识别结果回调
     */
    fun quickTranscribe(
        file: File,
        hotwords: List<String>? = null,
        callback: AsrCallback
    ) {
        val builder = AsrParams.Builder()
            .file(file)
            .responseFormat(AsrResponseFormat.JSON)

        hotwords?.let { builder.hotWords(it) }

        val params = builder.build()
        getClient().transcribe(params, callback)
    }

    /**
     * 取消所有识别任务
     */
    fun cancelAll() {
        asrClient?.cancelAll()
    }

    /**
     * 释放所有资源
     */
    fun release() {
        asrClient?.release()
        asrClient = null
    }
}