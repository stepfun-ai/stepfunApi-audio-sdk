package com.stepfun.stepfunaudiocoresdk.audio.common.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsAudioFormat
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsSampleRate
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.COMMON_TAG
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logD
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.*
import kotlin.concurrent.Volatile

class AudioStreamPlayer(private val context: Context) {
    companion object {
        private const val TAG = COMMON_TAG + "AudioStreamPlayer"
        
        // 数据累积策略：最小累积大小（32KB）
        private const val MIN_CHUNK_SIZE = 32 * 1024
    }

    private var audioTrack: AudioTrack? = null
    private var sampleRate = TtsSampleRate.RATE_24000.rate
    private var channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private var audioFormatEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val pcmAudioQueue = ConcurrentLinkedQueue<ByteArray>()
    private var isPcmPlaying = false
    private var pcmPlayJob: Job? = null

    @Volatile
    private var isPaused = false

    private var audioFormat: TtsAudioFormat = TtsAudioFormat.PCM
    private var chunkQueue = ConcurrentLinkedQueue<ByteArray>()
    private var isChunkPlaying = false
    private var chunkPlayJob: Job? = null
    private var chunkCounter = 0

    // 方案2: 数据累积策略
    private val accumulatedData = mutableListOf<ByteArray>()
    private var accumulatedSize = 0
    private val accumulateLock = Object()

    // 方案1: 双缓冲播放器
    private var currentPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    private var currentTempFile: File? = null
    private var nextTempFile: File? = null
    private var isNextPlayerReady = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 初始化播放器
     *
     * @param sampleRate 采样率
     * @param format 音频格式（pcm/mp3/wav/flac/opus）
     */
    fun initialize(
        sampleRate: Int = TtsSampleRate.RATE_24000.rate,
        format: TtsAudioFormat = TtsAudioFormat.PCM
    ) {
        this.sampleRate = sampleRate
        this.audioFormat = format

        // 重置累积数据
        synchronized(accumulateLock) {
            accumulatedData.clear()
            accumulatedSize = 0
        }

        when (this.audioFormat) {
            TtsAudioFormat.PCM -> initPcmPlayer()
            else -> initChunkPlayer()
        }
        "AudioStreamPlayer initialized with format: $audioFormat, sampleRate: $sampleRate".logD(TAG)
    }

    /** 初始化 PCM 播放器 */
    private fun initPcmPlayer() {
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormatEncoding)

