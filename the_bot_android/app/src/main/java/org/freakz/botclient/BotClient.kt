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

class BotClient private constructor(private val context: Context) {
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
        .client(OkHttpClient.Builder().readTimeout(120, java.util.concurrent.TimeUnit.SECONDS).addInterceptor(authInterceptor).build())
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build().create(BotApi::class.java)

    private val session = MobileSession(
        accessToken = { prefs.getString("accessToken", null) },
        refreshToken = { prefs.getString("refreshToken", null) },
        save = { tokens ->
            prefs.edit().putString("accessToken", tokens.accessToken).putString("refreshToken", tokens.refreshToken).apply()
        },
        clear = { prefs.edit().remove("accessToken").remove("refreshToken").apply() },
        refresh = { api.refresh(RefreshRequest(it)) }
    )
    internal val sessionState = session.state

    suspend fun login(username: String, password: String) {
        session.signIn { api.login(LoginRequest(username, password, "Android")) }
        WaterWatchWorker.start(context)
    }

    suspend fun logout() {
        session.signOut { api.logout(RefreshRequest(it)) }
        runCatching { WaterWatchWorker.clear(context) }
    }

    suspend fun irssiPairing() = authenticated { api.irssiPairing() }
    suspend fun irssiAgents() = authenticated { api.irssiAgents() }
    suspend fun revokeIrssi(id: String) = authenticated { api.revokeIrssi(id) }
    suspend fun irssiAvailability(id: String) = authenticated { api.irssiAvailability(id) }
    suspend fun irssiJobs(id: String) = authenticated { api.irssiJobs(id) }
    suspend fun sendIrssi(id: String, text: String, retry: Boolean) = authenticated { api.sendIrssi(id, IrssiSend(text, retry)) }
    suspend fun irssiJob(id: String) = authenticated { api.irssiJob(id) }
    suspend fun refreshWater() = authenticated { api.refreshWater() }
    suspend fun webUiLinks() = authenticated { api.webUiLinks() }
    suspend fun currentUser() = authenticated { api.me() }
    suspend fun registerCurrentDevice() {
        val token = FirebaseMessaging.getInstance().token.await()
        authenticated { api.registerDevice(DeviceRequest(token, "Android")) }
    }
    suspend fun notifications() = authenticated { api.notifications() }
    suspend fun command(command: String) = authenticated { api.command(CommandRequest(command)) }
    suspend fun markRead(id: String) = authenticated { api.markRead(id) }
    suspend fun markAllRead() = authenticated { api.markAllRead() }
    fun isLoggedIn() = sessionState.value == MobileSession.State.SignedIn

    private suspend fun <T> authenticated(request: suspend () -> T): T = session.authenticated(request)

    companion object {
        @Volatile
        private var instance: BotClient? = null

        fun get(context: Context): BotClient = instance ?: synchronized(this) {
            instance ?: BotClient(context.applicationContext).also { instance = it }
        }
    }
}
