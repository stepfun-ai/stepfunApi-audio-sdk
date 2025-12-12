package com.stepfun.stepfunaudiocoresdk.audio.common.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsAudioFormat
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsSampleRate
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.COMMON_TAG
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logD
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logI
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.*
import kotlin.concurrent.Volatile

class AudioStreamPlayerV2(private val context: Context) {
    companion object {
        private const val TAG = COMMON_TAG + "AudioStreamPlayerV2"
    }

    // ========== PCM 播放相关 ==========
    private var audioTrack: AudioTrack? = null
    private var sampleRate = TtsSampleRate.RATE_24000.rate
    private var channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private var audioFormatEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val pcmAudioQueue = ConcurrentLinkedQueue<ByteArray>()
    private var isPcmPlaying = false
    private var pcmPlayJob: Job? = null

    @Volatile
    private var isPaused = false

    @Volatile
    private var isStopped = false

    // ========== ExoPlayer Chunk 播放相关 ==========
    private var audioFormat: TtsAudioFormat = TtsAudioFormat.PCM
    private var chunkQueue = ConcurrentLinkedQueue<ByteArray>()
    private var isChunkPlaying = false

    // ExoPlayer 实例
    private var exoPlayer: ExoPlayer? = null

    @Volatile
    private var isExoPlayerBusy = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var playbackCallback: AudioPlaybackCallback? = null

    // 用于检测播放完成的标记
    @Volatile
    private var isStreamFinished = false  // 流是否已结束（不再有新数据）

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
        isStopped = false
        isStreamFinished = false

