package stepai.android.audio.core

import android.content.Context
import stepai.android.audio.common.logger.LoggerManager
import com.stepfun.stepfunaudiocoresdk.audio.common.network.HttpClient
import com.stepfun.stepfunaudiocoresdk.audio.common.network.WebSocketClient

object SpeechCoreSdk {
    private lateinit var speechConfig: SpeechConfig
    private var isInitialized: Boolean = false

    fun init(context: Context, config: SpeechConfig) {
        if (isInitialized) {
            return
        }
        LoggerManager.setEnableLogger(config.enableLogging)
        HttpClient.init(context, config)
        WebSocketClient.init(context, config)
        this.speechConfig = config
        isInitialized = true
    }

    fun isInitialized(): Boolean {
        return isInitialized
    }

    fun updateConfig(config: SpeechConfig) {
        if (isInitialized.not()) {
            throw IllegalStateException("SpeechSdk not initialized. Call init() first.")
        }
        this.speechConfig = config
    }

    fun getConfig(): SpeechConfig {
        return speechConfig
    }
}