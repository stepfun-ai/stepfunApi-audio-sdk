package com.stepfun.stepfunaudiottssdk.tts

import com.stepfun.stepfunaudiocoresdk.audio.common.network.NetworkManager
import com.stepfun.stepfunaudiocoresdk.audio.common.network.State
import com.stepfun.stepfunaudiocoresdk.audio.core.SpeechCoreSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.stepfun.stepfunaudiottssdk.tts.configs.TtsSpeechParams
import java.io.File
import java.io.FileOutputStream

class TtsClient {

    companion object {
        private const val TTS_URL = "https://api.stepfun.com/v1/audio/speech"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 方式一：生成语音（异步回调方式）
     */
    fun generateSpeech(params: TtsSpeechParams, callback: TtsCallback) {
        if (!SpeechCoreSdk.isInitialized()) {
            callback.onError(
                TtsError(
                    code = TtsError.ERROR_NOT_INITIALIZED,
                    message = "SpeechCoreSdk 没有初始化"
                )
            )
            return
        }

        scope.launch {
            try {
                val result = NetworkManager.postForBinary(TTS_URL, params.toNetworkRequest(), 2)

                when {
                    result.isSuccess -> {
                        val audioData = result.getOrNull()
                        if (audioData != null) {
                            withContext(Dispatchers.Main) {
                                callback.onSuccess(audioData)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                callback.onError(
                                    TtsError(
                                        code = TtsError.ERROR_SERVER,
                                        message = "音频数据为空"
                                    )
                                )
                            }
                        }
                    }

                    result.isError -> {
                        val error = (result as State.Error).exception
                        withContext(Dispatchers.Main) {
                            callback.onError(
                                TtsError(
                                    code = TtsError.ERROR_NETWORK,
                                    message = error.message ?: "Network error",
                                    cause = error
                                )
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(
                        TtsError(
                        code = TtsError.ERROR_UNKNOWN,
                        message = e.message ?: "Unknown error",
                        cause = e
                    )
                    )
                }
            }
        }
    }

    /**
     * 方式2：生成语音（协程方式）
     */
    suspend fun generateSpeechAsync(params: TtsSpeechParams): ByteArray {
        return withContext(Dispatchers.IO) {
            val result = NetworkManager.postForBinary(
                url = TTS_URL,
                request = params.toNetworkRequest(),
                retryTimes = 3
            )

            when {
                result.isSuccess -> {
                    result.getOrNull() ?: throw Exception("Empty response body")
                }
                result.isError -> {
                    val error = (result as State.Error).exception
                    throw error
                }
                else -> throw Exception("Unknown error")
            }
        }
    }

    /**
     * 生成语音并保存到文件
     */
    fun generateSpeechToFile(
        request: TtsSpeechParams,
        outputFile: File,
        callback: TtsCallback
    ) {
        generateSpeech(request, object : TtsCallback {
            override fun onSuccess(audioData: ByteArray) {
                try {
                    FileOutputStream(outputFile).use { fos ->
                        fos.write(audioData)
                    }
                    callback.onSuccess(audioData)
                } catch (e: Exception) {
                    callback.onError(
                        TtsError(
                        code = TtsError.ERROR_UNKNOWN,
                        message = "Failed to save file: ${e.message}",
                        cause = e
                    )
                    )
                }
            }

            override fun onError(error: TtsError) {
                callback.onError(error)
            }
        })
    }

    /**
     * 取消所有任务
     */
    fun cancelAll() {
        scope.coroutineContext.cancelChildren()
    }

    /**
     * 释放资源
     */
    fun release() {
        scope.cancel()
    }
}