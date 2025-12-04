package com.stepfun.stepfunaudiocoresdk.audio.core

import com.stepfun.stepfunaudiocoresdk.audio.common.config.AsrConfig
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsConfig

data class SpeechConfig(
    // 基础配置
//    val apiKey: String = "",

    // 环境配置
    val environment: Environment = Environment.PRODUCTION,
    val httpBaseUrl: String = "",
    val webSocketUrl: String = "",

    // 日志配置
    val enableFileLog: Boolean = false,
    val enableLogging: Boolean = false,

    //请求头
    val customHeaders: Map<String, String> = emptyMap(),

    // 网络配置
    val connectTimeout: Long = 30,
    val readTimeout: Long = 30,
    val writeTimeout: Long = 60,

    // TTS配置
    val ttsConfig: TtsConfig = TtsConfig(),
    // ASR配置
    val asrConfig: AsrConfig = AsrConfig(),
) {
    class Builder {
        //        private var apiKey: String = ""
        private var apiSecret: String = ""
        private var appId: String = ""
        private var httpBaseUel: String = ""
        private var webSocketUrl: String = ""
        private var environment: Environment = Environment.PRODUCTION
        private var enableFileLog: Boolean = false
        private var enableLogging: Boolean = false
        private var customHeaders : Map<String, String> = emptyMap()
        private var connectTimeout: Long = 30
        private var readTimeout: Long = 30
        private var writeTimeout: Long = 60
        private var ttsConfig: TtsConfig = TtsConfig()
        private var asrConfig: AsrConfig = AsrConfig()

//        fun apiKey(key: String) = apply {
//            require(key.isNotEmpty()) {
//                "API Key cannot be empty"
//            }
//            this.apiKey = key
//        }

        fun httpBaseUrl(url: String) = apply {
            require(url.isNotEmpty()) {
                "http Url cannot be empty"
            }
            this.httpBaseUel = url
        }

        fun webSocketUrl(url: String) = apply {
            require(url.isNotEmpty()) {
                "websocket Url cannot be empty"
            }
            this.webSocketUrl = url
        }

        fun environment(env: Environment) = apply { this.environment = env }
        fun enableLogging(enable: Boolean) = apply { this.enableLogging = enable }
        fun ttsConfig(config: TtsConfig) = apply { this.ttsConfig = config }
        fun asrConfig(config: AsrConfig) = apply { this.asrConfig = config }

        fun customHeaders(headers: Map<String, String>) = apply { this.customHeaders = headers }

        fun build() = SpeechConfig(
//            apiKey = apiKey,
            httpBaseUrl = httpBaseUel,
            webSocketUrl = webSocketUrl,
            environment = environment,
            customHeaders = customHeaders,
            enableFileLog = enableFileLog,
            enableLogging = enableLogging,
            connectTimeout = connectTimeout,
            readTimeout = readTimeout,
            writeTimeout = writeTimeout,
            ttsConfig = ttsConfig,
            asrConfig = asrConfig
        )
    }
}

enum class Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}