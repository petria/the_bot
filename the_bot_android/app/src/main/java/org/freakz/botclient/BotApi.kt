package org.freakz.botclient

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable data class LoginRequest(val username: String, val password: String, val deviceName: String? = null)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class TokenPair(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long, val username: String, val deviceId: String)
@Serializable data class MobileMe(val username: String, val name: String?, val permissions: List<String> = emptyList())
@Serializable data class NotificationItem(val eventId: String, val username: String, val type: String, val title: String, val body: String, val connectionType: String? = null, val channelAlias: String? = null, val command: String? = null, val occurredAt: String, val read: Boolean)
@Serializable data class DeviceRequest(val fcmToken: String, val deviceName: String?, val platform: String = "android")
@Serializable data class CommandRequest(val command: String)
@Serializable data class CommandResponse(val reply: String? = null)

interface BotApi {
    @POST("/api/mobile/auth/login") suspend fun login(@Body request: LoginRequest): TokenPair
    @POST("/api/mobile/auth/refresh") suspend fun refresh(@Body request: RefreshRequest): TokenPair
    @POST("/api/mobile/auth/logout") suspend fun logout(@Body request: RefreshRequest)
    @GET("/api/mobile/me") suspend fun me(): MobileMe
    @POST("/api/mobile/devices") suspend fun registerDevice(@Body request: DeviceRequest)
    @GET("/api/mobile/notifications") suspend fun notifications(): List<NotificationItem>
    @POST("/api/mobile/notifications/{id}/read") suspend fun markRead(@Path("id") id: String)
    @POST("/api/mobile/notifications/read-all") suspend fun markAllRead()
    @POST("/api/mobile/command") suspend fun command(@Body request: CommandRequest): CommandResponse
}
