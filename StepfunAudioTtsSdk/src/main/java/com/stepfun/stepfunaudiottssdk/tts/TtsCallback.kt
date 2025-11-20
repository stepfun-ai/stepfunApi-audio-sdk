package com.stepfun.stepfunaudiottssdk.tts

// TTS 回调接口
interface TtsCallback {

    fun onSuccess(audioData: ByteArray)

    fun onError(error: TtsError)
}

data class TtsError(
    val code: Int,
    val message: String,
    val cause: Throwable? = null
) {
    companion object {
        const val ERROR_NOT_INITIALIZED = 1001
        const val ERROR_INVALID_PARAMETERS = 1002
        const val ERROR_NETWORK = 1003
        const val ERROR_SERVER = 1004
        const val ERROR_WEBSOCKET = 1005
        const val ERROR_NO_SESSION = 1006
        const val ERROR_UNKNOWN = 1999
    }
}