        audioTrack =
            AudioTrack.Builder()
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
        "PCM player initialized with buffer size: $bufferSize".logD(TAG)
    }

    private fun initChunkPlayer() {
        // MediaPlayer 会在播放时动态创建
        "Chunk player initialized for format: $audioFormat".logD(TAG)
    }

    /** 添加音频数据到队列 */
    fun addAudioData(data: ByteArray) {
        when (audioFormat) {
            TtsAudioFormat.PCM -> {
                pcmAudioQueue.offer(data)

                "AudioStreamPlayer added audio data to queue, queue size: ${pcmAudioQueue.size}".logD(
                    TAG
                )

                // 如果还没开始播放，启动播放
                if (!isPcmPlaying) {
                    startPcmPlayback()
                }
            }

            else -> {
                // 方案2: 数据累积策略
                synchronized(accumulateLock) {
                    accumulatedData.add(data)
                    accumulatedSize += data.size
                    "Accumulated data: ${data.size} bytes, total: $accumulatedSize bytes".logD(TAG)

                    // 累积到一定大小后再放入播放队列
                    if (accumulatedSize >= MIN_CHUNK_SIZE) {
                        val mergedData = mergeAccumulatedChunks()
                        chunkQueue.offer(mergedData)
                        "Merged chunk added to queue, size: ${mergedData.size}, queue size: ${chunkQueue.size}".logD(TAG)
                        accumulatedData.clear()
                        accumulatedSize = 0
                    }
                }

                if (!isChunkPlaying) {
                    startChunkPlayback()
                }
            }
        }
    }

    /**
     * 刷新剩余的累积数据到播放队列
     * 在流结束时调用，确保所有数据都被播放
     */
    fun flushRemainingData() {
        synchronized(accumulateLock) {
            if (accumulatedSize > 0) {
                val mergedData = mergeAccumulatedChunks()
                chunkQueue.offer(mergedData)
                "Flushed remaining data to queue, size: ${mergedData.size}".logD(TAG)
                accumulatedData.clear()
                accumulatedSize = 0
            }
        }
    }

    /** 合并累积的数据块 */
    private fun mergeAccumulatedChunks(): ByteArray {
        val totalSize = accumulatedData.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (chunk in accumulatedData) {
            System.arraycopy(chunk, 0, result, offset, chunk.size)
            offset += chunk.size
        }
        return result
    }

    // pcm 流式播放
    private fun startPcmPlayback() {
        if (isPcmPlaying) {
            "AudioStreamPlayer is already playing".logD(TAG)
            return
        }

        isPcmPlaying = true
        audioTrack?.play()
        isPaused = false
        "AudioStreamPlayer started playing".logD(TAG)

        pcmPlayJob =
            scope.launch {
                while (isActive && isPcmPlaying) {
                    if (isPaused) {
                        // 仅当正在播放时才暂停
                        if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            audioTrack?.pause()
                            "AudioTrack paused".logD(TAG)
                        }
                        delay(100)
                        continue
                    } else {
                        // 如果之前暂停了，现在需要恢复播放
                        if (audioTrack?.playState == AudioTrack.PLAYSTATE_PAUSED) {
                            audioTrack?.play()
                            "AudioTrack resumed".logD(TAG)
                        }
                    }

                    val data = pcmAudioQueue.poll()
                    if (data != null) {
                        audioTrack?.write(data, 0, data.size)
                    } else {
                        delay(10) // 等待新数据
                    }
                }
            }
    }

    // 方案1: 双缓冲分片播放（用于 MP3/WAV/FLAC/OPUS）
    private fun startChunkPlayback() {
        if (isChunkPlaying) {
            "Chunk player is already playing".logD(TAG)
            return
        }
        isChunkPlaying = true
        "Chunk playback started with double buffering".logD(TAG)

        chunkPlayJob = scope.launch {
            // 预加载第一个 chunk
            prepareNextPlayer()

            while (isActive && isChunkPlaying) {
                if (isNextPlayerReady && nextPlayer != null) {
                    // 切换: next -> current
                    currentPlayer?.release()
                    currentTempFile?.delete()

                    currentPlayer = nextPlayer
                    currentTempFile = nextTempFile
                    nextPlayer = null
                    nextTempFile = null
                    isNextPlayerReady = false

                    // 并行：播放当前 chunk 的同时，异步准备下一个
                    val playJob = async { playCurrentChunk() }
                    val prepareJob = async { prepareNextPlayer() }

                    // 等待播放完成
                    playJob.await()

                    // 确保下一个 chunk 准备好（如果还没准备好的话）
                    prepareJob.await()
                } else if (chunkQueue.isEmpty() && accumulatedSize == 0) {
                    // 队列为空且没有累积数据，等待新数据或结束
                    delay(20)
                } else {
                    // 有数据但还没准备好，尝试准备
                    prepareNextPlayer()
                    delay(10)
                }
            }

            // 清理资源
            currentPlayer?.release()
            currentPlayer = null
            currentTempFile?.delete()
            currentTempFile = null
            nextPlayer?.release()
            nextPlayer = null
            nextTempFile?.delete()
            nextTempFile = null
        }
    }

    /** 准备下一个播放器（双缓冲的核心） */
    private suspend fun prepareNextPlayer() = withContext(Dispatchers.IO) {
        if (isNextPlayerReady || nextPlayer != null) {
            return@withContext
        }

        val chunkData = chunkQueue.poll()
        if (chunkData == null) {
            return@withContext
        }

        try {
            val tempFile = File.createTempFile(
                "audio_chunk_${chunkCounter++}",
                ".${audioFormat.format}",
                context.cacheDir
            )

            FileOutputStream(tempFile).use { it.write(chunkData) }

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepare()  // 在后台线程同步准备
            }

            nextPlayer = player
            nextTempFile = tempFile
            isNextPlayerReady = true
            "Prepared next chunk: ${tempFile.name}, size: ${chunkData.size}".logD(TAG)
        } catch (e: Exception) {
            "Error preparing next chunk: ${e.message}".logD(TAG)
        }
    }

    /** 播放当前准备好的 chunk */
    private suspend fun playCurrentChunk() = suspendCancellableCoroutine<Unit> { continuation ->
        val player = currentPlayer
        if (player == null) {
            continuation.resume(Unit) {}
            return@suspendCancellableCoroutine
        }

        player.setOnCompletionListener {
            "Chunk playback completed: ${currentTempFile?.name}".logD(TAG)
            continuation.resume(Unit) {}
        }

        player.setOnErrorListener { _, what, extra ->
            "Chunk playback error: what=$what, extra=$extra".logD(TAG)
            continuation.resume(Unit) {}
            true
        }

        continuation.invokeOnCancellation {
            try {
                player.stop()
            } catch (e: Exception) {
                // ignore
            }
        }

        try {
            player.start()
            "Started playing chunk: ${currentTempFile?.name}".logD(TAG)
        } catch (e: Exception) {
            "Error starting playback: ${e.message}".logD(TAG)
            continuation.resume(Unit) {}
        }
    }

    /** 停止播放 */
    fun stop() {
        "Stopping playback".logD(TAG)

        isPaused = false
        // PCM 播放停止
        isPcmPlaying = false
        pcmPlayJob?.cancel()
        audioTrack?.stop()
        pcmAudioQueue.clear()

        // Chunk 播放停止
        isChunkPlaying = false
        chunkPlayJob?.cancel()

        // 释放双缓冲播放器
        currentPlayer?.release()
        currentPlayer = null
        currentTempFile?.delete()
        currentTempFile = null

        nextPlayer?.release()
        nextPlayer = null
        nextTempFile?.delete()
        nextTempFile = null
        isNextPlayerReady = false

        // 清理累积数据
        synchronized(accumulateLock) {
            accumulatedData.clear()
            accumulatedSize = 0
        }
        chunkQueue.clear()

        cleanupTempFiles()
    }

    /** 暂停播放 */
    fun pause() {
        "Pausing playback".logD(TAG)
        isPaused = true
    }

    /** 恢复播放 */
    fun resume() {
        "Resuming playback".logD(TAG)
        isPaused = false
    }

    /** 释放资源 */
    fun release() {
        "AudioStreamPlayer released resources".logD(TAG)
        stop()
        audioTrack?.release()
        audioTrack = null
        scope.cancel()
    }

    /** 清理临时文件 */
    private fun cleanupTempFiles() {
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("audio_chunk_")) {
                    file.delete()
                    "Deleted temp file: ${file.name}".logD(TAG)
                }
            }
        } catch (e: Exception) {
            "Error cleaning temp files: ${e.message}".logD(TAG)
        }
    }
}
