package com.stepfun.stepfunaudiosdk

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stepfun.stepfunaudiocoresdk.audio.common.audio.AudioPlaybackCallback
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsAudioFormat
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsModel
import com.stepfun.stepfunaudiocoresdk.audio.common.config.TtsVoice
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.COMMON_TAG
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logI
import com.stepfun.stepfunaudiocoresdk.audio.core.Environment
import com.stepfun.stepfunaudiocoresdk.audio.core.SpeechConfig
import com.stepfun.stepfunaudiosdk.SpeechSdk
import com.stepfun.stepfunaudiottssdk.tts.TtsCallback
import com.stepfun.stepfunaudiottssdk.tts.TtsError
import com.stepfun.stepfunaudiottssdk.tts.callbacks.TtsStreamCallback
import com.stepfun.stepfunaudiottssdk.tts.callbacks.TtsStreamError
import com.stepfun.stepfunaudiottssdk.tts.configs.TtsStreamParams
import com.stepfun.stepfunaudiottssdk.tts.event.TtsCreateEvent

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DEMO"
    }

    private lateinit var etText: EditText
    private lateinit var btnPlay: Button
    private lateinit var btnStop: Button
    private lateinit var btnStreamPlay: Button
    private lateinit var btnPauseStream: Button
    private lateinit var btnResumeStream: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 简单的 UI 布局
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 200, 50, 50)
        }

        etText = EditText(this).apply {
            hint = "请输入要合成的文本"
            setText("你好，我是阶跃星辰语音助手，很高兴为你服务。你好，我是阶跃星辰语音助手，很高兴为你服务。你好，我是阶跃星辰语音助手，很高兴为你服务。")
        }

        btnPlay = Button(this).apply {
            text = "播放 TTS (非流式)"
        }

        btnStop = Button(this).apply {
            text = "停止播放"
        }

        btnStreamPlay = Button(this).apply {
            text = "播放 TTS (流式)"
        }

        btnPauseStream = Button(this).apply {
            text = "暂停流式播放"
        }

        btnResumeStream = Button(this).apply {
            text = "恢复流式播放"
        }

        layout.addView(etText)
        layout.addView(btnPlay)
        layout.addView(btnStop)
        layout.addView(btnStreamPlay)
        layout.addView(btnPauseStream)
        layout.addView(btnResumeStream)
        setContentView(layout)

        // 2. 初始化 SDK
        initSdk()

        // 3. 设置点击事件
        btnPlay.setOnClickListener {
            val text = etText.text.toString()
            if (text.isNotEmpty()) {
                playTts(text)
            } else {
                Toast.makeText(this, "请输入文本", Toast.LENGTH_SHORT).show()
            }
        }

        btnStop.setOnClickListener {
            SpeechSdk.TTS.stopPlayback()
            SpeechSdk.TTS.stopStream()
            Toast.makeText(this, "已停止播放", Toast.LENGTH_SHORT).show()
        }

        btnStreamPlay.setOnClickListener {
            val text = etText.text.toString()
            if (text.isNotEmpty()) {
                playStreamTts(text)
            } else {
                Toast.makeText(this, "请输入文本", Toast.LENGTH_SHORT).show()
            }
        }

        btnPauseStream.setOnClickListener {
            SpeechSdk.TTS.pauseStream()
            Toast.makeText(this, "已暂停流式播放", Toast.LENGTH_SHORT).show()
        }

        btnResumeStream.setOnClickListener {
            SpeechSdk.TTS.resumeStream()
            Toast.makeText(this, "已恢复流式播放", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initSdk() {
        // 创建配置
        // 注意：httpBaseUrl 和 webSocketUrl 请根据实际文档填写
        val config = SpeechConfig.Builder()
            .environment(Environment.PRODUCTION)
            .httpBaseUrl("https://k3s-tts.stepfun-inc.net/")
            .webSocketUrl("wss://k3s-tts.stepfun-inc.net/openapi/v1/realtime/audio")
            .customHeaders(mapOf("header_keys" to "header_values"))
            .enableLogging(true)
            .build()

        // 初始化核心 SDK
        SpeechSdk.init(this, config)
    }

    private fun playTts(text: String) {
        btnPlay.isEnabled = false
        btnPlay.text = "生成中..."

        // 使用 quickPlay 快速生成并播放
        SpeechSdk.TTS.quickPlay(
            context = this,
            text = text,
            voice = TtsVoice.STEP_TTS_MINI_WENROUNANSHENG,
            callback = object : TtsCallback {
                override fun onSuccess(audioData: ByteArray) {
                    runOnUiThread {
                        btnPlay.isEnabled = true
                        btnPlay.text = "播放 TTS (非流式)"
                        Toast.makeText(
                            this@MainActivity,
                            "非流式生成成功，正在播放",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(error: TtsError) {
                    runOnUiThread {
                        btnPlay.isEnabled = true
                        btnPlay.text = "播放 TTS (非流式)"
                        Toast.makeText(
                            this@MainActivity,
                            "错误: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    private fun playStreamTts(text: String) {
        btnStreamPlay.isEnabled = false
        btnStreamPlay.text = "流式连接中..."

        val params = TtsStreamParams.Builder().model(TtsModel.STEP_TTS_MINI.modelId)
            .voice(TtsVoice.STEP_TTS_MINI_CIXINGNANSHENG.voiceId).responseFormat(TtsAudioFormat.PCM)
            .mode("sentence").features(
            TtsCreateEvent.Features(
                enableMarkdownFilter = true
            )
        ).build()

        // 1. 创建流式会话
        SpeechSdk.TTS.createStreamSession(
            context = this,
            params = params,
            callback = object : TtsStreamCallback {
                override fun onConnectionEstablished(sessionId: String) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "连接成功", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onSessionCreated(sessionId: String) {
                    runOnUiThread {
                        btnStreamPlay.text = "发送文本..."
//                         2. 会话创建成功后，发送文本
                        SpeechSdk.TTS.sendStreamText(text)
//                         3. 发送完毕后，通知结束（如果是单句播放）
                        SpeechSdk.TTS.finishStream()
                    }
                }

                override fun onSentenceStart(text: String) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "开始播放: $text", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onAudioData(audioData: ByteArray, isFinished: Boolean) {
                    // SDK 内部会自动播放音频，这里只需要监听即可
                }

                override fun onSentenceEnd(text: String) {
                    // 单句结束
                }

                override fun onFlushed() {
                    // 缓冲区已清空
                }

                override fun onComplete() {
                    "流式数据传输完成".logI(COMMON_TAG)
                    runOnUiThread {
                        btnStreamPlay.isEnabled = true
                        btnStreamPlay.text = "播放 TTS (流式)"
                        Toast.makeText(this@MainActivity, "流式播放完成", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(error: TtsStreamError) {
                    runOnUiThread {
                        btnStreamPlay.isEnabled = true
                        btnStreamPlay.text = "播放 TTS (流式)"
                        Toast.makeText(
                            this@MainActivity,
                            "流式错误: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            playbackCallback = object : AudioPlaybackCallback {
                override fun onPlaybackStarted() {
                    // 更新 UI，如显示播放动画
                    "开始播放".logI(TAG)
                }

                override fun onPlaybackPaused() {
                    "暂停播放".logI(TAG)
                }

                override fun onPlaybackResumed() {
                    "恢复播放".logI(TAG)
                }

                override fun onPlaybackStopped() {
                    "停止播放".logI(TAG)
                }

                override fun onPlaybackCompleted() {
                    // 所有音频数据已播放完毕
                    "播放完成".logI(TAG)
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放资源
        SpeechSdk.release()
    }
}