        when (this.audioFormat) {
            TtsAudioFormat.PCM -> initPcmPlayer()
            else -> initExoPlayer()
        }
        "AudioStreamPlayer initialized with format: $audioFormat, sampleRate: $sampleRate".logI(TAG)
    }

    /**
     * 设置播放状态回调
     */
    fun setPlaybackCallback(callback: AudioPlaybackCallback?) {
        this.playbackCallback = callback
    }

    /**
     * 标记流结束，用于检测播放完成
     */
    fun markStreamFinished() {
        isStreamFinished = true
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

    /** 初始化 ExoPlayer */
    private fun initExoPlayer() {
        mainHandler.post {
            releaseExoPlayer()
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(exoPlayerListener)
            }
            "ExoPlayer initialized for format: $audioFormat".logD(TAG)
        }
    }

    /** ExoPlayer 事件监听器 */
    private val exoPlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    "ExoPlayer chunk playback ended".logD(TAG)
                    isExoPlayerBusy = false
                    // 播放完成后，尝试播放下一个 chunk
                    playNextChunkIfAvailable()
                }

                Player.STATE_READY -> {
                    "ExoPlayer ready".logD(TAG)
                }

                Player.STATE_BUFFERING -> {
                    "ExoPlayer buffering".logD(TAG)
                }

                Player.STATE_IDLE -> {
                    "ExoPlayer idle".logD(TAG)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            "ExoPlayer error: ${error.message}".logD(TAG)
            isExoPlayerBusy = false
            // 出错后尝试播放下一个
            playNextChunkIfAvailable()
        }
    }

    /** 添加音频数据到队列 */
    fun addAudioData(data: ByteArray) {

        if (isStopped) {
            "AudioStreamPlayer is stopped, ignoring audio data".logD(TAG)
            return
        }

        if (data.isEmpty()) {
            "Ignoring empty audio data".logD(TAG)
            return
        }
        when (audioFormat) {
            TtsAudioFormat.PCM -> {
                pcmAudioQueue.offer(data)
                "AudioStreamPlayer added PCM data, size: ${data.size}, queue size: ${pcmAudioQueue.size}".logD(
                    TAG
                )

                if (!isPcmPlaying) {
                    startPcmPlayback()
                }
            }

            else -> {
                chunkQueue.offer(data)
                "AudioStreamPlayer added chunk data, size: ${data.size}, queue size: ${chunkQueue.size}".logD(
                    TAG
                )

                if (!isChunkPlaying) {
                    isChunkPlaying = true
                    playNextChunkIfAvailable()
                }
            }
        }
    }

    // ========== PCM 流式播放 ==========
    private fun startPcmPlayback() {
        if (isPcmPlaying) {
            "AudioStreamPlayer is already playing".logD(TAG)
            return
        }

        isPcmPlaying = true
        audioTrack?.play()
        isPaused = false
        "AudioStreamPlayer started playing".logD(TAG)

        mainHandler.post { playbackCallback?.onPlaybackStarted() }

        pcmPlayJob =
            scope.launch {
                while (isActive && isPcmPlaying) {
                    if (isPaused) {
                        if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            audioTrack?.pause()
                            "AudioTrack paused".logD(TAG)
                        }
                        delay(100)
                        continue
                    } else {
                        if (audioTrack?.playState == AudioTrack.PLAYSTATE_PAUSED) {
                            audioTrack?.play()
                            "AudioTrack resumed".logD(TAG)
                        }
                    }

                    val data = pcmAudioQueue.poll()
                    if (data != null) {
                        audioTrack?.write(data, 0, data.size)
                    } else {
                        // 队列为空
                        if (isStreamFinished) {
                            // 流已结束且队列为空，播放完成
                            "Playback completed".logD(TAG)
                            isPcmPlaying = false
                            mainHandler.post { playbackCallback?.onPlaybackCompleted() }
                            break
                        }
                        delay(10)
                    }
                }
            }
    }

    // ========== ExoPlayer Chunk 播放 ==========
    /** 尝试播放队列中的下一个 chunk */
    private fun playNextChunkIfAvailable() {
        if (isExoPlayerBusy || isPaused) {
            return
        }

        val chunkData = chunkQueue.poll()
        if (chunkData != null) {
            playChunkWithExoPlayer(chunkData)
        } else {
            // 队列为空，等待新数据
            scope.launch {
                delay(20)
                if (chunkQueue.isNotEmpty() && !isExoPlayerBusy && !isPaused) {
                    mainHandler.post { playNextChunkIfAvailable() }
                }
            }
        }
    }

    /**
     * 使用 ExoPlayer 播放内存中的音频数据
     * 核心优化：直接从 ByteArray 播放，无需写入临时文件
     */
    @OptIn(UnstableApi::class)
    private fun playChunkWithExoPlayer(audioData: ByteArray) {
        mainHandler.post {
            val player = exoPlayer
            if (player == null) {
                "ExoPlayer is null, cannot play chunk".logD(TAG)
                isExoPlayerBusy = false
                return@post
            }

            try {
                isExoPlayerBusy = true

                // 1. 创建 ByteArrayDataSource - 直接从内存读取
                val dataSource = ByteArrayDataSource(audioData)

                // 2. 创建 DataSource.Factory
                val dataSourceFactory = DataSource.Factory { dataSource }

                // 3. 创建 ProgressiveMediaSource
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(Uri.EMPTY))

                // 4. 设置并播放
                player.setMediaSource(mediaSource)
                player.prepare()
                player.play()

                "Playing chunk with ExoPlayer, size: ${audioData.size} bytes".logD(TAG)
            } catch (e: Exception) {
                "Error playing chunk with ExoPlayer: ${e.message}".logD(TAG)
                isExoPlayerBusy = false
                // 出错后尝试下一个
                playNextChunkIfAvailable()
            }
        }
    }

    /** 停止播放 */
    fun stop() {
        "Stopping playback".logD(TAG)

        isPaused = false
        isStopped = true

        // PCM 播放停止
        isPcmPlaying = false
        pcmPlayJob?.cancel()
        audioTrack?.stop()
        pcmAudioQueue.clear()

        // Chunk 播放停止
        isChunkPlaying = false
        isExoPlayerBusy = false
        chunkQueue.clear()

        // 停止 ExoPlayer
        mainHandler.post {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            playbackCallback?.onPlaybackStopped()
        }
    }

    /** 暂停播放 */
    fun pause() {
        "Pausing playback".logD(TAG)
        isPaused = true
        mainHandler.post {
            exoPlayer?.pause()
            playbackCallback?.onPlaybackPaused()
        }
    }

    /** 恢复播放 */
    fun resume() {
        "Resuming playback".logD(TAG)
        isPaused = false
        mainHandler.post {
            playbackCallback?.onPlaybackResumed()
            // 如果 ExoPlayer 有正在播放的内容，恢复它
            if (exoPlayer?.playbackState == Player.STATE_READY ||
                exoPlayer?.playbackState == Player.STATE_BUFFERING
            ) {
                exoPlayer?.play()
            } else {
                // 否则尝试播放队列中的下一个
                playNextChunkIfAvailable()
            }
        }
    }

    /** 释放 ExoPlayer 资源 */
    private fun releaseExoPlayer() {
        exoPlayer?.removeListener(exoPlayerListener)
        exoPlayer?.release()
        exoPlayer = null
        isExoPlayerBusy = false
    }

    /** 释放资源 */
    fun release() {
        "AudioStreamPlayer released resources".logD(TAG)
        stop()

        audioTrack?.release()
        audioTrack = null

        mainHandler.post {
            releaseExoPlayer()
        }

        scope.cancel()
    }
}