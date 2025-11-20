package com.stepfun.stepfunaudiocoresdk.audio.common.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logD
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logE
import java.io.File
import java.io.IOException

/**
 * 音频播放器
 * 提供音频播放、暂停、停止、释放等功能
 */
class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentTempFile: File? = null
    private var callback: PlaybackCallback? = null

    companion object {
        private const val TAG = "AudioPlayer"
    }

    /**
     * 播放回调接口
     */
    interface PlaybackCallback {
        /**
         * 播放准备完成
         */
        fun onPrepared() {}

        /**
         * 播放开始
         */
        fun onStart() {}

        /**
         * 播放完成
         */
        fun onCompletion() {}

        /**
         * 播放错误
         */
        fun onError(error: String, exception: Exception?) {}

        /**
         * 播放进度更新（可选实现）
         * @param current 当前播放位置（毫秒）
         * @param duration 总时长（毫秒）
         */
        fun onProgressUpdate(current: Int, duration: Int) {}
    }

    /**
     * 从字节数组播放音频
     * 
     * 此方法内部自动在后台线程处理 IO 操作，无需在协程中调用
     * 
     * @param audioData 音频数据
     * @param fileExtension 文件扩展名（如 "mp3", "wav"）
     * @param callback 播放回调
     */
    fun playFromByteArray(
        audioData: ByteArray,
        fileExtension: String = "mp3",
        callback: PlaybackCallback? = null
    ) {
        this.callback = callback

        // 在后台线程执行 IO 操作
        Thread {
            try {
                // 停止当前播放
                stop()

                // 创建临时文件
                val tempFile = File.createTempFile(
                    "audio_temp",
                    ".$fileExtension",
                    context.cacheDir
                ).apply {
                    writeBytes(audioData)
                }
                currentTempFile = tempFile

                // 播放文件（会在主线程回调）
                playFromFile(tempFile.absolutePath)

            } catch (e: Exception) {
                "播放失败: ${e.message}".logE(TAG, "播放失败: ${e.message}")
                callback?.onError("播放失败: ${e.message}", e)
            }
        }.start()
    }

    /**
     * 从文件路径播放音频
     * @param filePath 文件路径
     */
    fun playFromFile(filePath: String) {
        try {
            // 释放旧的播放器
            releaseMediaPlayer()

            // 创建新的播放器
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setOnPreparedListener {
                    "播放器准备完成".logD(TAG, "播放器准备完成")
                    callback?.onPrepared()
                }

                setOnCompletionListener {
                    "播放完成".logD(TAG, "播放完成")
                    callback?.onCompletion()
                    cleanup()
                }

                setOnErrorListener { mp, what, extra ->
                    "播放器错误: what=$what, extra=$extra".logE(TAG, "播放器错误: what=$what, extra=$extra")
                    callback?.onError("播放器错误: what=$what, extra=$extra", null)
                    cleanup()
                    true
                }

                setDataSource(filePath)
                prepare()
                start()

                callback?.onStart()
                "开始播放: $filePath".logD(TAG, "开始播放: $filePath")
            }

        } catch (e: IOException) {
            "播放文件失败: ${e.message}".logE(TAG, "播放文件失败: ${e.message}")
            callback?.onError("播放文件失败: ${e.message}", e)
        } catch (e: IllegalStateException) {
            "播放器状态异常: ${e.message}".logE(TAG, "播放器状态异常: ${e.message}")
            callback?.onError("播放器状态异常: ${e.message}", e)
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    "播放已暂停".logD(TAG, "播放已暂停")
                }
            }
        } catch (e: IllegalStateException) {
            "暂停失败: ${e.message}".logE(TAG, "暂停失败: ${e.message}")
        }
    }

    /**
     * 恢复播放
     */
    fun resume() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    "播放已恢复".logD(TAG, "播放已恢复")
                }
            }
        } catch (e: IllegalStateException) {
            "恢复播放失败: ${e.message}".logE(TAG, "恢复播放失败: ${e.message}")
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                    "播放已停止".logD(TAG, "播放已停止")
                }
            }
            cleanup()
        } catch (e: IllegalStateException) {
            "停止失败: ${e.message}".logE(TAG, "停止失败: ${e.message}")
        }
    }

    /**
     * 跳转到指定位置
     * @param position 位置（毫秒）
     */
    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
        } catch (e: IllegalStateException) {
            "跳转失败: ${e.message}".logE(TAG, "跳转失败: ${e.message}")
        }
    }

    /**
     * 检查是否正在播放
     */
    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * 获取当前播放位置
     * @return 当前位置（毫秒）
     */
    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: IllegalStateException) {
            0
        }
    }

    /**
     * 获取音频总时长
     * @return 总时长（毫秒）
     */
    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: IllegalStateException) {
            0
        }
    }

    /**
     * 设置音量
     * @param leftVolume 左声道音量 (0.0-1.0)
     * @param rightVolume 右声道音量 (0.0-1.0)
     */
    fun setVolume(leftVolume: Float, rightVolume: Float) {
        try {
            mediaPlayer?.setVolume(leftVolume, rightVolume)
        } catch (e: IllegalStateException) {
            "设置音量失败: ${e.message}".logE(TAG, "设置音量失败: ${e.message}")
        }
    }

    /**
     * 设置是否循环播放
     * @param looping true-循环播放，false-播放一次
     */
    fun setLooping(looping: Boolean) {
        try {
            mediaPlayer?.isLooping = looping
        } catch (e: IllegalStateException) {
            "设置循环失败: ${e.message}".logE(TAG, "设置循环失败: ${e.message}")
        }
    }

    /**
     * 释放播放器资源
     */
    fun release() {
        cleanup()
        callback = null
        "播放器已释放".logD(TAG, "播放器已释放")
    }

    /**
     * 清理资源
     */
    private fun cleanup() {
        releaseMediaPlayer()
        deleteTempFile()
    }

    /**
     * 释放MediaPlayer
     */
    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            "释放播放器失败: ${e.message}".logE(TAG, "释放播放器失败: ${e.message}")
        }
    }

    /**
     * 删除临时文件
     */
    private fun deleteTempFile() {
        try {
            currentTempFile?.let {
                if (it.exists()) {
                    it.delete()
                    "临时文件已删除: ${it.absolutePath}".logD(TAG, "临时文件已删除: ${it.absolutePath}")
                }
            }
            currentTempFile = null
        } catch (e: Exception) {
            "删除临时文件失败: ${e.message}".logE(TAG, "删除临时文件失败: ${e.message}")
        }
    }
}

