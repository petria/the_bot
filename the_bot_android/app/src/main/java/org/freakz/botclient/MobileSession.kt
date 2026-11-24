package org.freakz.botclient

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

/** Serializes rotation of the refresh token shared by the inbox and phone workers. */
internal class MobileSession(
    private val accessToken: () -> String?,
    private val refreshToken: () -> String?,
    private val save: (TokenPair) -> Unit,
    private val clear: () -> Unit,
    private val refresh: suspend (String) -> TokenPair
) {
    enum class State { SignedOut, SignedIn, Expired }
    class Expired : IllegalStateException("Session expired. Please sign in again.")

    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(if (refreshToken() == null) State.SignedOut else State.SignedIn)
    val state = mutableState.asStateFlow()

    @Volatile
    private var generation = 0L

    suspend fun signIn(login: suspend () -> TokenPair) = mutex.withLock {
        save(login())
        generation++
        mutableState.value = State.SignedIn
    }

    suspend fun signOut(logout: suspend (String) -> Unit) = mutex.withLock {
        try {
            refreshToken()?.let { logout(it) }
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
        }
        clear()
        generation++
        mutableState.value = State.SignedOut
    }

    suspend fun <T> authenticated(request: suspend () -> T): T {
        val sessionGeneration = generation
        val attemptedToken = accessToken()
        try {
            return request()
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
        }

        mutex.withLock {
            if (sessionGeneration != generation) throw Expired()
            val token = refreshToken() ?: throw Expired()
            if (attemptedToken == accessToken()) {
                try {
                    save(refresh(token))
                } catch (error: HttpException) {
                    if (error.code() != 401) throw error
                    clear()
                    generation++
                    mutableState.value = State.Expired
                    throw Expired()
                }
            }
        }
        return request()
    }
}
