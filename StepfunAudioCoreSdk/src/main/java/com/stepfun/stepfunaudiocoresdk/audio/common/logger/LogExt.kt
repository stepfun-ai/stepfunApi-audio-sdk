package com.stepfun.stepfunaudiocoresdk.audio.common.logger

import android.util.Log

const val COMMON_TAG = "SpeechSdk_"
const val CORE_TAG = COMMON_TAG + "core"
const val TTS_TAG = COMMON_TAG + "TTS"


fun String.logE(tag: String) {
    Log.e(tag, this)
}

fun String.logW(tag: String) {
    Log.w(tag, this)
}

fun String.logI(tag: String) {
    Log.i(tag, this)
}

fun String.logD(tag: String) {
    Log.d(tag, this)
}