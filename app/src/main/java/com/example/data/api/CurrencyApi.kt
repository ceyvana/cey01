package com.example.data.api

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CurrencyResponse(
    val result: String,
    val base_code: String,
    val rates: Map<String, Double>
)

interface CurrencyApiService {
    @GET("v6/latest/USD")
    suspend fun getLatestRates(): CurrencyResponse
}

object CurrencyRetrofitClient {
    private const val BASE_URL = "https://open.er-api.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: CurrencyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CurrencyApiService::class.java)
    }
}
