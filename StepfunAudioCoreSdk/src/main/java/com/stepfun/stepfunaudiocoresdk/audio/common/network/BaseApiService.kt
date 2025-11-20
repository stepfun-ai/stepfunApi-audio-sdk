package com.stepfun.stepfunaudiocoresdk.audio.common.network

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface BaseApiService {
    @POST
    suspend fun post(@Url url: String, @Body body: Any): retrofit2.Response<ResponseBody>
}