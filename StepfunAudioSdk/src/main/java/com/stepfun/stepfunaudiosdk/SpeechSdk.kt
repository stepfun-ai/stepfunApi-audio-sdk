package com.stepfun.stepfunaudiosdk

import android.content.Context
import com.stepfun.stepfunaudioasrsdk.asr.Asr
import com.stepfun.stepfunaudioasrsdk.asr.AsrCallback
import com.stepfun.stepfunaudioasrsdk.asr.AsrError
import com.stepfun.stepfunaudioasrsdk.asr.AsrError.Companion.ERROR_UNKNOWN
import com.stepfun.stepfunaudioasrsdk.asr.AsrParams
import com.stepfun.stepfunaudiocoresdk.audio.common.audio.AudioPlayer
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsVoice
import com.stepfun.stepfunaudiottssdk.tts.Tts
import com.stepfun.stepfunaudiottssdk.tts.TtsCallback
import com.stepfun.stepfunaudiottssdk.tts.callbacks.TtsStreamCallback
import com.stepfun.stepfunaudiottssdk.tts.configs.TtsStreamParams
import stepai.android.audio.common.audio.AudioRecordFormat
import stepai.android.audio.common.audio.AudioRecorder
import stepai.android.audio.common.audio.RecordingCallback
import com.stepfun.stepfunaudiocoresdk.audio.core.SpeechConfig
import com.stepfun.stepfunaudiocoresdk.audio.core.SpeechCoreSdk
import com.stepfun.stepfunaudiottssdk.tts.configs.TtsSpeechParams
import java.io.File

// Kotlin 源文件：speech_sdk

