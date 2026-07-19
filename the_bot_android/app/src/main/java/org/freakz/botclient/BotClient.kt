package org.freakz.botclient

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
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
    suspend fun currentUser() = api.me()
    suspend fun registerCurrentDevice() {
        val token = FirebaseMessaging.getInstance().token.await()
        api.registerDevice(DeviceRequest(token, "Android"))
    }
    suspend fun notifications() = api.notifications()
    suspend fun command(command: String) = api.command(CommandRequest(command))
    suspend fun markRead(id: String) = api.markRead(id)
    suspend fun markAllRead() = api.markAllRead()
    fun isLoggedIn() = prefs.getString("refreshToken", null) != null
    private fun save(tokens: TokenPair) { prefs.edit().putString("accessToken", tokens.accessToken).putString("refreshToken", tokens.refreshToken).apply() }
    private fun clear() { prefs.edit().clear().apply() }
}
