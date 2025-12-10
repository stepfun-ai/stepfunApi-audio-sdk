# StepFun Audio SDK 使用文档

---

## 目录

1. [快速开始](#快速开始)
2. [SDK 初始化](#sdk-初始化)
3. [TTS 文字转语音](#tts-文字转语音)
   - [一次性生成](#一次性生成)
   - [生成并播放](#生成并播放)
   - [流式 TTS](#流式-tts)
4. [ASR 语音识别](#asr-语音识别)
   - [识别音频文件](#识别音频文件)
   - [录音并识别](#录音并识别)
5. [配置参数说明](#配置参数说明)
6. [错误处理](#错误处理)
7. [资源释放](#资源释放)

---

## 快速开始

### 添加依赖

```kotlin
implementation("com.stepfun:stepfun-audio-sdk:x.x.x")
```

### 最简示例

```kotlin
// 1. 初始化 SDK
SpeechSdk.init(context, SpeechConfig.Builder()
    .httpBaseUrl("https://your-api-server.com")
    .webSocketUrl("wss://your-ws-server.com")
    .customHeaders(mapOf("Authorization" to "Bearer YOUR_API_KEY"))
    .enableLogging(true)
    .build()
)

// 2. 一键播放语音
SpeechSdk.TTS.quickPlay(context, "你好，世界！")
```

---

## SDK 初始化

### SpeechConfig 配置

```kotlin
val config = SpeechConfig.Builder()
    // 服务器地址（必填）
    .httpBaseUrl("https://api.stepfun.com")
    .webSocketUrl("wss://ws.stepfun.com")
    
    // 认证信息（通过 headers 传递）
    .customHeaders(mapOf(
        "Authorization" to "Bearer sk-xxxxxx"
    ))
    
    // 环境配置
    .environment(Environment.PRODUCTION)
    
    // 日志开关
    .enableLogging(true)
    
    // TTS 默认配置
    .ttsConfig(TtsConfig(
        defaultModel = TtsModel.STEP_TTS_MINI.modelId,
        defaultVoice = TtsVoice.STEP_TTS_MINI_DEFAULT,
        defaultResponseFormat = TtsAudioFormat.PCM,
        defaultSpeed = 1.0f,
        defaultVolume = 1.0f,
        defaultSampleRate = 24000
    ))
    
    .build()

SpeechSdk.init(context, config)
```

### 检查初始化状态

```kotlin
if (SpeechSdk.isInitialized()) {
    // SDK 已初始化
}
```

### 更新配置

```kotlin
SpeechSdk.updateConfig(newConfig)
```

---

## TTS 文字转语音

### 一次性生成

#### 使用默认配置

```kotlin
SpeechSdk.TTS.generateSpeech(
    text = "你好，我是阶跃星辰语音助手",
    voice = TtsVoice.STEP_TTS_MINI_DEFAULT,
    callback = object : TtsCallback {
        override fun onSuccess(audioData: ByteArray) {
            // 获取到音频数据（可保存或自行播放）
        }

        override fun onError(error: TtsError) {
            Log.e("TTS", "生成失败: ${error.message}")
        }
    }
)
```

#### 使用自定义参数

```kotlin
val params = TtsSpeechParams.Builder()
    .model(TtsModel.STEP_TTS_MINI.modelId)
    .input("需要合成的文本内容")
    .voice(TtsVoice.STEP_TTS_MINI_CIXINGNANSHENG)
    .responseFormat(TtsAudioFormat.MP3)
    .speed(1.2f)           // 语速 0.5-2.0
    .volume(1.0f)          // 音量 0.1-2.0
    .sampleRate(24000)     // 采样率
    .build()

SpeechSdk.TTS.generateSpeech(params, callback)
```

#### 协程方式

```kotlin
lifecycleScope.launch {
    try {
        val audioData = SpeechSdk.TTS.generateSpeechAsync(params)
        // 使用音频数据
    } catch (e: Exception) {
        Log.e("TTS", "生成失败", e)
    }
}
```

#### 保存到文件

```kotlin
val outputFile = File(cacheDir, "speech.mp3")
SpeechSdk.TTS.generateSpeechToFile(params, outputFile, callback)
```

---

### 生成并播放

#### 快速播放（推荐）

```kotlin
// 最简单的方式，一行代码搞定
SpeechSdk.TTS.quickPlay(
    context = this,
    text = "你好，世界！",
    voice = TtsVoice.STEP_TTS_MINI_DEFAULT
)
```

#### 带回调的播放

```kotlin
SpeechSdk.TTS.generateAndPlay(
    context = this,
    params = TtsSpeechParams.Builder()
        .model(TtsModel.STEP_TTS_MINI.modelId)
        .input("需要播放的文本")
        .voice(TtsVoice.STEP_TTS_MINI_DEFAULT)
        .build(),
    callback = object : TtsCallback {
        override fun onSuccess(audioData: ByteArray) {
            Log.d("TTS", "生成成功，正在播放")
        }

        override fun onError(error: TtsError) {
            Log.e("TTS", "生成失败: ${error.message}")
        }
    },
    playbackCallback = object : AudioPlayer.PlaybackCallback {
        override fun onStart() {
            Log.d("TTS", "开始播放")
        }

        override fun onComplete() {
            Log.d("TTS", "播放完成")
        }

        override fun onError(error: String) {
            Log.e("TTS", "播放错误: $error")
        }
    }
)
```

#### 控制播放

```kotlin
// 停止播放
SpeechSdk.TTS.stopPlayback()

// 取消所有任务
SpeechSdk.TTS.cancelAll()
```

---

### 流式 TTS

流式 TTS 适用于需要**边生成边播放**的场景，可以显著降低首字延迟。

#### 创建流式会话

```kotlin
val params = TtsStreamParams.Builder()
    .model(TtsModel.STEP_TTS_MINI.modelId)
    .voice(TtsVoice.STEP_TTS_MINI_CIXINGNANSHENG.voiceId)
    .responseFormat(TtsAudioFormat.MP3)  // 支持 PCM/MP3/FLAC/WAV/OPUS
    .sampleRate(24000)                   // 8000/16000/24000
    .speedRatio(1.0f)                    // 语速 0.5-2.0
    .volumeRatio(1.0f)                   // 音量 0.1-2.0
    .mode("sentence")
    .build()

SpeechSdk.TTS.createStreamSession(
    context = this,
    params = params,
    callback = object : TtsStreamCallback {
        override fun onConnectionEstablished(sessionId: String) {
            Log.d("TTS", "WebSocket 连接建立")
        }

        override fun onSessionCreated(sessionId: String) {
            Log.d("TTS", "会话创建成功: $sessionId")
            // 可以开始发送文本
            SpeechSdk.TTS.sendStreamText("你好，我是阶跃星辰语音助手")
        }

        override fun onSentenceStart(text: String) {
            Log.d("TTS", "开始生成句子: $text")
        }

        override fun onAudioData(audioData: ByteArray, isFinished: Boolean) {
            // 音频数据会自动播放，也可以在这里处理
            Log.d("TTS", "收到音频: ${audioData.size} bytes, finished: $isFinished")
        }

        override fun onSentenceEnd(text: String) {
            Log.d("TTS", "句子生成完成: $text")
        }

        override fun onFlushed() {
            Log.d("TTS", "缓冲区已清空")
        }

        override fun onComplete() {
            Log.d("TTS", "全部生成完成")
        }

        override fun onError(error: TtsStreamError) {
            Log.e("TTS", "错误: ${error.code} - ${error.message}")
        }
    }
)
```

#### 使用默认配置创建会话

```kotlin
SpeechSdk.TTS.createStreamSession(
    context = this,
    voice = TtsVoice.STEP_TTS_MINI_DEFAULT.voiceId,
    callback = streamCallback
)
```

#### 流式控制方法

```kotlin
// 发送文本（可多次调用）
SpeechSdk.TTS.sendStreamText("第一段文本")
SpeechSdk.TTS.sendStreamText("第二段文本")

// 清空缓冲区（快速获取当前未返回的音频）
SpeechSdk.TTS.flushStream()

// 完成生成（通知服务端不再发送文本）
SpeechSdk.TTS.finishStream()

// 暂停播放
SpeechSdk.TTS.pauseStream()

// 恢复播放
SpeechSdk.TTS.resumeStream()

// 停止播放（不关闭连接）
SpeechSdk.TTS.stopStream()
```

---

## ASR 语音识别

### 识别音频文件

#### 基础用法

```kotlin
val audioFile = File("/path/to/audio.mp3")

SpeechSdk.ASR.transcribe(audioFile, object : AsrCallback {
    override fun onSuccess(result: AsrResult) {
        Log.d("ASR", "识别结果: ${result.text}")
    }

    override fun onError(error: AsrError) {
        Log.e("ASR", "识别失败: ${error.message}")
    }
})
```

#### 使用自定义参数

```kotlin
val params = AsrParams.Builder()
    .file(audioFile)
    .responseFormat(AsrResponseFormat.JSON)
    .hotwords(listOf("阶跃星辰", "人工智能"))  // 热词提升识别准确度
    .build()

SpeechSdk.ASR.transcribe(params, callback)
```

#### 快速识别

```kotlin
SpeechSdk.ASR.quickTranscribe(
    file = audioFile,
    hotwords = listOf("专业术语"),
    callback = asrCallback
)
```

#### 协程方式

```kotlin
lifecycleScope.launch {
    try {
        val result = SpeechSdk.ASR.transcribeAsync(params)
        Log.d("ASR", "识别结果: ${result.text}")
    } catch (e: Exception) {
        Log.e("ASR", "识别失败", e)
    }
}
```

---

### 录音并识别

#### 分步录音

```kotlin
// 1. 开始录音
val recorder = SpeechSdk.ASR.startRecording(
    context = this,
    format = AudioRecordFormat.M4A,
    sampleRate = 16000,
    recordingCallback = object : RecordingCallback {
        override fun onStart() {
            Log.d("ASR", "开始录音")
        }

        override fun onComplete(file: File, duration: Long) {
            Log.d("ASR", "录音完成: ${file.path}, 时长: ${duration}ms")
            // 2. 识别录音文件
            SpeechSdk.ASR.transcribe(file, asrCallback)
        }

        override fun onError(error: String) {
            Log.e("ASR", "录音错误: $error")
        }
    }
)

// 3. 用户操作后停止录音
binding.stopButton.setOnClickListener {
    recorder.stopRecording()
}
```

#### 一体化录音识别

```kotlin
val recorder = SpeechSdk.ASR.recordAndTranscribe(
    context = this,
    hotwords = listOf("专业术语"),
    asrCallback = object : AsrCallback {
        override fun onSuccess(result: AsrResult) {
            Log.d("ASR", "识别结果: ${result.text}")
        }

        override fun onError(error: AsrError) {
            Log.e("ASR", "识别失败: ${error.message}")
        }
    }
)

// 停止录音后自动执行识别
recorder.stopRecording()
```

---

## 配置参数说明

### TTS 音色列表

| 音色 | voiceId | 说明 |
|------|---------|------|
| `STEP_TTS_MINI_DEFAULT` | elegantgentle-female | 默认女声 |
| `STEP_TTS_MINI_CIXINGNANSHENG` | cixingnansheng | 磁性男声 |
| `STEP_TTS_MINI_WENROUNANSHENG` | wenrounansheng | 温柔男声 |
| `STEP_TTS_MINI_TIANMEINVSHENG` | tianmeinvsheng | 甜美女声 |
| `STEP_TTS_MINI_QINGCHUNSHAONV` | qingchunshaonv | 青春少女 |
| `STEP_TTS_MINI_BOYINNANSHENG` | boyinnansheng | 播音男声 |
| `STEP_TTS_MINI_WENROUGONGZI` | wenrougongzi | 温柔公子 |
| `STEP_TTS_MINI_YUANQINANSHENG` | yuanqinansheng | 元气男声 |
| `STEP_TTS_MINI_QINGNIANDAXUESHENG` | qingniandaxuesheng | 青年大学生 |
| `STEP_TTS_MINI_SHENCHENNANYIN` | shenchennanyin | 深沉男音 |
| `STEP_TTS_MINI_WENROUNVSHENG` | wenrounvsheng | 温柔女声 |
| `STEP_TTS_MINI_JILINGSHAONV` | jilingshaonv | 机灵少女 |
| `STEP_TTS_MINI_YUANQISHAONV` | yuanqishaonv | 元气少女 |
| `STEP_TTS_MINI_RUANMENGNVSHENG` | ruanmengnvsheng | 软萌女声 |
| `STEP_TTS_MINI_YOUYANVSHENG` | youyanvsheng | 优雅女声 |
| `STEP_TTS_MINI_LENGYANYUJIE` | lengyanyujie | 冷艳御姐 |
| `STEP_TTS_MINI_SHUANGKUAIJIEJIE` | shuangkuaijiejie | 爽快姐姐 |
| `STEP_TTS_MINI_WENJINGXUEJIE` | wenjingxuejie | 文静学姐 |
| `STEP_TTS_MINI_LINJIAJIEJIE` | linjiajiejie | 邻家姐姐 |
| `STEP_TTS_MINI_LINJIAMEIMEI` | linjiameimei | 邻家妹妹 |
| `STEP_TTS_MINI_ZHIXINGJIEJIE` | zhixingjiejie | 知性姐姐 |
| `STEP_TTS_VIVID_SHUANGKUAINANSHENG` | shuangkuainansheng | 爽快男声 (Vivid) |
| `STEP_TTS_VIVID_GANLIANNVSHENG` | ganliannvsheng | 干练女声 (Vivid) |
| `STEP_TTS_VIVID_QINHENVSHENG` | qinhenvsheng | 亲和女声 (Vivid) |
| `STEP_TTS_VIVID_HUOLINVSHENG` | huolinvsheng | 活力女声 (Vivid) |

### TTS 音频格式

| 格式 | 说明 | 适用场景 |
|------|------|----------|
| `PCM` | 原始 PCM 数据 | 低延迟流式播放 |
| `MP3` | MP3 压缩格式 | 通用场景 |
| `WAV` | WAV 无损格式 | 高质量需求 |
| `FLAC` | FLAC 无损压缩 | 存储节省 |
| `OPUS` | Opus 压缩格式 | 网络传输 |

### TTS 采样率

| 采样率 | 说明 |
|--------|------|
| 8000 | 电话质量 |
| 16000 | 语音识别常用 |
| 24000 | 推荐（默认） |

### 参数范围

| 参数 | 范围 | 默认值 |
|------|------|--------|
| `speed` / `speedRatio` | 0.5 - 2.0 | 1.0 |
| `volume` / `volumeRatio` | 0.1 - 2.0 | 1.0 |
| `input` 文本长度 | ≤ 1000 字符 | - |

---

## 错误处理

### TTS 错误

```kotlin
override fun onError(error: TtsError) {
    when (error.code) {
        TtsError.ERROR_NETWORK -> // 网络错误
        TtsError.ERROR_INVALID_PARAMS -> // 参数错误
        TtsError.ERROR_UNAUTHORIZED -> // 认证失败
        else -> // 其他错误
    }
}
```

### 流式 TTS 错误

```kotlin
override fun onError(error: TtsStreamError) {
    Log.e("TTS", "错误码: ${error.code}, 消息: ${error.message}")
    error.details?.let { details ->
        // 详细错误信息
    }
}
```

### ASR 错误

```kotlin
override fun onError(error: AsrError) {
    when (error.code) {
        AsrError.ERROR_UNKNOWN -> // 未知错误
        else -> Log.e("ASR", error.message)
    }
}
```

---

## 资源释放

```kotlin
// 在 Activity/Fragment 销毁时释放资源
override fun onDestroy() {
    super.onDestroy()
    SpeechSdk.release()
}
```

---

## 完整示例

### 流式 TTS 完整示例

```kotlin
class TtsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 SDK
        if (!SpeechSdk.isInitialized()) {
            SpeechSdk.init(this, SpeechConfig.Builder()
                .httpBaseUrl("https://api.stepfun.com")
                .webSocketUrl("wss://ws.stepfun.com")
                .customHeaders(mapOf("Authorization" to "Bearer YOUR_KEY"))
                .build()
            )
        }
        
        // 创建流式会话
        SpeechSdk.TTS.createStreamSession(
            context = this,
            voice = TtsVoice.STEP_TTS_MINI_CIXINGNANSHENG.voiceId,
            callback = object : TtsStreamCallback {
                override fun onConnectionEstablished(sessionId: String) {}
                
                override fun onSessionCreated(sessionId: String) {
                    // 发送文本
                    SpeechSdk.TTS.sendStreamText("你好，我是阶跃星辰语音助手，很高兴为你服务。")
                    SpeechSdk.TTS.finishStream()
                }
                
                override fun onSentenceStart(text: String) {}
                override fun onAudioData(audioData: ByteArray, isFinished: Boolean) {}
                override fun onSentenceEnd(text: String) {}
                override fun onFlushed() {}
                override fun onComplete() {
                    Log.d("TTS", "播放完成")
                }
                
                override fun onError(error: TtsStreamError) {
                    Log.e("TTS", "错误: ${error.message}")
                }
            }
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        SpeechSdk.release()
    }
}
```

### 录音识别完整示例

```kotlin
class AsrActivity : AppCompatActivity() {
    private var recorder: AudioRecorder? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 开始录音按钮
        binding.startButton.setOnClickListener {
            recorder = SpeechSdk.ASR.recordAndTranscribe(
                context = this,
                hotwords = listOf("阶跃星辰"),
                asrCallback = object : AsrCallback {
                    override fun onSuccess(result: AsrResult) {
                        binding.resultText.text = result.text
                    }

                    override fun onError(error: AsrError) {
                        Toast.makeText(this@AsrActivity, error.message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
            binding.startButton.isEnabled = false
            binding.stopButton.isEnabled = true
        }
        
        // 停止录音按钮
        binding.stopButton.setOnClickListener {
            recorder?.stopRecording()
            binding.startButton.isEnabled = true
            binding.stopButton.isEnabled = false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        SpeechSdk.release()
    }
}
```

---

## 常见问题

### Q: 流式 TTS 和一次性 TTS 如何选择？

| 场景 | 推荐方式 |
|------|----------|
| 短文本（< 100 字） | 一次性 TTS |
| 长文本、需要低延迟 | 流式 TTS |
| 实时对话场景 | 流式 TTS |
| 离线保存音频 | 一次性 TTS |

### Q: PCM 和 MP3 格式如何选择？

| 格式 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| PCM | 延迟低、解码快 | 数据量大 | 流式播放 |
| MP3 | 数据量小 | 需要解码 | 存储、传输 |

### Q: 如何处理网络异常？

SDK 会通过回调返回错误信息，建议：
1. 在 `onError` 回调中处理错误
2. 对于网络错误，可以提示用户检查网络后重试
3. 对于认证错误，检查 API Key 是否正确

---

## 更新日志

### v1.0.0
- 初始版本发布
- 支持 TTS 一次性生成和流式生成
- 支持 ASR 语音识别
- 支持录音功能

