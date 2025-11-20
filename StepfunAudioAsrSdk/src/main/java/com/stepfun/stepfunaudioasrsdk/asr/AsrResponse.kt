package com.stepfun.stepfunaudioasrsdk.asr

import com.google.gson.annotations.SerializedName

data class AsrResponse(
    @SerializedName("text") val text: String
)
