package com.stepfun.stepfunaudiottssdk.tts.configs

import com.stepfun.stepfunaudiocoresdk.audio.common.config.PronunciationMap
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsAudioFormat
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsModel
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsVoice
import com.stepfun.stepfunaudiottssdk.tts.event.TtsCreateEvent

data class TtsStreamParams(
//    val model: TtsModel,
    val model: String,
//    val voice: TtsVoice,
    val voice: String,
    val responseFormat: TtsAudioFormat = TtsAudioFormat.PCM,
    val sampleRate: Int = 24000,
    val volumeRatio: Float = 1.0f,
    val speedRatio: Float = 1.0f,
    val features: TtsCreateEvent.Features? = null,
    val pronunciationMap: List<PronunciationMap>? = null
) {
    class Builder {
        private var model: String? = null
        private var voice: String? = null
        private var responseFormat: TtsAudioFormat = TtsAudioFormat.MP3
        private var sampleRate: Int = 24000
        private var volumeRatio: Float = 1.0f
        private var speedRatio: Float = 1.0f
        private var pronunciationMap: List<PronunciationMap>? = null
        private var features: TtsCreateEvent.Features? = null

        fun model(model: String) = apply { this.model = model }
        fun voice(voice: String) = apply { this.voice = voice }
        fun responseFormat(format: TtsAudioFormat) = apply { this.responseFormat = format }
        fun sampleRate(rate: Int) = apply {
            require(rate in listOf(8000, 16000, 24000)) {
                "Sample rate must be 8000, 16000, or 24000"
            }
            this.sampleRate = rate
        }

        fun volumeRatio(ratio: Float) = apply {
            require(ratio in 0.1f..2.0f) { "Volume ratio must be between 0.1 and 2.0" }
            this.volumeRatio = ratio
        }

        fun speedRatio(ratio: Float) = apply {
            require(ratio in 0.5f..2.0f) { "Speed ratio must be between 0.5 and 2.0" }
            this.speedRatio = ratio
        }

        fun pronunciationMap(map: List<PronunciationMap>) = apply {
            this.pronunciationMap = map
        }

        fun features(features: TtsCreateEvent.Features) = apply {
            this.features = features
        }

        fun build(): TtsStreamParams {
            requireNotNull(model) { "model is required" }
            requireNotNull(voice) { "voice is required" }
            return TtsStreamParams(
                model = model!!,
                voice = voice!!,
                responseFormat = responseFormat,
                sampleRate = sampleRate,
                volumeRatio = volumeRatio,
                speedRatio = speedRatio,
                pronunciationMap = pronunciationMap,
                features = features
            )
        }
    }
}
