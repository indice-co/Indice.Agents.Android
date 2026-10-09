package gr.indice.agents.network

import com.squareup.moshi.Moshi
import gr.indice.agents.dex.BuildConfig
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

internal object ApiClient {

    private lateinit var BASE_URL: String
    private lateinit var RETROFIT_CONVERTERS  : List<() -> MoshiConverterFactory>
    private lateinit var OKHTTP_BUILDER_BLOCK : OkHttpClient.Builder.() -> OkHttpClient.Builder
    private lateinit var MOSHI_BUILDER_BLOCK  : Moshi.Builder.() -> Moshi.Builder

    internal fun initialize(
        baseUrl: String,
        retrofitConverterFactories: List<() -> MoshiConverterFactory> = emptyList(),
        okHttpBuilderBlock: OkHttpClient.Builder.() -> OkHttpClient.Builder = { this },
        moshiBuilderBlock: Moshi.Builder.() -> Moshi.Builder = { this }
    ) {
        BASE_URL = baseUrl
        RETROFIT_CONVERTERS  = retrofitConverterFactories
        OKHTTP_BUILDER_BLOCK = okHttpBuilderBlock
        MOSHI_BUILDER_BLOCK  = moshiBuilderBlock
    }



    private val clientBuilder: OkHttpClient.Builder by lazy {
        when (BuildConfig.DEBUG) {
            true -> OkHttpClientBuilderHelper.getUnsafeOkHttpClient()
            else -> OkHttpClientBuilderHelper.getOkHttpBuilder()
        }.let(OKHTTP_BUILDER_BLOCK)
            .apply { if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                if (BuildConfig.DEBUG) {
                    addInterceptor(Interceptor {
                        it.request()
                            .newBuilder()
                            .apply {
                                addHeader("Authorization","Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IjM4NDNGODY4OEZGNDM1REMxOUQ2MkU3QzQxQjRFMjQ3QURGRjg5QkVSUzI1NiIsIng1dCI6Ik9FUDRhSV8wTmR3WjFpNThRYlRpUjYzX2liNCIsInR5cCI6ImF0K2p3dCJ9.eyJpc3MiOiJodHRwczovL215LmluZGljZS5nciIsIm5iZiI6MTc5MTUzOTQzMSwiaWF0IjoxNzkxNTM5NDMxLCJleHAiOjE3OTE1NDMwMzEsImF1ZCI6ImFnZW50cyIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgcm9sZSBlbWFpbCBhZ2VudHMgY2hhdCBpbmdlc3QiLCJhbXIiOlsiTWljcm9zb2Z0IiwibWZhIl0sImNsaWVudF9pZCI6ImRleC11aSIsInN1YiI6ImZmZmQyN2QyLTgzMTYtNGUyNi1iODEyLWY5MWIxZjNmMDFkOCIsImF1dGhfdGltZSI6MTc5MTUzOTQzMSwiaWRwIjoiTWljcm9zb2Z0IiwiZW1haWwiOiJlLmtvdXRyYWtpc0BpbmRpY2UuZ3IiLCJnaXZlbl9uYW1lIjoiXHUwMzk1XHUwM0JCXHUwM0I1XHUwM0M1XHUwM0I4XHUwM0FEXHUwM0MxXHUwM0I5XHUwM0JGXHUwM0MyIiwiZmFtaWx5X25hbWUiOiJcdTAzOUFcdTAzQkZcdTAzQzVcdTAzQzRcdTAzQzFcdTAzQUNcdTAzQkFcdTAzQjdcdTAzQzIiLCJsb2NhbGUiOiJlbiIsImFkbWluIjpmYWxzZSwibmFtZSI6ImUua291dHJha2lzQGluZGljZS5nciIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJwaG9uZV9udW1iZXIiOiI2OTczODM2OTE4IiwicGhvbmVfbnVtYmVyX3ZlcmlmaWVkIjp0cnVlLCJpcGFkZHIiOiI5NC43MS4xNDIuNDAiLCJzaWQiOiIxMUU2MTA1NEY4RTdDNjUyQ0QzNkNCNjVDNjFFRjg4OCJ9.fSpZMY0APg3ThglgMazButcZhzu74niN0O1JseZs5w8whNDnXKZyZRM2kV6K_rrR8BQFdy46PfgdBAfXek8gzA4I6TwiFB6ruBSLdn9LKKd9LVbo6FawiihxL26r75htunK6b7B6qtQm4ViGRj2bFnr76wVYsAKt8QDJ_AQtV2i7w5QYotSEpPm-OSnlX49VgvCLXBLXksVkhFcxCdo5-ld3KhMFmmJrXfcUtGRUngfG8MQa-aw3lD6yq41117t5wgJnjbNba2pI3vXxDdooYlvX0G_1ROku3ziCw6lgju1HgXiCvKbhTsi0JTpQ8JRefySMI8M2PveW9volpKMJ5w")
                            }.build()
                            .let(it::proceed)
                    })
                }
            } }
    }

    private val retrofitBuilder: Retrofit.Builder by lazy {
        val moshi = Serializer
            .moshiBuilder
            .let(MOSHI_BUILDER_BLOCK)
            .build()

        Retrofit.Builder().callFactory(object : Call.Factory {
            private val client by lazy { clientBuilder.build() }
            override fun newCall(request: Request): Call = client.newCall(request)
        }).baseUrl(BASE_URL)
            .apply {
                RETROFIT_CONVERTERS.forEach {
                    addConverterFactory(it.invoke())
                }
            }
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
    }

    internal val retrofit: Retrofit by lazy { retrofitBuilder.build() }
}