/**
 * 音频播放器管理器（单例）
 * 提供全局的音频播放器实例
 * 
 * 注意：
 * 1. 单例播放器会在应用进程结束时自动被系统回收，通常不需要手动释放
 * 2. 如需在低内存时释放资源，可在 Application.onLowMemory() 或 onTrimMemory() 中调用 release()
 * 3. Activity 中使用的 AudioPlayer 实例应该在 onDestroy() 中手动释放
 */
object AudioPlayerManager {
    
    @Volatile
    private var instance: AudioPlayer? = null
    
    /**
     * 获取音频播放器实例
     * @param context 上下文
     */
    fun getInstance(context: Context): AudioPlayer {
        return instance ?: synchronized(this) {
            instance ?: AudioPlayer(context.applicationContext).also {
                instance = it
            }
        }
    }
    
    /**
     * 释放全局播放器
     * 
     * 使用场景：
     * - 在 Application.onLowMemory() 中调用以释放内存
     * - 在 Application.onTrimMemory() 中根据内存级别调用
     * - 应用退出时不需要调用（系统会自动回收）
     */
    fun release() {
        synchronized(this) {
            instance?.release()
            instance = null
        }
    }
    
    /**
     * 检查播放器是否已初始化
     */
    fun isInitialized(): Boolean {
        return instance != null
    }
    
    /**
     * 停止播放但不释放播放器
     * 适用于需要暂时停止播放但后续可能继续使用的场景
     */
    fun stop() {
        instance?.stop()
    }
}

