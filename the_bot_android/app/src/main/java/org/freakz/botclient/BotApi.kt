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
@Serializable data class NotificationItem(val eventId: String, val username: String, val type: String, val title: String, val body: String, val connectionType: String? = null, val channelAlias: String? = null, val command: String? = null, val occurredAt: String, val read: Boolean, val replyDraft: ReplyDraft? = null)
@Serializable data class DeviceRequest(val fcmToken: String, val deviceName: String?, val platform: String = "android")
@Serializable data class CommandRequest(val command: String)
@Serializable data class CommandResponse(val reply: String? = null)

@Serializable data class ReplyDraft(val id: String, val network: String, val channel: String, val sender: String, val sourceMessage: String, val text: String, val expectedNick: String? = null)
@Serializable data class IrssiAvailability(val available: Boolean)
@Serializable data class IrssiSend(val text: String, val retry: Boolean = false)
@Serializable data class IrssiJob(val id: String, val status: String, val detail: String? = null)
@Serializable data class IrssiAgent(val id: String, val name: String, val expectedNick: String, val online: Boolean)
@Serializable data class IrssiPairing(val code: String, val expiresAt: String)

interface BotApi {
    @POST("/api/mobile/irssi/pairing") suspend fun irssiPairing(): IrssiPairing
    @GET("/api/mobile/irssi/agents") suspend fun irssiAgents(): List<IrssiAgent>
    @DELETE("/api/mobile/irssi/agents/{id}") suspend fun revokeIrssi(@Path("id") id: String)
    @GET("/api/mobile/irssi/drafts/{id}/availability") suspend fun irssiAvailability(@Path("id") id: String): IrssiAvailability
    @GET("/api/mobile/irssi/drafts/{id}/jobs") suspend fun irssiJobs(@Path("id") id: String): List<IrssiJob>
    @POST("/api/mobile/irssi/drafts/{id}/send") suspend fun sendIrssi(@Path("id") id: String, @Body request: IrssiSend): IrssiJob
    @GET("/api/mobile/irssi/jobs/{id}") suspend fun irssiJob(@Path("id") id: String): IrssiJob
    @POST("/api/mobile/auth/login") suspend fun login(@Body request: LoginRequest): TokenPair
    @POST("/api/mobile/auth/refresh") suspend fun refresh(@Body request: RefreshRequest): TokenPair
    @POST("/api/mobile/auth/logout") suspend fun logout(@Body request: RefreshRequest)
    @POST("/api/mobile/water/refresh") suspend fun refreshWater(): kotlinx.serialization.json.JsonObject
    @GET("/api/mobile/me") suspend fun me(): MobileMe
    @POST("/api/mobile/devices") suspend fun registerDevice(@Body request: DeviceRequest)
    @GET("/api/mobile/notifications") suspend fun notifications(): List<NotificationItem>
    @POST("/api/mobile/notifications/{id}/read") suspend fun markRead(@Path("id") id: String)
    @POST("/api/mobile/notifications/read-all") suspend fun markAllRead()
    @POST("/api/mobile/command") suspend fun command(@Body request: CommandRequest): CommandResponse
}
