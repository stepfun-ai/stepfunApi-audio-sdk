# 阶跃星辰语音SDK

Android平台的语音合成（TTS）SDK，提供高质量的文本转语音服务。

## 项目结构

```
speech-sdk/
├── audio-sdk/              # 统一SDK入口，集成各子模块能力并对外暴露
├── audio-sdk-core/         # 核心模块，提供网络、配置等基础能力
├── audio-sdk-tts/          # TTS模块，实现文本转语音功能
├── audio-sdk-asr/          # ASR模块（待实现）
├── audio-sdk-sample/       # 示例项目，演示SDK集成和使用
├── README.md               # 本文件
└── INTEGRATION_GUIDE.md    # 集成指南
```

## 功能特性

### 已实现

- ✅ **TTS文本转语音**
  - 支持多种模型（step-tts-mini、step-tts-vivid）
  - 支持多种音频格式（MP3、WAV、FLAC、OPUS）
  - 可调节语速和音量
  - 支持自定义音色标签（语言、情感、风格）
  - 支持发音映射定制
  - 提供回调和协程两种异步方式

- ✅ **完善的SDK架构**
  - 模块化设计，易于扩展
  - 统一的配置管理
  - 完善的错误处理
  - 线程安全

- ✅ **示例项目**
  - 完整的集成示例
  - 丰富的功能演示
  - UI友好的测试界面

### 待实现

- ⏳ ASR语音识别
- ⏳ 实时语音流式处理
- ⏳ 音色管理
- ⏳ 缓存机制


## 快速开始

### 1. 查看示例项目

```bash
cd audio-sdk-sample
./gradlew assembleDebug
```

### 2. 集成到您的项目

详细步骤请查看 [集成指南](INTEGRATION_GUIDE.md)

简要步骤：

```kotlin
// 1. 添加依赖
implementation("stepfun.android:speech_sdk:0.0.1")

// 2. 初始化SDK
val config = SpeechConfig.Builder()
    .apiKey("your-api-key")
    .build()
SpeechSdk.init(context, config)

// 3. 使用TTS
SpeechSdk.TTS.generateSpeech(
    text = "你好世界",
    voice = "default",
    callback = object : TtsCallback {
        override fun onSuccess(audioData: ByteArray) {
            // 处理音频
        }
        override fun onError(error: TtsError) {
            // 处理错误
        }
    }
)
```

## 模块说明

### audio-sdk-core

核心基础模块，提供：
- HTTP网络客户端封装
- API接口定义
- 配置管理
- 通用工具类

**主要文件：**
- `HttpClient.kt` - 网络请求封装
- `SpeechConfig.kt` - SDK配置
- `TtsApi.kt` - TTS API定义
- `TtsConfig.kt` - TTS配置选项

### audio-sdk-tts

TTS功能模块，提供：
- TTS客户端实现
- 参数构建器
- 回调接口
- 错误处理

**主要文件：**
- `Tts.kt` - TTS主入口
- `TtsClient.kt` - TTS客户端
- `TtsSpeechParams.kt` - 请求参数
- `TtsCallback.kt` - 回调接口

### audio-sdk

集成模块，提供：
- 统一的SDK入口
- 模块能力整合
- 对外统一API

**主要文件：**
- `SpeechSdk.kt` - 统一入口

### audio-sdk-sample

示例应用，包含：
- 完整的集成示例
- 功能演示界面
- 最佳实践代码

## API 接口说明

### TTS文本转语音

**请求地址**
```
POST https://api.stepfun.com/v1/audio/speech
```

**支持的模型**
- `step-tts-mini` - 标准模型
- `step-tts-vivid` - 高级模型

**支持的格式**
- MP3 (默认)
- WAV
- FLAC
- OPUS

**参数说明**

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| model | string | ✓ | 模型名称 |
| input | string | ✓ | 要合成的文本（最大1000字符） |
| voice | string | ✓ | 音色名称 |
| response_format | string | - | 音频格式 |
| speed | float | - | 语速（0.5-2.0，默认1.0） |
| volume | float | - | 音量（0.1-2.0，默认1.0） |
| voice_label | object | - | 音色标签 |
| sample_rate | integer | - | 采样率（8000/16000/22050/24000） |
| pronunciation_map | array | - | 发音映射 |

详细API文档请查看 [集成指南](INTEGRATION_GUIDE.md)

## 开发说明

### 环境要求

- Android Studio Hedgehog | 2023.1.1+
- Kotlin 1.9.0+
- Gradle 8.0+
- JDK 11+

### 构建项目

```bash
# 构建所有模块
./gradlew build

# 构建特定模块
./gradlew :audio-sdk-core:build
./gradlew :audio-sdk-tts:build
./gradlew :audio-sdk:build

# 构建示例应用
cd audio-sdk-sample
./gradlew assembleDebug
```

### 运行示例

1. 修改 `audio-sdk-sample/app/src/main/java/com/stepfun/audiosample/SampleApplication.kt`
2. 替换 `your-api-key-here` 为您的实际API Key
3. 运行应用：
   ```bash
   ./gradlew :app:installDebug
   ```

## 文档

- [集成指南](INTEGRATION_GUIDE.md) - 详细的集成步骤和API文档
- [示例项目](audio-sdk-sample/README.md) - 示例项目说明
- [API参考](#api-接口说明) - API接口说明

## 版本历史

### v0.0.1-dev (当前)
- ✅ 实现TTS基础功能
- ✅ 支持所有API参数
- ✅ 提供完整示例项目
- ✅ 完善文档

## 技术支持

如有问题或建议，请联系：
- 邮箱：support@stepfun.com
- 文档：https://docs.stepfun.com

## 许可证

请遵循SDK使用协议。