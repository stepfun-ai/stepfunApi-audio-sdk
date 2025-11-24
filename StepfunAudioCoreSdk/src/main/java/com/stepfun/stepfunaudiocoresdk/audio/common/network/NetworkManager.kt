package com.stepfun.stepfunaudiocoresdk.audio.common.network

import android.accounts.NetworkErrorException
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logD
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logE
import com.stepfun.stepfunaudiocoresdk.audio.common.logger.logI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

const val TAG = "NetworkManager"

object NetworkManager {

    internal lateinit var applicationContext: Context

    private lateinit var baseUrl: String //自定义的baseurl


    private const val DELAY_TIME = 1000L

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient) // 添加 OkHttp 客户端
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val _defaultOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)    // 设置连接超时
            .readTimeout(15, TimeUnit.SECONDS)       // 设置读取超时
            .writeTimeout(15, TimeUnit.SECONDS)      // 设置写入超时
            .retryOnConnectionFailure(true)         // 允许失败重试
            .addInterceptor { chain ->
                if (!isNetworkAvailable()) {
                    return@addInterceptor Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(599)
                        .message("No network available")
                        .body(ByteArray(0).toResponseBody(null))
                        .build()
                }
                chain.proceed(chain.request())
            }.build()
    }

    lateinit var httpClient: OkHttpClient
    private lateinit var eventFactory: EventSource.Factory
    val apiService by lazy {
        createApiService(BaseApiService::class.java)
    }

    private fun <T> createApiService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

    /**
     * 初始化方法
     *  @param context : Context application Context，最好在Application.onAttachBaseContext中初始化
     *  @param baseUrl: String 域名
     */
    @JvmStatic
    fun init(
        context: Context,
        baseUrl: String,
        okHttpClient: OkHttpClient? = null,
    ) {
        applicationContext = context
        NetworkManager.baseUrl = baseUrl
        httpClient = okHttpClient ?: _defaultOkHttpClient
        eventFactory = EventSources.createFactory(httpClient)
    }

    fun isInitialized() = NetworkManager::applicationContext.isInitialized

    suspend inline fun <reified T : BaseResponse> post(
        url: String,
        request: Any,
        retryTimes: Int? = 1 //默认只执行一次
    ): State<T> {
        return safeApiCall(retryTimes ?: 1) {
            "发送请求".logD(TAG)
            val response = apiService.post(url, request)
            if (response.isSuccessful) {
                "请求成功,${response.body()}".logD(TAG)
                val responseBody =
                    response.body() ?: throw IllegalArgumentException("response body is null")
                val body = parseResponseBody<T>(responseBody)
                "请求解码成功,${body}".logD(TAG)
                if (body.isSuccess()) {
                    body
                } else {
                    throw IOException(body.getFailedReason())
                }
            } else {
                val errorInfo = response.errorBody()?.string()
                throw IOException(errorInfo)
            }
        }
    }

    inline fun <reified T> post(
        url: String,
        request: RequestBody,
    ): State<T> = runCatching {
        "发送请求".logD(TAG)
        httpClient.newCall(
            Request.Builder()
                .url(url)
                .post(request)
                .build()
        ).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw NetworkErrorException("http=${resp.code}")
            }
            resp.body?.use { body ->
                parseResponseBody2(body, T::class.java)
            } ?: throw IllegalArgumentException("empty body")
        }
    }.fold(
        onSuccess = {
            "请求成功,$it".logD(TAG)
            State.success(it)
        },
        onFailure = {
            "请求失败,$it".logD(TAG)
            State.error(it)
        }
    )


    inline fun <reified T> postFlow(
        url: String,
        request: RequestBody,
    ): Flow<State<T>> = callbackFlow {
        "发送请求".logD(TAG)
        val call = httpClient.newCall(
            Request.Builder()
                .url(url)
                .post(request)
                .build()
        )
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                trySend(State.error(e))
                close(e)          // 结束 Flow
            }

            override fun onResponse(call: Call, response: Response) {
                val state = runCatching {
                    if (!response.isSuccessful) {
                        throw NetworkErrorException("http=${response.code}")
                    }
                    response.body?.use { body ->
                        parseResponseBody2(body, T::class.java)
                    } ?: throw IllegalArgumentException("empty body")
                }.fold(
                    onSuccess = {
                        "请求成功,$it".logD(TAG)
                        State.success(it)
                    },
                    onFailure = {
                        "请求失败,$it".logD(TAG)
                        State.error(it)
                    }
                )
                trySend(state)
                close()
            }
        })
        awaitClose { call.cancel() }
    }

    /**
     * 发送post请求并返回二进制数据（用于TTS 等返回音频文件的接口）
     */
    suspend fun postForBinary(
        url: String,
        request: Any,
        retryTimes: Int = 1 // 默认只执行一次
    ): State<ByteArray> {
        return safeApiCall(retryTimes) {
            "发送二进制请求".logD(TAG)
            val response = apiService.post(url, request)
            if (response.isSuccessful) {
                "二进制请求成功,${response.body()}".logD(TAG)
                val responseBody =
                    response.body() ?: throw IllegalArgumentException("response body is null")
                val bytes = responseBody.bytes()
                "二进制请求解码成功, size=${bytes.size}".logD(TAG)
                bytes
            } else {
                val errorInfo = response.errorBody()?.string()
                throw IOException(errorInfo)
            }
        }
    }


    fun sseFlow(url: String, requestBody: RequestBody): Flow<String> = callbackFlow {
        val request = Request.Builder().url(url).post(requestBody).build()
        val eventSource = eventFactory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                "stream 请求开始".logD(TAG)
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                "stream 请求中".logD(TAG)
                trySend(data)
            }

            override fun onClosed(eventSource: EventSource) {
                "stream 请求结束".logD(TAG)
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                "stream 请求错误，error:$t".logE(TAG)
                close(t ?: Exception("SSE failed")) // 发生错误时关闭 Flow
            }
        })
        awaitClose { eventSource.cancel() }
    }

    inline fun <reified T : BaseResponse> parseResponseBody(responseBody: ResponseBody): T {
        val gson = Gson()
        val type = object : TypeToken<T>() {}.type
        return gson.fromJson(responseBody.charStream(), type)
    }

    fun <T> parseResponseBody2(responseBody: ResponseBody, clazz: Class<T>): T {
        val gson = Gson()
        val string = responseBody.string()
        "请求成功,${string}".logD(TAG)
        return gson.fromJson(string, clazz)
    }

    @PublishedApi
    internal suspend fun <T> safeApiCall(retryTimes: Int, apiCall: suspend () -> T): State<T> {
        return withContext(Dispatchers.IO) {
            var lastError: Exception? = null
            repeat(retryTimes) { retryIndex ->
                try {
                    val result = apiCall()
                    return@withContext State.success(result)
                } catch (e: Exception) {
                    if (e is IllegalArgumentException) {
                        "参数异常，异常详细信息为:  $e ".logE(TAG)
                        throw e
                    }
                    lastError = e
                    "请求异常: ${DELAY_TIME / 1000} s 后，重试请求 $retryIndex ,$e".logI(TAG)
                    delay(DELAY_TIME)
                }
            }
            State.error(Exception("something wrong,err:$lastError"))
        }
    }

}

//网络检查工具
@SuppressLint("MissingPermission")
fun NetworkManager.isNetworkAvailable(): Boolean {
    if (isInitialized().not()) {
        "网络不可用".logI(TAG)
        return false
    }
    val connectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager
        .getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

