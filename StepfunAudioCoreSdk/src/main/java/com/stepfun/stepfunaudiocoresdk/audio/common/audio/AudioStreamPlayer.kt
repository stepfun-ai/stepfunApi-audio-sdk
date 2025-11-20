package com.stepfun.stepfunaudiocoresdk.audio.common.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import kotlinx.coroutines.*
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsAudioFormat
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsSampleRate
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.COMMON_TAG
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logD
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logE
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logW
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue


class AudioStreamPlayer(private val context: Context) {
    companion object {
        private const val TAG = COMMON_TAG + "AudioStreamPlayer"
    }

    private var audioTrack: AudioTrack? = null
    private var sampleRate = TtsSampleRate.RATE_24000.rate
    private var channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private var audioFormatEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val pcmAudioQueue = ConcurrentLinkedQueue<ByteArray>()
    private var isPcmPlaying = false
    private var pcmPlayJob: Job? = null

    private var audioFormat: TtsAudioFormat = TtsAudioFormat.PCM
    private var chunkQueue = ConcurrentLinkedQueue<ByteArray>()
    private var isChunkPlaying = false
    private var chunkPlayJob: Job? = null
    private var chunkCounter = 0

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 初始化播放器
     *
     * @param sampleRate 采样率
     * @param format 音频格式（pcm/mp3/wav/flac/opus）
     */
    fun initialize(sampleRate: Int = TtsSampleRate.RATE_24000.rate, format: TtsAudioFormat = TtsAudioFormat.PCM) {
        this.sampleRate = sampleRate
        this.audioFormat = format

        when (this.audioFormat) {
            TtsAudioFormat.PCM -> initPcmPlayer()
            else -> initChunkPlayer()
        }
        "AudioStreamPlayer initialized with format: $audioFormat, sampleRate: $sampleRate".logD(
            TAG,
            "AudioStreamPlayer initialized with format: $audioFormat, sampleRate: $sampleRate"
        )

    }

    /**
     * 初始化 PCM 播放器
     */
    private fun initPcmPlayer() {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormatEncoding
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(audioFormatEncoding)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        "PCM player initialized with buffer size: $bufferSize".logD(TAG, "PCM player initialized with buffer size: $bufferSize")
    }

    private fun initChunkPlayer() {
        // MediaPlayer 会在播放时动态创建
        "Chunk player initialized for format: $audioFormat".logD(TAG, "Chunk player initialized for format: $audioFormat")
    }

    /**
     * 添加音频数据到队列
     */
    fun addAudioData(data: ByteArray) {

        when (audioFormat) {
            TtsAudioFormat.PCM -> {
                pcmAudioQueue.offer(data)

                "AudioStreamPlayer added audio data to queue, queue size: ${pcmAudioQueue.size}".logD(
                    TAG,
                    "AudioStreamPlayer added audio data to queue, queue size: ${pcmAudioQueue.size}"
                )

                // 如果还没开始播放，启动播放
                if (!isPcmPlaying) {
                    startPcmPlayback()
                }
            }

            else -> {
                chunkQueue.offer(data)
                "Added $audioFormat chunk, queue size: ${chunkQueue.size}".logD(TAG, "Added $audioFormat chunk, queue size: ${chunkQueue.size}")
                if (!isChunkPlaying) {
                    startChunkPlayback()
                }
            }
        }
    }


    // pcm 流式播放
    private fun startPcmPlayback() {
        if (isPcmPlaying) {
            "AudioStreamPlayer is already playing".logD(TAG, "AudioStreamPlayer is already playing")
            return
        }

        isPcmPlaying = true
        audioTrack?.play()
        "AudioStreamPlayer started playing".logD(TAG, "AudioStreamPlayer started playing")

        pcmPlayJob = scope.launch {
            while (isActive && isPcmPlaying) {
                val data = pcmAudioQueue.poll()
                if (data != null) {
                    audioTrack?.write(data, 0, data.size)
                } else {
                    delay(10) // 等待新数据
                }
            }
        }
    }

    // 分片播放（用于 MP3/WAV/FLAC/OPUS）
    private fun startChunkPlayback() {
        if (isChunkPlaying) {
            "Chunk player is already playing".logD(TAG, "Chunk player is already playing")
            return
        }
        isChunkPlaying = true
        "Chunk playback started".logD(TAG, "Chunk playback started")
        chunkPlayJob = scope.launch {
            while (isActive && isChunkPlaying) {
                val chunkData = chunkQueue.poll()
                if (chunkData != null) {
                    playChunk(chunkData)
                } else {
                    delay(50)
                }
            }
        }
    }

    private suspend fun playChunk(chunkData: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile(
                "audio_chunk_${chunkCounter++}",
                ".${audioFormat.format}",
                context.cacheDir
            )

            FileOutputStream(tempFile).use {
                it.write(chunkData)
            }

            "Playing chunk: ${tempFile.name}, size: ${chunkData.size}".logD(TAG, "Playing chunk: ${tempFile.name}, size: ${chunkData.size}")

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepare()
            }

            suspendCancellableCoroutine<Unit> { continuation ->
                player.setOnCompletionListener {
                    "Chunk playback completed: ${tempFile.name}".logD(TAG, "Chunk playback completed: ${tempFile.name}")
                    player.release()
                    tempFile.delete()
                    continuation.resume(Unit) {}
                }
                player.setOnErrorListener { mp, what, extra ->
                    "Chunk playback error: what=$what, extra=$extra".logD(TAG, "Chunk playback error: what=$what, extra=$extra")
                    mp.release()
                    tempFile.delete()
                    continuation.resume(Unit) { }
                    true
                }
                player.start()
            }
        } catch (e: Exception) {
            "Error playing chunk: ${e.message}".logD(TAG, "Error playing chunk: ${e.message}")
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        "Stopping playback".logD(TAG, "Stopping playback")

        isPcmPlaying = false
        pcmPlayJob?.cancel()
        audioTrack?.stop()
        pcmAudioQueue.clear()

        isChunkPlaying = false
        chunkPlayJob?.cancel()
        cleanupTempFiles()
    }

    /**
     * 释放资源
     */
    fun release() {
        "AudioStreamPlayer released resources".logD(TAG, "AudioStreamPlayer released resources")
        stop()
        audioTrack?.release()
        audioTrack = null
        scope.cancel()
    }

    /**
     * 清理临时文件
     */
    private fun cleanupTempFiles() {
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("audio_chunk_")) {
                    file.delete()
                    "Deleted temp file: ${file.name}".logD(TAG, "Deleted temp file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            "Error cleaning temp files: ${e.message}".logD(TAG, "Error cleaning temp files: ${e.message}")
        }
    }

}