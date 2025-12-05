package com.stepfun.stepfunaudiottssdk.tts

import android.content.Context
import com.stepfun.stepfunaudiocoresdk.audio.common.audio.AudioPlayer
import com.stepfun.stepfunaudiocoresdk.audio.common.audio.AudioStreamPlayerV2
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsVoice
import com.stepfun.stepfunaudiottssdk.tts.callbacks.TtsStreamCallback
import com.stepfun.stepfunaudiottssdk.tts.callbacks.TtsStreamError
import com.stepfun.stepfunaudiottssdk.tts.configs.TtsStreamParams
import com.stepfun.stepfunaudiocoresdk.audio.core.SpeechCoreSdk
import com.stepfun.stepfunaudiottssdk.tts.configs.TtsSpeechParams
import java.io.File

object Tts {

    private var ttsClient: TtsClient? = null
    private var audioPlayer: AudioPlayer? = null
    private var ttsStreamClient: TtsStreamClient? = null
    private var audioStreamPlayer: AudioStreamPlayerV2? = null

    private fun getClient(): TtsClient {
        if (!SpeechCoreSdk.isInitialized()) {
            throw IllegalStateException("SpeechCoreSdk not initialized. Call SpeechCoreSdk.init() first.")
        }

        if (ttsClient == null) {
            ttsClient = TtsClient()
        }
        return ttsClient!!
    }

    /**
     * 获取或创建音频播放器实例
     */
    private fun getAudioPlayer(context: Context): AudioPlayer {
        if (audioPlayer == null) {
            audioPlayer = AudioPlayer(context.applicationContext)
        }
        return audioPlayer!!
    }

    /**
     * 生成语音（使用默认配置）
     */
    fun generateSpeech(
        text: String,
        voice: TtsVoice,
        callback: TtsCallback
    ) {
        val config = SpeechCoreSdk.getConfig().ttsConfig

        val params = TtsSpeechParams.Builder()
            .model(config.defaultModel)
            .input(text)
            .voice(voice)
            .responseFormat(config.defaultResponseFormat)
            .speed(config.defaultSpeed)
            .volume(config.defaultVolume)
            .sampleRate(config.defaultSampleRate)
            .build()

        getClient().generateSpeech(params, callback)
    }

    /**
     * 生成语音（使用自定义参数）
     */
    fun generateSpeech(params: TtsSpeechParams, callback: TtsCallback) {
        getClient().generateSpeech(params, callback)
    }
    /**
     * 生成语音（协程异步方式）
     */

    suspend fun generateSpeechAsync(params: TtsSpeechParams): ByteArray {
        return getClient().generateSpeechAsync(params)
    }

    /**
     * 生成语音并保存到文件
     */
    fun generateSpeechToFile(
        params: TtsSpeechParams,
        outputFile: File,
        callback: TtsCallback
    ) {
        getClient().generateSpeechToFile(params, outputFile, callback)
    }

    /**
     * 生成语音并播放（便捷方法）
     * 
     * 这是一个便捷方法，自动完成"生成 → 播放"的流程
     * 
     * @param context 上下文
     * @param params TTS参数
     * @param callback TTS生成回调
     * @param playbackCallback 播放回调（可选）
     */
    fun generateAndPlay(
        context: Context,
        params: TtsSpeechParams,
        callback: TtsCallback? = null,
        playbackCallback: AudioPlayer.PlaybackCallback? = null
    ) {
        getClient().generateSpeech(params, object : TtsCallback {
            override fun onSuccess(audioData: ByteArray) {
                // 通知 TTS 生成成功
                callback?.onSuccess(audioData)
                
                // 自动播放
                val format = params.responseFormat?.format ?: "mp3"
                getAudioPlayer(context).playFromByteArray(
                    audioData = audioData,
                    fileExtension = format,
                    callback = playbackCallback
                )
            }

            override fun onError(error: TtsError) {
                callback?.onError(error)
            }
        })
    }

    /**
     * 快速生成并播放（使用默认配置）
     * 
     * 最简单的使用方式，适合快速测试和简单场景
     * 
     * @param context 上下文
     * @param text 要合成的文本
     * @param voice 音色
     * @param callback TTS生成回调（可选）
     * @param playbackCallback 播放回调（可选）
     */
    fun quickPlay(
        context: Context,
        text: String,
        voice: TtsVoice,
        callback: TtsCallback? = null,
        playbackCallback: AudioPlayer.PlaybackCallback? = null
    ) {
        val config = SpeechCoreSdk.getConfig().ttsConfig
        
        val params = TtsSpeechParams.Builder()
            .model(config.defaultModel)
            .input(text)
            .voice(voice)
            .responseFormat(config.defaultResponseFormat)
            .speed(config.defaultSpeed)
            .volume(config.defaultVolume)
            .sampleRate(config.defaultSampleRate)
            .build()

        generateAndPlay(context, params, callback, playbackCallback)
    }

    /**
     * 停止当前播放
     */
    fun stopPlayback() {
        audioPlayer?.stop()
    }

    /**
     * 取消所有TTS任务
     */
    fun cancelAll() {
        ttsClient?.cancelAll()
    }

    /**
     * 释放所有资源
     * 包括 TTS 客户端和音频播放器
     */
    fun release() {
        ttsClient?.release()
        ttsClient = null
        
        audioPlayer?.release()
        audioPlayer = null

        ttsStreamClient?.release()
        ttsStreamClient = null

        audioStreamPlayer?.release()
        audioStreamPlayer = null
    }


    /**
     * 创建流式TTS会话
     */
    fun createStreamSession(
        context: Context,
        params: TtsStreamParams,
        callback: TtsStreamCallback
    ) {
        // 释放旧的流式客户端
        ttsStreamClient?.release()
        audioStreamPlayer?.release()

        // 创建新的客户端和播放器
        ttsStreamClient = TtsStreamClient()
        audioStreamPlayer = AudioStreamPlayerV2(context.applicationContext).apply {
            initialize(params.sampleRate, params.responseFormat)
        }

        // 连接并创建会话
        ttsStreamClient?.connect(params, object : TtsStreamCallback {
            override fun onConnectionEstablished(sessionId: String) {
                callback.onConnectionEstablished(sessionId)
            }

            override fun onSessionCreated(sessionId: String) {
                callback.onSessionCreated(sessionId)
            }

            override fun onSentenceStart(text: String) {
                callback.onSentenceStart(text)
            }

            override fun onAudioData(audioData: ByteArray, isFinished: Boolean) {
                // 添加到播放队列
                audioStreamPlayer?.addAudioData(audioData)
                callback.onAudioData(audioData, isFinished)
            }

            override fun onSentenceEnd(text: String) {
                callback.onSentenceEnd(text)
            }

            override fun onFlushed() {
                callback.onFlushed()
            }

            override fun onComplete() {
                callback.onComplete()
            }

            override fun onError(error: TtsStreamError) {
                callback.onError(error)
            }
        })
    }

    /**
     * 发送文本到流式会话
     */
    fun sendStreamText(text: String) {
        ttsStreamClient?.sendText(text)
    }

    /**
     * 清空流式缓冲区
     */
    fun flushStream() {
        ttsStreamClient?.flush()
    }

    /**
     * 完成流式生成
     */
    fun finishStream() {
        ttsStreamClient?.done()
    }

    /**
     * 停止流式播放
     */
    fun stopStream() {
        audioStreamPlayer?.stop()
    }

    /**
     * 暂停流式播放
     */
    fun pauseStream() {
        audioStreamPlayer?.pause()
    }

    /**
     * 恢复流式播放
     */
    fun resumeStream() {
        audioStreamPlayer?.resume()
    }
}