object SpeechSdk {
    /**
     * 初始化SDK
     */
    fun init(context: Context, config: SpeechConfig) {
        SpeechCoreSdk.init(context, config)
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean {
        return SpeechCoreSdk.isInitialized()
    }

    /**
     * 更新配置
     */
    fun updateConfig(config: SpeechConfig) {
        SpeechCoreSdk.updateConfig(config)
    }

    /**
     * 获取配置
     */
    fun getConfig(): SpeechConfig {
        return SpeechCoreSdk.getConfig()
    }

    /**
     * TTS相关方法
     */
    object TTS {

        // ========== 基础方法：生成音频 ==========

        /**
         * 生成语音（使用默认配置）
         */
        fun generateSpeech(text: String, voice: TtsVoice, callback: TtsCallback) {
            Tts.generateSpeech(text, voice, callback)
        }

        /**
         * 生成语音（使用自定义参数）
         */
        fun generateSpeech(params: TtsSpeechParams, callback: TtsCallback) {
            Tts.generateSpeech(params, callback)
        }

        /**
         * 生成语音（协程方式）
         */
        suspend fun generateSpeechAsync(params: TtsSpeechParams): ByteArray {
            return Tts.generateSpeechAsync(params)
        }

        /**
         * 生成语音并保存到文件
         */
        fun generateSpeechToFile(
            params: TtsSpeechParams,
            outputFile: File,
            callback: TtsCallback
        ) {
            Tts.generateSpeechToFile(params, outputFile, callback)
        }

        // ========== 便捷方法：生成并播放 ==========

        /**
         * 生成语音并播放（一体化方式）
         *
         * 自动完成"生成 → 播放"流程，适合大多数场景
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
            Tts.generateAndPlay(context, params, callback, playbackCallback)
        }

        /**
         * 快速生成并播放（最简单方式）
         *
         * 使用默认配置，一行代码搞定
         *
         * @param context 上下文
         * @param text 要合成的文本
         * @param voice 音色（默认 "default"）
         * @param callback TTS生成回调（可选）
         * @param playbackCallback 播放回调（可选）
         */
        fun quickPlay(
            context: Context,
            text: String,
            voice: TtsVoice = TtsVoice.STEP_TTS_MINI_DEFAULT,
            callback: TtsCallback? = null,
            playbackCallback: AudioPlayer.PlaybackCallback? = null
        ) {
            Tts.quickPlay(context, text, voice, callback, playbackCallback)
        }

        // ========== 控制方法 ==========

        /**
         * 停止当前播放
         */
        fun stopPlayback() {
            Tts.stopPlayback()
        }

        /**
         * 取消所有TTS任务
         */
        fun cancelAll() {
            Tts.cancelAll()
        }

        // ========== 流式 TTS 方法 ==========

        /**
         * 创建流式 TTS 会话
         *
         * 流式 TTS 适用于需要实时生成和播放语音的场景，
         * 可以边发送文本边接收音频，降低延迟
         *
         * @param context 上下文
         * @param params 流式 TTS 参数
         * @param callback 流式回调
         *
         * 示例：
         * ```
         * val params = TtsStreamParams.Builder()
         *     .model(TtsModel.STEP_TTS_MINI)
         *     .voice(TtsVoice.STEP_TTS_MINI_DEFAULT)
         *     .sampleRate(22050)
         *     .build()
         *
         * SpeechSdk.TTS.createStreamSession(context, params, object : TtsStreamCallback {
         *     override fun onSessionCreated(sessionId: String) {
         *         // 会话创建成功，可以开始发送文本
         *         SpeechSdk.TTS.sendStreamText("你好，世界")
         *     }
         *
         *     override fun onAudioData(audioData: ByteArray, isFinished: Boolean) {
         *         // 接收音频数据（自动播放）
         *     }
         *
         *     override fun onComplete() {
         *         // 生成完成
         *     }
         *
         *     override fun onError(error: TtsStreamError) {
         *         // 处理错误
         *     }
         * })
         * ```
         */
        fun createStreamSession(
            context: Context,
            params: TtsStreamParams,
            callback: TtsStreamCallback
        ) {
            Tts.createStreamSession(context, params, callback)
        }

        /**
         * 创建流式 TTS 会话（使用默认配置）
         *
         * 便捷方法，使用默认配置快速创建流式会话
         *
         * @param context 上下文
         * @param voice 音色
         * @param callback 流式回调
         */
        fun createStreamSession(
            context: Context,
            voice: TtsVoice = TtsVoice.STEP_TTS_MINI_DEFAULT,
            callback: TtsStreamCallback
        ) {
            val config = SpeechCoreSdk.getConfig().ttsConfig

            val params = TtsStreamParams.Builder()
                .model(config.defaultModel)
                .voice(voice)
                .responseFormat(config.defaultResponseFormat)
                .sampleRate(config.defaultSampleRate)
                .speedRatio(config.defaultSpeed)
                .volumeRatio(config.defaultVolume)
                .build()

            Tts.createStreamSession(context, params, callback)
        }

        /**
         * 发送文本到流式会话
         *
         * 在会话创建成功后（onSessionCreated 回调）调用此方法发送文本
         * 可以多次调用，支持分段发送
         *
         * @param text 要合成的文本
         *
         * 注意：需要先调用 createStreamSession 创建会话
         */
        fun sendStreamText(text: String) {
            Tts.sendStreamText(text)
        }

        /**
         * 清空流式缓冲区
         *
         * 快速清空缓冲区，一次性获得当前尚未返回的音频内容
         * 适用于需要快速完成当前句子生成的场景
         */
        fun flushStream() {
            Tts.flushStream()
        }

        /**
         * 完成流式生成
         *
         * 通知服务端不再发送文本，等待当前所有音频生成完成
         * 调用后会自动断开连接
         *
         * 注意：调用此方法后不能再发送文本
         */
        fun finishStream() {
            Tts.finishStream()
        }

        /**
         * 停止流式播放
         *
         * 立即停止音频播放，但不关闭 WebSocket 连接
         * 适用于需要暂停播放但保留会话的场景
         */
        fun stopStream() {
            Tts.stopStream()
        }
    }

    object ASR {
        /**
         * 识别音频文件（使用默认配置）
         *
         * @param file 音频文件
         * @param callback 识别结果回调
         *
         * 示例：
         * ```
         * val audioFile = File("/path/to/audio.mp3")
         * SpeechSdk.ASR.transcribe(audioFile, object : AsrCallback {
         *     override fun onSuccess(result: AsrResult) {
         *         Log.d("ASR", "识别结果: ${result.text}")
         *     }
         *
         *     override fun onError(error: AsrError) {
         *         Log.e("ASR", "识别失败: ${error.message}")
         *     }
         * })
         * ```
         */
        fun transcribe(file: File, callback: AsrCallback) {
            Asr.transcribe(file, callback)
        }

        /**
         * 识别音频文件（使用自定义参数）
         *
         * @param params ASR参数
         * @param callback 识别结果回调
         *
         * 示例：
         * ```
         * val params = AsrParams.Builder()
         *     .file(audioFile)
         *     .responseFormat(AsrResponseFormat.JSON)
         *     .hotwords(listOf("阶跃星辰", "人工智能"))
         *     .build()
         *
         * SpeechSdk.ASR.transcribe(params, callback)
         * ```
         */
        fun transcribe(params: AsrParams, callback: AsrCallback) {
            Asr.transcribe(params, callback)
        }

        /**
         * 识别音频文件（协程方式）
         *
         * @param params ASR参数
         * @return 识别结果
         *
         * 示例：
         * ```
         * lifecycleScope.launch {
         *     try {
         *         val result = SpeechSdk.ASR.transcribeAsync(params)
         *         Log.d("ASR", "识别结果: ${result.text}")
         *     } catch (e: Exception) {
         *         Log.e("ASR", "识别失败", e)
         *     }
         * }
         * ```
         */
        suspend fun transcribeAsync(params: AsrParams) = Asr.transcribeAsync(params)

        /**
         * 快速识别（使用默认配置）
         *
         * 最简单的使用方式，适合快速测试和简单场景
         *
         * @param file 音频文件
         * @param hotwords 热词列表（可选）
         * @param callback 识别结果回调
         *
         * 示例：
         * ```
         * SpeechSdk.ASR.quickTranscribe(
         *     file = audioFile,
         *     hotwords = listOf("专业术语"),
         *     callback = callback
         * )
         * ```
         */
        fun quickTranscribe(
            file: File,
            hotwords: List<String>? = null,
            callback: AsrCallback
        ) {
            Asr.quickTranscribe(file, hotwords, callback)
        }

        /**
         * 开始录音并准备识别
         *
         * 这是一个便捷方法，自动完成"录音 → 识别"的流程
         *
         * @param context 上下文
         * @param outputFile 输出文件（可选），如果为null则自动创建临时文件
         * @param format 音频格式，默认 MP3
         * @param sampleRate 采样率，默认 16000Hz（适合语音识别）
         * @param recordingCallback 录音回调
         * @return AudioRecorder实例，用于控制录音
         *
         * 示例：
         * ```
         * val recorder = SpeechSdk.ASR.startRecording(
         *     context = this,
         *     recordingCallback = object : AudioRecorder.RecordingCallback {
         *         override fun onStart() {
         *             Log.d("ASR", "开始录音")
         *         }
         *
         *         override fun onComplete(file: File, duration: Long) {
         *             Log.d("ASR", "录音完成: ${file.path}")
         *         }
         *
         *         override fun onError(error: String) {
         *             Log.e("ASR", "录音错误: $error")
         *         }
         *     }
         * )
         *
         * // 停止录音
         * val audioFile = recorder.stopRecording()
         *
         * // 识别录音文件
         * if (audioFile != null) {
         *     SpeechSdk.ASR.transcribe(audioFile, asrCallback)
         * }
         * ```
         */
        fun startRecording(
            context: Context,
            outputFile: File? = null,
            format: AudioRecordFormat = AudioRecordFormat.M4A,
            sampleRate: Int = 16000,
            recordingCallback: RecordingCallback
        ): AudioRecorder {
            val recorder = AudioRecorder(context)
            recorder.startRecording(outputFile, format, sampleRate, recordingCallback)
            return recorder
        }

        /**
         * 录音并自动识别
         *
         * 一体化方法，自动完成录音和识别的全流程
         * 需要手动调用返回的 recorder 来停止录音
         *
         * @param context 上下文
         * @param hotwords 热词列表（可选）
         * @param asrCallback ASR识别回调
         * @return AudioRecorder实例，用于停止录音
         *
         * 示例：
         * ```
         * // 开始录音
         * val recorder = SpeechSdk.ASR.recordAndTranscribe(
         *     context = this,
         *     hotwords = listOf("专业术语"),
         *     asrCallback = object : AsrCallback {
         *         override fun onSuccess(result: AsrResult) {
         *             Log.d("ASR", "识别结果: ${result.text}")
         *         }
         *
         *         override fun onError(error: AsrError) {
         *             Log.e("ASR", "识别失败: ${error.message}")
         *         }
         *     }
         * )
         *
         * // 用户操作后停止录音
         * recorder.stopRecording()
         * ```
         */

        fun recordAndTranscribe(
            context: Context,
            hotwords: List<String>? = null,
            asrCallback: AsrCallback
        ): AudioRecorder {
            val recorder = AudioRecorder(context)

            recorder.startRecording(
                format = AudioRecordFormat.M4A,
                sampleRate = 16000,
                callback = object : RecordingCallback {
                    override fun onStart() {
                        // 录音开始
                    }

                    override fun onComplete(file: File, duration: Long) {
                        // 录音完成，自动执行识别
                        quickTranscribe(file, hotwords, asrCallback)
                    }

                    override fun onError(error: String) {
                        // 录音失败，通知ASR回调
                        asrCallback.onError(
                            AsrError(
                                code = ERROR_UNKNOWN,
                                message = "录音失败: $error"
                            )
                        )
                    }
                }
            )

            return recorder
        }

        /**
         * 取消所有ASR任务
         */
        fun cancelAll() {
            Asr.cancelAll()
        }
    }

    /**
     * 释放所有资源
     */
    fun release() {
        Tts.release()
        Asr.release()
    }
}