package com.stepfun.stepfunaudioasrsdk.asr

import com.google.gson.Gson
import com.stepfun.stepfunaudiocoresdk.audio.common.network.NetworkManager
import com.stepfun.stepfunaudiocoresdk.audio.common.network.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import com.stepfun.stepfunaudiocoresdk.audio.core.SpeechCoreSdk

class AsrClient {

    companion object {
        private const val ASR_URL = "https://api.stepfun.com/v1/audio/transcriptions"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    //构建请求体
    private fun buildMultipartRequestBody(params: AsrParams): MultipartBody {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("model", params.model.modelId)
            .addFormDataPart("response_format", params.responseFormat.format)
        val fileRequestBody = params.file.asRequestBody("audio/*".toMediaTypeOrNull())
        builder.addFormDataPart(
            "file",
            params.file.name,
            fileRequestBody
        )

        params.hotWords?.let {
            if (it.isNotEmpty()) {
                val hotWordsJson = gson.toJson(it)
                builder.addFormDataPart("hotwords", hotWordsJson)
            }
        }
        return builder.build()
    }

    fun transcribe(params: AsrParams, callback: AsrCallback) {
        if (!SpeechCoreSdk.isInitialized()) {
            callback.onError(
                AsrError(
                    code = AsrError.ERROR_NOT_INITIALIZED,
                    message = "SpeechCoreSdk 没有初始化"
                )
            )
            return
        }
        params.validate()?.let {
            callback.onError(it)
            return
        }

        scope.launch {
            try {
                val requestBody = buildMultipartRequestBody(params)
                val result = NetworkManager.post<AsrResponse>(ASR_URL, requestBody)
                when {
                    result.isSuccess -> {
                        val response = result.getOrNull()
                        if (response != null) {
                            withContext(Dispatchers.Main) {
                                callback.onSuccess(
                                    AsrResult(
                                        text = response.text,
                                        rawResponse = gson.toJson(response)
                                    )
                                )
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                callback.onError(
                                    AsrError(
                                        code = AsrError.ERROR_SERVER,
                                        message = "识别结果为空"
                                    )
                                )
                            }
                        }
                    }

                    result.isError -> {
                        val error = (result as State.Error).exception
                        withContext(Dispatchers.Main) {
                            callback.onError(
                                AsrError(
                                    code = AsrError.ERROR_NETWORK,
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
                        AsrError(
                            code = AsrError.ERROR_UNKNOWN,
                            message = e.message ?: "Unknown error",
                            cause = e
                        )
                    )
                }
            }
        }
    }

    suspend fun transcribeAsync(params: AsrParams): AsrResult {
        return withContext(Dispatchers.IO) {
            if (!SpeechCoreSdk.isInitialized()) {
                throw IllegalStateException("SpeechCoreSdk 没有初始化")
            }
            params.validate()?.let {
                throw Exception("${it.code} : ${it.message}")
            }
            val requestBody = buildMultipartRequestBody(params)
            val result = NetworkManager.post<AsrResponse>(ASR_URL, requestBody)
            when {
                result.isSuccess -> {
                    val response = result.getOrNull() ?: throw Exception("empty response")
                    AsrResult(
                        text = response.text,
                        rawResponse = gson.toJson(response)
                    )
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