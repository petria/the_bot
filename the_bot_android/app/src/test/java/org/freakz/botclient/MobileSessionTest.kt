package org.freakz.botclient

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class MobileSessionTest {
    @Test
    fun rejectedRefreshReturnsToSignIn() = runBlocking {
        val fixture = Fixture { throw httpError(401) }

        val failure = runCatching { fixture.session.authenticated { throw httpError(401) } }.exceptionOrNull()

        assertTrue(failure is MobileSession.Expired)
        assertEquals(MobileSession.State.Expired, fixture.session.state.value)
        assertNull(fixture.tokens)
    }

    @Test
    fun throttledRefreshKeepsCredentialsForRetry() = runBlocking {
        val fixture = Fixture { throw httpError(429) }

        val failure = runCatching { fixture.session.authenticated { throw httpError(401) } }.exceptionOrNull()

        assertEquals(429, (failure as HttpException).code())
        assertEquals(MobileSession.State.SignedIn, fixture.session.state.value)
        assertEquals("old-refresh", fixture.tokens?.refreshToken)
    }

    @Test
    fun concurrentRequestsShareOneRefresh() = runBlocking {
        var refreshes = 0
        val fixture = Fixture { refreshes++; delay(20); pair("new") }
        suspend fun call() = fixture.session.authenticated {
            if (fixture.tokens?.accessToken != "new-access") throw httpError(401)
            "loaded"
        }

        val inbox = async { call() }
        val watch = async { call() }

        assertEquals("loaded", inbox.await())
        assertEquals("loaded", watch.await())
        assertEquals(1, refreshes)
    }

    @Test
    fun logoutWithRejectedTokenStillSignsOutLocally() = runBlocking {
        val fixture = Fixture { error("Unexpected refresh") }

        fixture.session.signOut { throw httpError(401) }

        assertEquals(MobileSession.State.SignedOut, fixture.session.state.value)
        assertNull(fixture.tokens)
    }

    private class Fixture(refresh: suspend (String) -> TokenPair) {
        var tokens: TokenPair? = pair("old")
        val session = MobileSession(
            accessToken = { tokens?.accessToken },
            refreshToken = { tokens?.refreshToken },
            save = { tokens = it },
            clear = { tokens = null },
            refresh = refresh
        )
    }

    private companion object {
        fun httpError(status: Int) = HttpException(Response.error<String>(status, "".toResponseBody()))
        fun pair(prefix: String) = TokenPair("$prefix-access", "$prefix-refresh", 900, "fixture", "device")
    }
}
