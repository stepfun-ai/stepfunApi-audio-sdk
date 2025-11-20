package stepai.android.audio.common.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.COMMON_TAG
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logW
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logD
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logE
import java.io.File

interface RecordingCallback {
    /**
     * 录制开始
     */
    fun onStart()

    /**
     * 录制完成
     * @param file 录制的音频文件
     * @param duration 录制时长（毫秒）
     */
    fun onComplete(file: File, duration: Long)

    /**
     * 录制错误
     * @param error 错误信息
     */
    fun onError(error: String)
}

enum class AudioRecordFormat(val extension: String) {
    M4A("m4a"),  // MPEG-4 容器 + AAC 编码（推荐用于 ASR）
    AAC("aac"),  // 纯 AAC 流
}

class AudioRecorder(private val context: Context) {
    companion object {
        private const val TAG = COMMON_TAG + "AudioRecorder"
        private const val DEFAULT_SAMPLE_RATE = 16000  // 16kHz 适合语音识别
        private const val DEFAULT_BIT_RATE = 128000
    }

    private var isRecording = false
    private var outputFile: File? = null
    private var callback: RecordingCallback? = null
    private var mediaRecorder: MediaRecorder? = null
    private var startTime: Long = 0

    /**
     * 开始录制
     *
     * @param outputFile 输出文件，如果为null则自动创建临时文件
     * @param format 音频格式，默认 M4A（推荐用于 ASR）
     * @param sampleRate 采样率，默认 16000Hz（适合语音识别）
     * @param callback 录制回调
     * @return 成功返回true，失败返回false
     */
    fun startRecording(
        outputFile: File? = null,
        format: AudioRecordFormat = AudioRecordFormat.M4A,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        callback: RecordingCallback
    ): Boolean {
        if (isRecording) {
            "录制已在进行中".logW(TAG)
            return false
        }
        this.callback = callback
        return try {
            this.outputFile = outputFile ?: createTempAudioFile(format.extension)

            this.mediaRecorder = createMediaRecorder(format, sampleRate, this.outputFile!!)
            mediaRecorder?.start()

            isRecording = true
            startTime = System.currentTimeMillis()
            "开始录制: ${this.outputFile?.absolutePath}".logD(TAG)
            callback.onStart()

            true
        } catch (e: Exception) {
            val errorMsg = "开始录制失败: ${e.message}"
            errorMsg.logE(TAG)
            cleanup()
            callback.onError(errorMsg)
            false
        }
    }

    /**
     * 停止录制
     *
     * @return 录制的音频文件，失败返回null
     */
    fun stopRecording(): File? {
        if (!isRecording) {
            "当前未在录制".logW(TAG)
            return null
        }

        return try {
            mediaRecorder?.apply {
                stop()
                reset()
            }

            val duration = System.currentTimeMillis() - startTime
            val file = outputFile

            isRecording = false

            "录制已停止，时长: ${duration}ms，文件: ${file?.absolutePath}".logD(TAG)

            if (file != null && file.exists()) {
                callback?.onComplete(file, duration)
                file
            } else {
                callback?.onError("录制文件不存在")
                null
            }
        } catch (e: RuntimeException) {
            val errorMsg = "停止录制失败: ${e.message}"
            errorMsg.logE(TAG)
            callback?.onError(errorMsg)
            null
        } finally {
            cleanup()
        }
    }

    /**
     * 取消录制（停止并删除文件）
     */
    fun cancelRecording() {
        if (!isRecording) {
            "当前未在录制".logW(TAG)
            return
        }

        try {
            mediaRecorder?.apply {
                stop()
                reset()
            }

            // 删除录制的文件
            outputFile?.let {
                if (it.exists()) {
                    it.delete()
                    "录制文件已删除: ${it.absolutePath}".logD(TAG)
                }
            }

            isRecording = false

            "录制已取消".logD(TAG)
        } catch (e: Exception) {
            "取消录制失败: ${e.message}".logE(TAG)
        } finally {
            cleanup()
        }
    }

    /**
     * 获取录制时长
     *
     * @return 录制时长（毫秒）
     */
    fun getRecordingDuration(): Long {
        return if (isRecording) {
            System.currentTimeMillis() - startTime
        } else {
            0
        }
    }

    /**
     * 检查是否正在录制
     */
    fun isRecording(): Boolean {
        return isRecording
    }

    /**
     * 获取输出文件
     */
    fun getOutputFile(): File? {
        return outputFile
    }

    /**
     * 释放资源
     */
    fun release() {
        if (isRecording) {
            stopRecording()
        }
        cleanup()
        callback = null
        "录制器已释放".logD(TAG)
    }

    private fun createTempAudioFile(extension: String): File {
        val timestamp = System.currentTimeMillis()
        return File.createTempFile(
            "audio_record_${timestamp}",
            ".$extension",
            context.cacheDir
        )
    }

    /**
     * 创建 MediaRecorder 实例
     */
    private fun createMediaRecorder(
        format: AudioRecordFormat,
        sampleRate: Int,
        outputFile: File
    ): MediaRecorder {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return recorder.apply {
            // 音频源：语音识别优化
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)

            // 输出格式和编码器
            when (format) {
                AudioRecordFormat.M4A -> {
                    // M4A: MPEG-4 容器 + AAC 编码（推荐用于 ASR）
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }

                AudioRecordFormat.AAC -> {
                    // AAC: MPEG-4 容器 + AAC 编码（纯 AAC 流）
                    // 注意：Android MediaRecorder 的 AAC 实际上也是 MPEG-4 容器
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }
            }

            setAudioSamplingRate(sampleRate)
            setAudioEncodingBitRate(DEFAULT_BIT_RATE)
            setOutputFile(outputFile.absolutePath)

            // 设置错误监听器
            setOnErrorListener { _, what, extra ->
                val errorMsg = "录制错误: what=$what, extra=$extra"
                errorMsg.logE(TAG)
                callback?.onError(errorMsg)
            }

            prepare()
        }
    }

    /**
     * 清理资源
     */
    private fun cleanup() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            "释放录制器失败: ${e.message}".logE(TAG)
        }
    }
}