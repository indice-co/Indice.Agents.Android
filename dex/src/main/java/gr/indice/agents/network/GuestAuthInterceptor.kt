package gr.indice.agents.network

import gr.indice.agents.network.models.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

internal class GuestAuthInterceptor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val guestAuthorization = tokenStorage.authorization
            ?.takeIf { original.header(AUTHORIZATION) == null }

        val request = guestAuthorization
            ?.let { original.newBuilder().header(AUTHORIZATION, it).build() }
            ?: original

        val response = chain.proceed(request)

        if (response.code == 401 && guestAuthorization != null) {
            tokenStorage.clear(guestAuthorization)
        }

        return response
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
