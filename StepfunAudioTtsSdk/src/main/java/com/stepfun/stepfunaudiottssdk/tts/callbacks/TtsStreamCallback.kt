package com.stepfun.stepfunaudiottssdk.tts.callbacks

interface TtsStreamCallback {
    // 连接创建成功
    fun onConnectionEstablished(sessionId: String)

    // 会话创建成功
    fun onSessionCreated(sessionId: String)

    // 单句生成开始
    fun onSentenceStart(text: String)

    /**
     * 接收到音频数据
     * @param audioData 解码后的音频数据
     * @param isFinished 是否为最后一段
     */
    fun onAudioData(audioData: ByteArray, isFinished: Boolean)

    // 单句生成结束
    fun onSentenceEnd(text: String)

    //缓存区已清空
    fun onFlushed()

    // 生成完成
    fun onComplete()

    // 发生错误
    fun onError(error: TtsStreamError)


}

data class TtsStreamError(
    val code: Int,
    val message: String,
    val details: Map<String, Any>? = null
)
