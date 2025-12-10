package com.stepfun.stepfunaudiocoresdk.audio.common.config

data class TtsConfig(
    // 默认模型
    val defaultModel: String = TtsModel.STEP_TTS_MINI.modelId,

    // 默认音色
    val defaultVoice: TtsVoice = TtsVoice.STEP_TTS_MINI_DEFAULT,

    // 默认响应格式
    val defaultResponseFormat: TtsAudioFormat = TtsAudioFormat.PCM,

    // 默认语速
    val defaultSpeed: Float = 1.0f,

    // 默认音量
    val defaultVolume: Float = 1.0f,

    // 默认采样率
    val defaultSampleRate: Int = TtsSampleRate.RATE_24000.rate,

    // 最大文本长度
    val maxTextLength: Int = 1000,

    // 是否启用缓存
    val enableCache: Boolean = false,

    // 缓存目录
    val cacheDir: String? = null,

    //模式
    val mode: String = "default"

)

enum class TtsSampleRate(val rate: Int) {
    RATE_8000(8000),
    RATE_16000(16000),
    RATE_22050(22050),
    RATE_24000(24000)
}

enum class TtsAudioFormat(val format: String) {
    WAV("wav"),
    MP3("mp3"),
    FLAC("flac"),
    OPUS("opus"),
    PCM("pcm")
}

/**
 * 音色标签
 */
data class VoiceLabel(
    val language: String? = null,
    val emotion: String? = null,
    val style: String? = null
)

/**
 * 发音映射
 */
data class PronunciationMap(
    val tone: String
)

/**
 * TTS模型
 */
enum class TtsModel(val modelId: String) {
    STEP_TTS_MINI("step-tts-mini"),
    STEP_TTS_VIVID("step-tts-vivid")
}


/**
 * TTS音色
 */
enum class TtsVoice(val voiceId: String) {
    STEP_TTS_MINI_DEFAULT("elegantgentle-female"),
    STEP_TTS_MINI_ENERGETIC_ELEGANTGENTLE_FEMALE("elegantgentle-female"),
    STEP_TTS_MINI_ENERGETIC_livelybreezy_female("livelybreezy-female"),
    STEP_TTS_MINI_ENERGETIC_CONFIDENT("energeticconfident-female"),
    STEP_TTS_MINI_JINGDIANNVSHENG("jingdiannvsheng"),
    STEP_TTS_MINI_WENROUSHUNV("wenroushunv"),
    STEP_TTS_MINI_WENROUNANSHENG("wenrounansheng"),
    STEP_TTS_MINI_TIANMEINVSHENG("tianmeinvsheng"),
    STEP_TTS_MINI_WENROUGONGZI("wenrougongzi"),
    STEP_TTS_MINI_QINGCHUNSHAONV("qingchunshaonv"),
    STEP_TTS_MINI_CIXINGNANSHENG("cixingnansheng"),
    STEP_TTS_MINI_ZHENGPAIQINGNIAN("zhengpaiqingnian"),
    STEP_TTS_MINI_YUANQINANSHENG("yuanqinansheng"),
    STEP_TTS_MINI_QINGNIANDAXUESHENG("qingniandaxuesheng"),
    STEP_TTS_MINI_BOYINNANSHENG("boyinnansheng"),
    STEP_TTS_MINI_RUYANANSHI("ruyananshi"),
    STEP_TTS_MINI_SHENChenNANYIN("shenchennanyin"),
    STEP_TTS_MINI_QINQIENVSHENG("qinqienvsheng"),
    STEP_TTS_MINI_WENROUNVSHENG("wenrounvsheng"),
    STEP_TTS_MINI_JILINGSHAONV("jilingshaonv"),
    STEP_TTS_MINI_YUANQISHAONV("yuanqishaonv"),
    STEP_TTS_MINI_RUANMENGNSHEng("ruanmengnvsheng"),
    STEP_TTS_MINI_YOUYANVSHENG("youyanvsheng"),
    STEP_TTS_MINI_LENGYANYUJIE("lengyanyujie"),
    STEP_TTS_MINI_SHUANGKUAIJIEJIE("shuangkuaijiejie"),
    STEP_TTS_MINI_WENJINGXUEJIE("wenjingxuejie"),
    STEP_TTS_MINI_LINJIAJIEJIE("linjiajiejie"),
    STEP_TTS_MINI_LINJIAMEIMEI("linjiameimei"),
    STEP_TTS_MINI_ZHIXINGJIEJIE("zhixingjiejie"),

    STEP_TTS_VIVID_DEFAULT("shuangkuainansheng"),
    STEP_TTS_VIVID_SHUANGKUAINANSHENG("shuangkuainansheng"),
    STEP_TTS_VIVID_GANLIANNVSHENG("ganliannvsheng"),
    STEP_TTS_VIVID_QINHENVSHENG("qinhenvsheng"),
    STEP_TTS_VIVID_HUOLINVSHENG("huolinvsheng"),
}

