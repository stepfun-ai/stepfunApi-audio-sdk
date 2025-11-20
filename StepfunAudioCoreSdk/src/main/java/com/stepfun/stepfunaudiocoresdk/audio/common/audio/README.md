# AudioPlayer - 音频播放器组件

通用的音频播放器组件，封装了 MediaPlayer 的常用功能。

## 功能特性

- ✅ 支持从字节数组播放音频
- ✅ 支持从文件路径播放音频
- ✅ 支持暂停、恢复、停止
- ✅ 支持音量控制
- ✅ 支持循环播放
- ✅ 支持播放进度控制
- ✅ 自动管理临时文件
- ✅ 完善的回调机制
- ✅ 线程安全

## 使用方式

### 1. 基础使用

```kotlin
// 创建播放器实例
val audioPlayer = AudioPlayer(context)

// 从字节数组播放 - 内部自动处理线程，无需协程
audioPlayer.playFromByteArray(
    audioData = audioData,
    fileExtension = "mp3",
    callback = object : AudioPlayer.PlaybackCallback {
        override fun onStart() {
            Log.d(TAG, "开始播放")
        }
        
        override fun onCompletion() {
            Log.d(TAG, "播放完成")
        }
        
        override fun onError(error: String, exception: Exception?) {
            Log.e(TAG, "播放失败: $error", exception)
        }
    }
)

// 释放资源
audioPlayer.release()
```

### 2. 从文件播放

```kotlin
val audioPlayer = AudioPlayer(context)

// 播放本地文件
audioPlayer.playFromFile("/path/to/audio.mp3")

// 暂停
audioPlayer.pause()

// 恢复
audioPlayer.resume()

// 停止
audioPlayer.stop()
```

### 3. 使用单例管理器

```kotlin
// 获取全局播放器实例
val audioPlayer = AudioPlayerManager.getInstance(context)

// 直接播放，无需协程
audioPlayer.playFromByteArray(
    audioData = audioData,
    fileExtension = "wav"
)

// ⚠️ 注意：应用正常退出时不需要手动释放（系统会自动回收）
// 仅在低内存场景下才需要释放：
class MyApplication : Application() {
    override fun onLowMemory() {
        super.onLowMemory()
        AudioPlayerManager.release()  // 低内存时释放
    }
}
```

### 4. 控制播放

```kotlin
val audioPlayer = AudioPlayer(context)

// 检查播放状态
if (audioPlayer.isPlaying()) {
    audioPlayer.pause()
}

// 跳转到指定位置（毫秒）
audioPlayer.seekTo(5000)

// 获取当前位置
val currentPos = audioPlayer.getCurrentPosition()

// 获取总时长
val duration = audioPlayer.getDuration()

// 设置音量（左声道、右声道）
audioPlayer.setVolume(0.8f, 0.8f)

// 设置循环播放
audioPlayer.setLooping(true)
```

## API 文档

### AudioPlayer

#### 构造函数

```kotlin
AudioPlayer(context: Context)
```

#### 主要方法

| 方法 | 说明 |
|------|------|
| `playFromByteArray(audioData, fileExtension, callback)` | 从字节数组播放音频 |
| `playFromFile(filePath)` | 从文件路径播放音频 |
| `pause()` | 暂停播放 |
| `resume()` | 恢复播放 |
| `stop()` | 停止播放 |
| `seekTo(position)` | 跳转到指定位置 |
| `isPlaying()` | 检查是否正在播放 |
| `getCurrentPosition()` | 获取当前播放位置 |
| `getDuration()` | 获取音频总时长 |
| `setVolume(left, right)` | 设置音量 |
| `setLooping(looping)` | 设置是否循环播放 |
| `release()` | 释放资源 |

### PlaybackCallback

播放回调接口：

```kotlin
interface PlaybackCallback {
    fun onPrepared()                // 播放器准备完成
    fun onStart()                   // 播放开始
    fun onCompletion()              // 播放完成
    fun onError(error, exception)   // 播放错误
    fun onProgressUpdate(current, duration)  // 进度更新（可选）
}
```

