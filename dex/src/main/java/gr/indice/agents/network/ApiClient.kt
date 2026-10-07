package gr.indice.agents.network

import com.squareup.moshi.Moshi
import gr.indice.agents.dex.BuildConfig
import okhttp3.Call
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