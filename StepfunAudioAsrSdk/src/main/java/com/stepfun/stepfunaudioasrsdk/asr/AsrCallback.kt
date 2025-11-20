package com.stepfun.stepfunaudioasrsdk.asr

interface AsrCallback {

    /**
     * 识别成功
     * @param result 识别结果
     */
    fun onSuccess(result: AsrResult)

    /**
     * 识别失败
     * @param error 错误信息
     */
    fun onError(error: AsrError)
}

data class AsrResult(
    val text: String,
    val rawResponse: String? = null
)

data class AsrError(
    val code: Int,
    val message: String,
    val cause: Throwable? = null
) {
    companion object {
        const val ERROR_NOT_INITIALIZED = 2001      // SDK未初始化
        const val ERROR_INVALID_PARAMETERS = 2002   // 参数错误
        const val ERROR_FILE_NOT_FOUND = 2003       // 文件不存在
        const val ERROR_FILE_TOO_LARGE = 2004       // 文件过大（>100MB）
        const val ERROR_UNSUPPORTED_FORMAT = 2005   // 不支持的音频格式
        const val ERROR_NETWORK = 2006              // 网络错误
        const val ERROR_SERVER = 2007               // 服务器错误
        const val ERROR_UNKNOWN = 2999              // 未知错误
    }
}