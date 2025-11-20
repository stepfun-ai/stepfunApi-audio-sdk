package com.stepfun.stepfunaudiocoresdk.audio.common.network

interface BaseRequest

abstract class BaseResponse {
    var code: ResponseCode? = null

    val message: String? = null

    fun isSuccess() = code == 0

    fun isFail() = code != 0

    fun getFailedReason() = when (code) {
        Failed -> "网络请求失败,message:$message"
        Code_Error -> "参数异常,message:$message"
        Decode_Failed -> "数据解析失败,message:$message"
        else -> "未知的错误类型,message:$message"
    }
}

typealias ResponseCode = Int

val Failed: ResponseCode = -1 //请求失败
val Success: ResponseCode = 0 // 请求成功
val Code_Error: ResponseCode = 1 //参数异常
val Decode_Failed: ResponseCode = 2 //数据解码解析失败
