package com.stepfun.stepfunaudiottssdk.tts

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.stepfun.stepfunaudiocoresdk.audio.common.config.PronunciationMap
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.COMMON_TAG
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logD
import com.stepfun.stepfunaudiocoresdk.audio.common.network.WebSocketClient
import com.stepfun.stepfunaudiottssdk.tts.callbacks.TtsStreamCallback
import com.stepfun.stepfunaudiottssdk.tts.callbacks.TtsStreamError
import com.stepfun.stepfunaudiottssdk.tts.configs.TtsStreamParams
import com.stepfun.stepfunaudiottssdk.tts.event.TtsClientEvent
import com.stepfun.stepfunaudiottssdk.tts.event.TtsConnectionDoneEvent
import com.stepfun.stepfunaudiottssdk.tts.event.TtsCreateEvent
import com.stepfun.stepfunaudiottssdk.tts.event.TtsResponseAudioDeltaEvent
import com.stepfun.stepfunaudiottssdk.tts.event.TtsResponseCreatedEvent
import com.stepfun.stepfunaudiottssdk.tts.event.TtsResponseErrorEvent
import com.stepfun.stepfunaudiottssdk.tts.event.TtsResponseSentenceEndEvent
import com.stepfun.stepfunaudiottssdk.tts.event.TtsResponseSentenceStartEvent
import com.stepfun.stepfunaudiottssdk.tts.event.TtsTextDeltaEvent
import com.stepfun.stepfunaudiottssdk.tts.event.TtsTextDoneEvent
import com.stepfun.stepfunaudiottssdk.tts.event.TtsTextFlushEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import com.stepfun.stepfunaudiocoresdk.audio.core.SpeechCoreSdk

class TtsStreamClient {
    companion object {
        private const val TAG = COMMON_TAG + "TtsStreamClient"
        private const val IDLE_TIMEOUT = 60L // 60秒
    }


    private var callback: TtsStreamCallback? = null
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val gson = Gson()
    private var sessionId: String? = null
    private var isAudioDoneReceived = false  // 标记是否已收到 audio.done 事件
    private var isDoneEventSent = false       // 标记是否已发送 done 事件
    private var connectTime: Long = 0

