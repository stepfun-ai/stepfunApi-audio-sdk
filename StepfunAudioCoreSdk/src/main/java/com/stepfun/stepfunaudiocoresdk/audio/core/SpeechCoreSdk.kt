package com.stepfun.stepfunaudiocoresdk.audio.core

import android.content.Context
import com.stepfun.stepfunaudiocoresdk.audio.common.network.HttpClient
import com.stepfun.stepfunaudiocoresdk.audio.common.network.WebSocketClient

object SpeechCoreSdk {
    private lateinit var speechConfig: SpeechConfig
    private var isInitialized: Boolean = false

    fun init(context: Context, config: SpeechConfig) {
        if (isInitialized) {
            return
        }
        HttpClient.init(context, config)
        WebSocketClient.init(context, config)
        speechConfig = config
        isInitialized = true
    }

    fun isInitialized(): Boolean {
        return isInitialized
    }

    fun updateConfig(config: SpeechConfig) {
        if (isInitialized.not()) {
            throw IllegalStateException("SpeechSdk not initialized. Call init() first.")
        }
        speechConfig = config
    }

    fun getConfig(): SpeechConfig {
        return speechConfig
    }
}