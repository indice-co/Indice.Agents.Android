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
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

internal interface SseApi {
    @Streaming
    @POST("api/my/chats/stream")
    suspend fun newStream(
        @Body body: ChatRequest
    ): Response<ResponseBody>

    @Streaming
    @POST("api/my/chats/{chatId}/messages/stream")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body body: ChatRequest
    ): Response<ResponseBody>

    @GET("api/my/chats")
    suspend fun getMyChats(
        @Query("Page") page: Int? = null,
        @Query("Size") size: Int? = null,
        @Query("Sort") sort: Int? = null,
        @Query("Search") search: String? = null
    ): Response<ConversationListItemResult>

    @GET("api/my/chats/{chatId}")
    suspend fun getChatById(
        @Path("chatId") chatId: String
    ): Response<DexConversation>

    @DELETE("api/my/chats/{chatId}")
    suspend fun deleteChat(
        @Path("chatId") chatId: String
    ): Response<Unit>

    @PUT("api/my/chats/{chatId}/messages/{messageId}/like")
    suspend fun likeMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String,
        @Body request: LikeRequest
    ): Response<Unit>

    companion object {
        fun create(retrofit: Retrofit = ApiClient.retrofit): SseApi = retrofit.create()
    }
}
