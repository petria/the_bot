package org.freakz.botclient

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.HttpException
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class BotClient(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context, "bot-mobile-tokens", MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    private val authInterceptor = Interceptor { chain ->
        val token = prefs.getString("accessToken", null)
        val request = chain.request().newBuilder().apply { if (token != null) header("Authorization", "Bearer $token") }.build()
        chain.proceed(request)
    }
    private val api = Retrofit.Builder()
        .baseUrl(BuildConfig.BOT_WEB_BASE_URL.trimEnd('/') + "/")
        .client(OkHttpClient.Builder().addInterceptor(authInterceptor).build())
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build().create(BotApi::class.java)

    suspend fun login(username: String, password: String) { save(api.login(LoginRequest(username, password, "Android"))) }
    suspend fun logout() { prefs.getString("refreshToken", null)?.let { api.logout(RefreshRequest(it)) }; clear() }
    suspend fun currentUser() = authenticated { api.me() }
    suspend fun registerCurrentDevice() {
        val token = FirebaseMessaging.getInstance().token.await()
        api.registerDevice(DeviceRequest(token, "Android"))
    }
    suspend fun notifications() = authenticated { api.notifications() }
    suspend fun command(command: String) = authenticated { api.command(CommandRequest(command)) }
    suspend fun markRead(id: String) = authenticated { api.markRead(id) }
    suspend fun markAllRead() = authenticated { api.markAllRead() }
    fun isLoggedIn() = prefs.getString("refreshToken", null) != null
    private fun save(tokens: TokenPair) { prefs.edit().putString("accessToken", tokens.accessToken).putString("refreshToken", tokens.refreshToken).apply() }
    private fun clear() { prefs.edit().clear().apply() }

    private suspend fun refreshAccessToken() {
        val refreshToken = prefs.getString("refreshToken", null)
            ?: throw IllegalStateException("Mobile session has expired; please log in again")
        save(api.refresh(RefreshRequest(refreshToken)))
    }

    private suspend fun <T> authenticated(request: suspend () -> T): T {
        return try {
            request()
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
            refreshAccessToken()
            request()
        }
    }
}
