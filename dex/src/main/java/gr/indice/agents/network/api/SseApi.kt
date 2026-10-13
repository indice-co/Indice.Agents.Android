package gr.indice.agents.network.api


import gr.indice.agents.network.ApiClient
import gr.indice.agents.network.models.ChatRequest
import gr.indice.agents.network.models.ConversationListItemResult
import gr.indice.agents.network.models.DexConversation
import gr.indice.agents.network.models.LikeRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface SseApi {
    @Streaming
    @POST("api/my/chats/stream")
    suspend fun newStream(
        @Header("Authorization") authorization: String? = null,
        @Body body: ChatRequest
    ): Response<ResponseBody>

    @Streaming
    @POST("api/my/chats/{chatId}/messages/stream")
    suspend fun sendMessage(
        @Header("Authorization") authorization: String? = null,
        @Path("chatId") chatId: String,
        @Body body: ChatRequest
    ): Response<ResponseBody>

    @GET("api/my/chats")
    suspend fun getMyChats(
        @Header("Authorization") authorization: String? = null,
        @Query("Page") page: Int? = null,
        @Query("Size") size: Int? = null,
        @Query("Sort") sort: Int? = null,
        @Query("Search") search: String? = null
    ): Response<ConversationListItemResult>

    @GET("api/my/chats/{chatId}")
    suspend fun getChatById(
        @Header("Authorization") authorization: String? = null,
        @Path("chatId") chatId: String
    ): Response<DexConversation>

    @DELETE("api/my/chats/{chatId}")
    suspend fun deleteChat(
        @Header("Authorization") authorization: String? = null,
        @Path("chatId") chatId: String
    ): Response<Unit>

    @PUT("api/my/chats/{chatId}/messages/{messageId}/like")
    suspend fun likeMessage(
        @Header("Authorization") authorization: String? = null,
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String,
        @Body request: LikeRequest
    ): Response<Unit>

    companion object {
        fun create(retrofit: Retrofit = ApiClient.retrofit): SseApi = retrofit.create()
    }
}