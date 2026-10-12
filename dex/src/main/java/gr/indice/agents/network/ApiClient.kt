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
                                addHeader("Authorization","Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IjM4NDNGODY4OEZGNDM1REMxOUQ2MkU3QzQxQjRFMjQ3QURGRjg5QkVSUzI1NiIsIng1dCI6Ik9FUDRhSV8wTmR3WjFpNThRYlRpUjYzX2liNCIsInR5cCI6ImF0K2p3dCJ9.eyJpc3MiOiJodHRwczovL215LmluZGljZS5nciIsIm5iZiI6MTc5MTgxMTA5MSwiaWF0IjoxNzkxODExMDkxLCJleHAiOjE3OTE4MTQ2OTEsImF1ZCI6ImFnZW50cyIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgcm9sZSBlbWFpbCBhZ2VudHMgY2hhdCBpbmdlc3QiLCJhbXIiOlsiTWljcm9zb2Z0IiwibWZhIl0sImNsaWVudF9pZCI6ImRleC11aSIsInN1YiI6ImZmZmQyN2QyLTgzMTYtNGUyNi1iODEyLWY5MWIxZjNmMDFkOCIsImF1dGhfdGltZSI6MTc5MTU1MTY1MCwiaWRwIjoiTWljcm9zb2Z0IiwiZW1haWwiOiJlLmtvdXRyYWtpc0BpbmRpY2UuZ3IiLCJnaXZlbl9uYW1lIjoiXHUwMzk1XHUwM0JCXHUwM0I1XHUwM0M1XHUwM0I4XHUwM0FEXHUwM0MxXHUwM0I5XHUwM0JGXHUwM0MyIiwiZmFtaWx5X25hbWUiOiJcdTAzOUFcdTAzQkZcdTAzQzVcdTAzQzRcdTAzQzFcdTAzQUNcdTAzQkFcdTAzQjdcdTAzQzIiLCJsb2NhbGUiOiJlbiIsImFkbWluIjpmYWxzZSwibmFtZSI6ImUua291dHJha2lzQGluZGljZS5nciIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJwaG9uZV9udW1iZXIiOiI2OTczODM2OTE4IiwicGhvbmVfbnVtYmVyX3ZlcmlmaWVkIjp0cnVlLCJpcGFkZHIiOiI5NC43MS4xNDIuNDAiLCJkZXZpY2VfaWQiOiI5ZTY5YzkxOTlkY2ViNjVlODY0MWVmMGYxZjJmMjc3YS5TYWZhcmkiLCJzaWQiOiIxN0ZEM0E2RDc2NTZEMTA0NTMxN0QwMEE0Q0M0RDkyQiJ9.C-ExRYdeNPMG-5gs4IFR3NoR1fS8c80-GiKhtif5hxkmRd7RMyc8MPZNssnEl0OdQQ0kxS1TmjEymcNSrv8tzueU0XcJmOCWhErQZC3mPhtGRYbhxjubCglaKDmyFXjYpJqSfWizGN2RiH5KlOubDbGSLRb2o8U3l3VcdegiilhvFJYHrBbgJ0ansilKgyn8Na-0_jwK-P6QFVgyARCododFF_fOEWxOURBqM0t4-gxp8is2HNaBfw00M041c8gsaWTat5h6wmoVIQlA32nhPjvWWsUZblmFhXfcUjaS10duYcvc8DrJmjT_cPYVcK-4xnMKzP2hsP9MAFtUKes69A")
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