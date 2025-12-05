package com.stepfun.stepfunaudiottssdk.tts.event

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.stepfun.stepfunaudiocoresdk.audio.common.config.PronunciationMap
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsSampleRate

sealed class TtsClientEvent(val type: String) {
    abstract fun toJson(): String
}

// 创建会话事件
data class TtsCreateEvent(val data: CreateData) : TtsClientEvent("tts.create") {
    data class CreateData(
        @SerializedName("session_id") val sessionId: String,
        @SerializedName("voice_id") val voiceId: String,
        @SerializedName("response_format") val responseFormat: String? = "mp3",
        @SerializedName("sample_rate") val sampleRate: Int? = TtsSampleRate.RATE_24000.rate,
        @SerializedName("volume_ratio") val volumeRatio: Float = 1.0f,
        @SerializedName("speed_ratio") val speedRatio: Float = 1.0f,
        @SerializedName("pronunciation_map") val pronunciationMap: List<PronunciationMap>? = null,
        @SerializedName("features") val features: Features? = null,
    )

    data class Features(
        @SerializedName("enable_markdown_filter") val enableMarkdownFilter: Boolean? = false,
    )

    override fun toJson(): String {
        return Gson().toJson(mapOf("type" to type, "data" to data))
    }
}

// 生成音频
/**
 *  {
 * 	"type": "tts.text.delta",
 * 	"data": {
 * 		"session_id": "01956e7388477cfcbdc3aaabf364bc70",
 * 		"text": "今天的天气真不错，我想去学习阶跃星辰的大模型技术"
 * 	}
 * }
 */

data class TtsTextDeltaEvent(val data: TextData) : TtsClientEvent("tts.text.delta") {
    data class TextData(
        @SerializedName("text") val text: String,
        @SerializedName("session_id") val sessionId: String

    )

    override fun toJson(): String {
        return Gson().toJson(
            mapOf("type" to type, "data" to data)
        )
    }
}

// 清空缓冲区
data class TtsTextFlushEvent(val data: FlushData) : TtsClientEvent("tts.text.flush") {
    data class FlushData(
        @SerializedName("session_id") val sessionId: String
    )

    override fun toJson(): String {
        return Gson().toJson(
            mapOf("type" to type, "data" to data)
        )
    }
}

data class TtsTextDoneEvent(val data: DoneData) : TtsClientEvent("tts.text.done") {
    data class DoneData(
        @SerializedName("session_id") val sessionId: String
    )

    override fun toJson(): String {
        return Gson().toJson(
            mapOf("type" to type, "data" to data)
        )
    }
}
