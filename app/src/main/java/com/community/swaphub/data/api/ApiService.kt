package com.community.swaphub.data.api

import com.community.swaphub.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ------------------- USER -------------------
    @POST("api/users/register")
    suspend fun register(@Body user: User): Response<User>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: String): Response<User>

    @PUT("api/users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body user: User): Response<User>

    @GET("api/users/me")
    suspend fun getCurrentUser(): Response<User>

    // ------------------- ITEMS -------------------
    @GET("api/items")
    suspend fun getItems(
        @Query("type") type: String? = null,
        @Query("location") location: String? = null
    ): Response<List<Item>>

    @GET("api/items/{id}")
    suspend fun getItem(@Path("id") id: String): Response<Item>

    @Multipart
    @POST("api/items")
    suspend fun postItem(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("category") category: RequestBody?,
        @Part("type") type: RequestBody,
        @Part("location") location: RequestBody?,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part imageFile: MultipartBody.Part? = null
    ): Response<Item>

    @Multipart
    @PUT("api/items/{id}")
    suspend fun updateItem(
        @Path("id") id: String,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("category") category: RequestBody?,
        @Part("type") type: RequestBody,
        @Part("location") location: RequestBody?,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part imageFile: MultipartBody.Part? = null
    ): Response<Item>

    @DELETE("api/items/{id}")
    suspend fun deleteItem(@Path("id") id: String): Response<Unit>

    @GET("api/items/my-items")
    suspend fun getMyItems(): Response<List<Item>>

    @GET("api/items/others-items")
    suspend fun getOthersItems(): Response<List<Item>>

    // ✅ New endpoint for changing item status (frontend-only change)
    @Multipart
    @PUT("api/items/{id}")
    suspend fun updateItemStatus(
        @Path("id") itemId: String,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("category") category: RequestBody?,
        @Part("type") type: RequestBody,
        @Part("location") location: RequestBody?,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part("status") status: RequestBody
    ): Response<Item>


    @GET("api/items/nearby")
    suspend fun getNearbyItems(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double
    ): Response<List<Item>>



    // ------------------- SWAP REQUESTS -------------------
    @POST("api/swap-requests")
    suspend fun createSwapRequest(@Body request: CreateSwapRequest): Response<SwapRequest>

    @GET("api/swap-requests/my-swaps")
    suspend fun getMySwapRequests(): Response<List<SwapRequest>>

    // Add this to your ApiService.kt in the SWAP REQUESTS section

    @GET("api/swap-requests/my-requests")
    suspend fun getMyRequestedItems(): Response<List<SwapRequest>>

    @GET("api/swap-requests/item/{itemId}")
    suspend fun getSwapRequestsByItem(@Path("itemId") itemId: String): Response<List<SwapRequest>>

    @PUT("api/swap-requests/{id}/accept")
    suspend fun acceptSwapRequest(@Path("id") id: String): Response<SwapRequest>

    @PUT("api/swap-requests/{id}/complete")
    suspend fun completeSwapRequest(@Path("id") id: String): Response<SwapRequest>

    // ------------------- CHAT -------------------
    @GET("api/chat/my-conversations/{userId}")
    suspend fun getConversations(@Path("userId") userId: String): Response<List<ConversationResponse>>

    @POST("api/chat/message")
    suspend fun sendMessage(@Body request: ChatMessageDTO): Response<ChatBackendResponse>

    @POST("api/chat/conversation")
    suspend fun createConversation(@Body request: ConversationRequestDTO): Response<ConversationResponse>

    @GET("api/chat/conversation/{conversationId}")
    suspend fun getMessagesByConversation(@Path("conversationId") conversationId: String): Response<List<ChatBackendResponse>>

    // ------------------- POINTS -------------------
    @GET("api/points/user/{userId}")
    suspend fun getUserPoints(@Path("userId") userId: String): Response<Int>

    @GET("api/points/history/{userId}")
    suspend fun getPointsHistory(@Path("userId") userId: String): Response<List<PointsHistory>>

    // ------------------- ADMIN -------------------
    @GET("api/admin/users")
    suspend fun getAllUsers(): Response<List<User>>

    @GET("api/admin/items")
    suspend fun getAllItems(): Response<List<Item>>

    @DELETE("api/admin/users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<Unit>

    @DELETE("api/admin/items/{id}")
    suspend fun deleteItemAdmin(@Path("id") id: String): Response<Unit>

    // ===================== NOTIFICATIONS =====================

    @GET("api/notifications")
    suspend fun getNotifications(): Response<List<Notification>>

    @GET("api/notifications/unread/count")
    suspend fun getUnreadNotificationCount(): Response<Long>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: String): Response<Notification>

}