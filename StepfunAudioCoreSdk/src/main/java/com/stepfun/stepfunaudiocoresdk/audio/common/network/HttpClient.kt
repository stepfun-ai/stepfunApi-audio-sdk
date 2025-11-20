package com.stepfun.stepfunaudiocoresdk.audio.common.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import com.stepfun.stepfunaudiocoresdk.audio.core.SpeechConfig
import java.util.concurrent.TimeUnit

internal object HttpClient {

    private const val DEFAULT_TIMEOUT = 30L
    private const val UPLOAD_TIMEOUT = 60L

    /**
     * 初始化HTTP客户端
     */
    fun init(
        context: Context, config: SpeechConfig
    ) {
        if (NetworkManager.isInitialized().not()) {
            NetworkManager.init(
                context,
                config.baseUrl,
                getSpeechSdkOkhttpClient(context, config.apiKey)
            )
        }
    }

    private fun getSpeechSdkOkhttpClient(context: Context, apiKey: String): OkHttpClient {
        val client = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(UPLOAD_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
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
            }.addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }.addInterceptor(getHttpLoggingInterceptor())
            .build()
        return client
    }

    private fun getHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        return logging
    }

}