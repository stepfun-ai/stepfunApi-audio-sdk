package com.stepfun.stepfunaudiocoresdk.audio.common.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import com.stepfun.stepfunaudiocoresdk.audio.core.SpeechConfig
import java.util.concurrent.TimeUnit

/**
 * WebSocket 客户端管理器
 * 负责创建和配置 WebSocket 连接
 *
 * 类似 HttpClient，提供统一的 WebSocket 配置和管理
 */
object WebSocketClient {

    private const val DEFAULT_CONNECT_TIMEOUT = 15L
    private const val DEFAULT_READ_TIMEOUT = 70L  // 60秒空闲超时 + 10秒缓冲
    private const val DEFAULT_WRITE_TIMEOUT = 15L
    private const val PING_INTERVAL = 30L

    private var okHttpClient: OkHttpClient? = null
    private lateinit var apiKey: String

    /**
     * 初始化 WebSocket 客户端
     * 通常在 SpeechCoreSdk.init() 中调用
     */
    fun init(context: Context, config: SpeechConfig) {
        apiKey = config.apiKey

        if (okHttpClient == null) {
            okHttpClient = buildWebSocketClient(context, config.apiKey)
        }
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean {
        return okHttpClient != null
    }

    /**
     * 创建 WebSocket 连接
     *
     * @param url WebSocket URL
     * @param listener WebSocket 监听器
     * @return WebSocket 实例
     */
    fun newWebSocket(url: String, listener: WebSocketListener): WebSocket {
        if (!isInitialized()) {
            throw IllegalStateException("WebSocketClient not initialized. Call init() first.")
        }

        val request = Request.Builder()
            .url(url)
            .build()

        return okHttpClient!!.newWebSocket(request, listener)
    }

    /**
     * 创建带自定义请求的 WebSocket 连接
     */
    fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket {
        if (!isInitialized()) {
            throw IllegalStateException("WebSocketClient not initialized. Call init() first.")
        }
        return okHttpClient!!.newWebSocket(request, listener)
    }

    /**
     * 构建 WebSocket OkHttpClient
     * 配置超时、拦截器、心跳等
     */
    private fun buildWebSocketClient(context: Context, apiKey: String): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .pingInterval(PING_INTERVAL, TimeUnit.SECONDS)  // WebSocket 心跳
            .retryOnConnectionFailure(true)
            // 网络检查拦截器
            .addInterceptor { chain ->
                if (!NetworkManager.isNetworkAvailable()) {
                    return@addInterceptor Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(599)
                        .message("No network available")
                        .body(ByteArray(0).toResponseBody(null))
                        .build()
                }
                chain.proceed(chain.request())
            }
            // 认证拦截器 - 添加 Authorization header
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            // 日志拦截器
            .addInterceptor(buildLoggingInterceptor())
            .build()
    }

    /**
     * 构建日志拦截器
     */
    private fun buildLoggingInterceptor(): HttpLoggingInterceptor {
        val logging = HttpLoggingInterceptor()
        HttpLoggingInterceptor.Level.HEADERS  // WebSocket 只打印 headers
        return logging
    }

    /**
     * 释放资源（一般不需要主动调用）
     */
    fun release() {
        okHttpClient?.dispatcher?.executorService?.shutdown()
        okHttpClient?.connectionPool?.evictAll()
        okHttpClient = null
    }
}