### AudioPlayerManager

单例管理器：

```kotlin
// 获取实例
AudioPlayerManager.getInstance(context): AudioPlayer

// 释放全局播放器
AudioPlayerManager.release()
```

## 注意事项

1. **资源释放**: 
   - Activity 中的 `AudioPlayer` 实例：**必须**在 `onDestroy()` 中调用 `release()`
   - `AudioPlayerManager` 单例：**不需要**在应用退出时释放（系统自动回收）
   - 低内存场景：建议在 `onLowMemory()` 中释放单例

2. **线程安全**: 所有方法都是线程安全的

3. **临时文件**: 使用 `playFromByteArray` 会创建临时文件，播放完成后自动删除

4. **文件格式**: 支持 Android MediaPlayer 支持的所有音频格式（mp3, wav, ogg, flac 等）

5. **线程处理**: `playFromByteArray` 内部自动在后台线程处理 IO 操作，**无需协程**，直接调用即可

## 完整示例

```kotlin
class AudioActivity : AppCompatActivity() {
    
    private lateinit var audioPlayer: AudioPlayer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化播放器
        audioPlayer = AudioPlayer(this)
        
        // 播放TTS生成的音频 - 无需协程，直接调用
        SpeechSdk.TTS.generateSpeech(params, object : TtsCallback {
            override fun onSuccess(audioData: ByteArray) {
                audioPlayer.playFromByteArray(
                    audioData = audioData,
                    fileExtension = "mp3",
                    callback = object : AudioPlayer.PlaybackCallback {
                        override fun onStart() {
                            updateUI("正在播放")
                        }
                        
                        override fun onCompletion() {
                            updateUI("播放完成")
                        }
                        
                        override fun onError(error: String, exception: Exception?) {
                            Toast.makeText(
                                this@AudioActivity,
                                "播放失败: $error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
            
            override fun onError(error: TtsError) {
                Log.e(TAG, "TTS生成失败: ${error.message}")
            }
        })
    }
    
    override fun onPause() {
        super.onPause()
        // Activity暂停时暂停播放
        audioPlayer.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Activity销毁时释放资源
        audioPlayer.release()
    }
    
    private fun updateUI(status: String) {
        runOnUiThread {
            statusText.text = status
        }
    }
}
```

## 错误处理

常见错误及解决方案：

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| IOException | 文件不存在或无法访问 | 检查文件路径和权限 |
| IllegalStateException | 播放器状态异常 | 确保按正确顺序调用方法 |
| 播放器错误 | 音频格式不支持 | 使用 MediaPlayer 支持的格式 |

## 资源管理最佳实践

### Activity/Fragment 中使用

```kotlin
class AudioActivity : AppCompatActivity() {
    private lateinit var audioPlayer: AudioPlayer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioPlayer = AudioPlayer(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()  // ✅ 必须释放
    }
}
```

### Application 全局使用

```kotlin
class MyApplication : Application() {
    
    // 使用单例，不需要在 onCreate 中初始化
    
    override fun onLowMemory() {
        super.onLowMemory()
        // ✅ 低内存时释放以节省资源
        AudioPlayerManager.release()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // ✅ 应用在后台且内存紧张时释放
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            AudioPlayerManager.release()
        }
    }
    
    // ❌ 不要在 onTerminate 中释放（不会被调用）
    // override fun onTerminate() {
    //     AudioPlayerManager.release()
    // }
}
```

## 性能优化

1. **复用实例**: Activity 内尽量复用 AudioPlayer 实例，避免频繁创建销毁
2. **使用单例**: 对于跨 Activity 的全局播放，使用 `AudioPlayerManager`
3. **及时释放**: Activity 中的实例必须在 `onDestroy()` 中释放
4. **低内存处理**: 在 `onLowMemory()` 和 `onTrimMemory()` 中释放单例

## 更新日志

### v0.0.2-dev8
- ✅ 首次发布
- ✅ 支持从字节数组播放
- ✅ 支持从文件播放
- ✅ 完整的播放控制功能
- ✅ 单例管理器

