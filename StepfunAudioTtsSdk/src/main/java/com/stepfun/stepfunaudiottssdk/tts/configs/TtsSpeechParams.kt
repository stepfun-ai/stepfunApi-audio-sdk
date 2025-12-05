package com.stepfun.stepfunaudiottssdk.tts.configs

import com.google.gson.annotations.SerializedName
import com.stepfun.stepfunaudiocoresdk.audio.common.config.PronunciationMap
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsAudioFormat
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsModel
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsVoice
import com.stepfun.stepfunaudiocoresdk.audio.common.config.VoiceLabel
import com.stepfun.stepfunaudiocoresdk.audio.common.network.BaseRequest

data class TtsSpeechParams(
    // 必须参数
    val model: String,
    val input: String,
    val voice: TtsVoice,

    // 可选参数
    val responseFormat: TtsAudioFormat? = null,
    val speed: Float? = null,
    val volume: Float? = null,
    val voiceLabel: VoiceLabel? = null,
    val sampleRate: Int? = null,
    val pronunciationMap: List<PronunciationMap>? = null
) {
    class Builder {
        private var model: String? = null
        private var input: String = ""
        private var voice: TtsVoice? = null

        private var responseFormat: TtsAudioFormat? = null
        private var speed: Float? = null
        private var volume: Float? = null
        private var voiceLabel: VoiceLabel? = null
        private var sampleRate: Int? = null
        private var pronunciationMap: List<PronunciationMap>? = null

        fun model(model: String) = apply {
            this.model = model
        }

        fun input(input: String) = apply {
            this.input = input
        }

        fun voice(voice: TtsVoice) = apply {
            this.voice = voice
        }

        fun responseFormat(format: TtsAudioFormat) = apply {
            this.responseFormat = format
        }

        fun speed(speed: Float) = apply {
            require(speed in 0.5f..2.0f) { "Speed must be between 0.5 and 2.0" }
            this.speed = speed
        }

        fun volume(volume: Float) = apply {
            require(volume in 0.1f..2.0f) { "Volume must be between 0.1 and 2.0" }
            this.volume = volume
        }

        fun voiceLabel(label: VoiceLabel) = apply {
            this.voiceLabel = label
        }

        fun sampleRate(rate: Int) = apply {
            require(
                rate in listOf(
                    8000,
                    16000,
                    22050,
                    24000
                )
            ) { "Sample rate must be one of 8000, 16000, 22050, 44100, or 48000" }
            this.sampleRate = rate
        }

        fun pronunciation(map: List<PronunciationMap>) = apply {
            this.pronunciationMap = map
        }

        fun build(): TtsSpeechParams {
            require(model!= null) {
                "必须要设置 model"
            }
            require(input.isNotEmpty()) {
                "必须要设置 input"
            }
            require(input.length <= 1000) {
                "input 长度不能超过 1000"
            }
            require(voice != null) {
                "必须要设置 voice"
            }
            return TtsSpeechParams(
                model = model!!,
                input = input,
                voice = voice!!,
                responseFormat = responseFormat,
                speed = speed,
                volume = volume,
                voiceLabel = voiceLabel,
                sampleRate = sampleRate,
                pronunciationMap = pronunciationMap
            )
        }
    }

    internal fun toNetworkRequest() =
        TtsSpeechRequest(
            model = model,
            input = input,
            voice = voice.voiceId,
            responseFormat = responseFormat?.format,
            speed = speed,
            volume = volume,
            voiceLabel = voiceLabel,
            sampleRate = sampleRate,
            pronunciationMap = pronunciationMap
        )
}

data class TtsSpeechRequest(
    @SerializedName("model") val model: String,
    @SerializedName("input") val input: String,
    @SerializedName("voice") val voice: String,
    @SerializedName("responseFormat") val responseFormat: String?,
    @SerializedName("speed") val speed: Float?,
    @SerializedName("volume") val volume: Float?,
    @SerializedName("voiceLabel") val voiceLabel: VoiceLabel?,
    @SerializedName("sampleRate") val sampleRate: Int?,
    @SerializedName("pronunciationMap") val pronunciationMap: List<PronunciationMap>?
) : BaseRequest
