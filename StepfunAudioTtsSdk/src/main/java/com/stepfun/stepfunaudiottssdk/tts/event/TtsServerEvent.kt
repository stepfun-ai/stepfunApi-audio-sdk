package com.stepfun.stepfunaudiottssdk.tts.event

import com.google.gson.annotations.SerializedName

sealed class TtsServerEvent(val eventId: String, val type: String)

//连接成功
data class TtsConnectionDoneEvent(
    @SerializedName("event_id") val eventIdValue: String,
    val data: ConnectionData
) : TtsServerEvent(eventIdValue, "tts.connection.done") {
    data class ConnectionData(
        @SerializedName("session_id") val sessionId: String
    )
}

//会话创建成功
data class TtsResponseCreatedEvent(
    @SerializedName("event_id") val eventIdValue: String,
    val data: SessionData
) : TtsServerEvent(eventIdValue, "tts.response.created") {
    data class SessionData(
        @SerializedName("session_id") val sessionId: String
    )
}

//开始生成单句 tts.response.sentence.start
data class TtsResponseSentenceStartEvent(
    @SerializedName("event_id") val eventIdValue: String,
    val data: SentenceStartData
) : TtsServerEvent(eventIdValue, "tts.response.sentence.start") {
    data class SentenceStartData(
        @SerializedName("sentence_id") val sentenceId: String,
        @SerializedName("text") val text: String,
        @SerializedName("started_at") val started: Long,
    )
}

//接收生成好的音频 tts.response.audio.delta
data class TtsResponseAudioDeltaEvent(
    @SerializedName("event_id") val eventIdValue: String,
    val data: AudioDeltaData
) : TtsServerEvent(eventIdValue, "tts.response.audio.delta") {
    data class AudioDeltaData(
        @SerializedName("session_id") val sessionId: String,
        @SerializedName("status") val status: String,
        @SerializedName("audio") val audio: String, // base64 音频数据片段
        @SerializedName("duration") val duration: Float,
    )
}

// 结束生成单句 tts.response.sentence.end
data class TtsResponseSentenceEndEvent(
    @SerializedName("event_id") val eventIdValue: String,
    val data: SentenceEndData
) : TtsServerEvent(eventIdValue, "tts.response.sentence.end") {
    data class SentenceEndData(
        @SerializedName("sentence_id") val sentenceId: String,
        @SerializedName("text") val text: String,
        @SerializedName("ended_at") val endedAt: Long,
    )
}

/**
 * 清空缓存完成
 */
data class TtsTextFlushedEvent(
    @SerializedName("event_id") val eventIdValue: String,
    val data: SessionData
) : TtsServerEvent(eventIdValue, "tts.text.flushed") {
    data class SessionData(
        @SerializedName("session_id") val sessionId: String
    )
}

/**
 * 完成本次生成
 */
data class TtsResponseAudioDoneEvent(
    @SerializedName("event_id") val eventIdValue: String,
    val data: AudioDoneData
) : TtsServerEvent(eventIdValue, "tts.response.audio.done") {
    data class AudioDoneData(
        @SerializedName("session_id") val sessionId: String,
        val audio: String  // 完整音频BASE64
    )
}

/**
 * 错误事件
 */
data class TtsResponseErrorEvent(
    @SerializedName("event_id") val eventIdValue: String,
    val data: ErrorData
) : TtsServerEvent(eventIdValue, "tts.response.error") {
    data class ErrorData(
        @SerializedName("session_id") val sessionId: String,
        val code: String,
        val message: String,
        val details: Map<String, Any>?
    )
}