    /**
     * 连接WebSocket并创建会话
     */
    fun connect(params: TtsStreamParams, callback: TtsStreamCallback) {
        if (!SpeechCoreSdk.isInitialized()) {
            callback.onError(
                TtsStreamError(
                    code = TtsError.ERROR_NOT_INITIALIZED,
                    message = "SpeechCoreSdk 没有初始化"
                )
            )
            return
        }

        // 重置状态标记
        isAudioDoneReceived = false
        isDoneEventSent = false
        connectTime = System.currentTimeMillis()

        this.callback = callback
        val wsUrl = SpeechCoreSdk.getConfig().webSocketUrl
        val url = "$wsUrl?model=${params.model.modelId}"
        "Connecting to WebSocket URL: $url".logD(TAG)

        webSocket = WebSocketClient.newWebSocket(url, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                "WebSocket 连接成功".logD(TAG)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                handleServerEvent(text, params)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)

                val duration = System.currentTimeMillis() - connectTime
                "WebSocket 连接异常断开 (耗时${duration}ms). Exception: ${t.javaClass.simpleName}, Msg: ${t.message}".logD(TAG)
                if (response != null) {
                    "WebSocket Response: code=${response.code}, message=${response.message}".logD(TAG)
                }

                // 判断是否是正常的空闲超时断开
                val isIdleTimeout = isIdleTimeoutDisconnection(t, response, duration)

                if (isIdleTimeout) {
                    // 60秒空闲超时，这是正常行为
                    "WebSocket 空闲超时断开，这是正常行为".logD(TAG)
                    scope.launch {
                        callback.onComplete()
                    }
                } else {
                    // 真正的连接错误
                    "WebSocket 连接失败: ${t.message}".logD(TAG)
                    scope.launch {
                        callback.onError(
                            TtsStreamError(
                                code = TtsError.ERROR_WEBSOCKET,
                                message = t.message ?: "WebSocket connection failed (${t.javaClass.simpleName})"
                            )
                        )
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                "WebSocket 已关闭: code=$code, reason=$reason".logD(TAG)
                scope.launch {
                    callback.onComplete()
                }
            }
        })

    }

    private fun handleServerEvent(message: String, params: TtsStreamParams) {
        try {
            "收到消息：${message}".logD(TAG)
            val jsonObject = gson.fromJson(message, JsonObject::class.java)
            val type = jsonObject.get("type")?.asString
            if (type == null) {
                "消息格式错误，缺少 type 字段".logD(TAG)
                return
            }

            // 关键修复：在 IO 线程同步设置状态标记，避免 onFailure 回调时的竞态条件
            when (type) {
                "tts.response.audio.done" -> {
                    isAudioDoneReceived = true
                    "已标记 audio.done 接收完成".logD(TAG)
                }
            }

            scope.launch {
                when (type) {
                    "tts.connection.done" -> {
                        val event = gson.fromJson(message, TtsConnectionDoneEvent::class.java)
                        sessionId = event.data.sessionId
                        callback?.onConnectionEstablished(event.data.sessionId)

                        // 自动发送创建会话事件
                        sendCreateEvent(event.data.sessionId, params)
                    }

                    "tts.response.created" -> {
                        val event = gson.fromJson(message, TtsResponseCreatedEvent::class.java)
                        callback?.onSessionCreated(event.data.sessionId)
                    }

                    "tts.response.sentence.start" -> {
                        val event =
                            gson.fromJson(message, TtsResponseSentenceStartEvent::class.java)
                        callback?.onSentenceStart(event.data.text)
                    }

                    "tts.response.audio.delta" -> {
                        val event = gson.fromJson(message, TtsResponseAudioDeltaEvent::class.java)
                        val audioData = Base64.decode(event.data.audio, Base64.DEFAULT)
                        val isFinished = event.data.status == "finished"
                        callback?.onAudioData(audioData, isFinished)
                    }

                    "tts.response.sentence.end" -> {
                        val event = gson.fromJson(message, TtsResponseSentenceEndEvent::class.java)
                        callback?.onSentenceEnd(event.data.text)
                    }

                    "tts.text.flushed" -> {
                        callback?.onFlushed()
                    }

                    "tts.response.audio.done" -> {
                        // 状态已在上面同步设置
                        callback?.onComplete()
                        close()
                    }

                    "tts.response.error" -> {
                        val event = gson.fromJson(message, TtsResponseErrorEvent::class.java)
                        callback?.onError(
                            TtsStreamError(
                                event.data.code.toInt(),
                                event.data.message,
                                event.data.details
                            )
                        )
                        close()
                    }
                }
            }


        } catch (e: Exception) {

        }
    }

    private fun sendCreateEvent(sessionId: String, params: TtsStreamParams) {
        val event = TtsCreateEvent(
            data = TtsCreateEvent.CreateData(
                sessionId = sessionId,
                voiceId = params.voice.voiceId,
                responseFormat = params.responseFormat.format,
                sampleRate = params.sampleRate,
                volumeRatio = params.volumeRatio,
                speedRatio = params.speedRatio,
                pronunciationMap = params.pronunciationMap?.map {
                    PronunciationMap(it.tone)
                }
            )
        )
        sendEvent(event)
    }

    /**
     * 发送文本
     */
    fun sendText(text: String) {
        val sid = sessionId ?: run {
            callback?.onError(TtsStreamError(TtsError.ERROR_NO_SESSION, "Session not created"))
            return
        }

        val event = TtsTextDeltaEvent(
            data = TtsTextDeltaEvent.TextData(
                sessionId = sid,
                text = text
            )
        )
        sendEvent(event)
    }

    /**
     * 清空缓冲区
     */
    fun flush() {
        val sid = sessionId ?: return
        val event = TtsTextFlushEvent(TtsTextFlushEvent.FlushData(sid))
        sendEvent(event)
    }

    /**
     * 完成生成
     */
    fun done() {
        val sid = sessionId ?: return
        isDoneEventSent = true
        val event = TtsTextDoneEvent(TtsTextDoneEvent.DoneData(sid))
        sendEvent(event)
    }

    /**
     * 发送事件
     */
    private fun sendEvent(event: TtsClientEvent) {
        val json = event.toJson()
        "发送事件: $json".logD(TAG)
        webSocket?.send(json)
    }

    /**
     * 判断是否是空闲超时断开
     *
     * 根据 API 文档：如果连续 60 秒无动作，系统会自动断开连接
     * 这种情况下应该视为正常完成，而不是错误
     */
    /**
     * 判断是否是正常断开（非错误情况）
     *
     * 根据 API 文档：如果连续 60 秒无动作，系统会自动断开连接
     * 这种情况下应该视为正常完成，而不是错误
     */
    private fun isIdleTimeoutDisconnection(t: Throwable, _response: Response?, _duration: Long): Boolean {
        // 1. 如果已经收到 audio.done 事件，说明正常完成了
        if (isAudioDoneReceived) {
            "判定为正常断开：已收到 audio.done 事件".logD(TAG)
            return true
        }

        // 2. 如果已经发送了 done 事件，后续断开也是正常的
        if (isDoneEventSent) {
            "判定为正常断开：已发送 done 事件".logD(TAG)
            return true
        }

        // 3. 检查异常类型，判断是否是超时或正常 EOF
        val exceptionClassName = t.javaClass.simpleName
        val isTimeoutException = exceptionClassName.contains("Timeout", ignoreCase = true) ||
                exceptionClassName.contains("EOF", ignoreCase = true) ||
                exceptionClassName.contains("SocketException", ignoreCase = true)

        // 4. 如果异常消息为空或包含 "Socket closed"，通常是正常关闭
        val message = t.message ?: ""
        val isNormalClosure = message.isEmpty() ||
                message.contains("Socket closed", ignoreCase = true) ||
                message.contains("Connection closed", ignoreCase = true)

        if (isTimeoutException || isNormalClosure) {
            "判定为正常断开：异常类型=$exceptionClassName, 消息=$message".logD(TAG)
        }

        return isTimeoutException || isNormalClosure
    }

    /**
     * 关闭连接
     */
    fun close() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        sessionId = null
    }

    /**
     * 释放资源
     */
    fun release() {
        close()
        scope.cancel()
        callback = null
    }
}