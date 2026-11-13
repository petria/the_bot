package org.freakz.botclient.water

import android.content.Context
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class LiveWaterTest {
    @Test fun receivesZeroRejectsOldUpdatesAndExpires() = runBlocking {
        val service = Robolectric.buildService(LiveWaterService::class.java).create().get()
        val now = System.currentTimeMillis()
        val payload = """{"enabled":true,"temperatureC":0.0,"updatedAt":$now,"expiresAt":${now + 60000}}"""
        WaterDataListener.store(service, payload)
        val request = ComplicationRequest(1, ComplicationType.SHORT_TEXT, false)
        val data = service.onComplicationRequest(request) as ShortTextComplicationData
        assertEquals("0.0°C", data.text.getTextAt(service.resources, java.time.Instant.now()).toString())
        WaterDataListener.store(service, """{"enabled":false,"updatedAt":${now - 1}}""")
        assertTrue(service.onComplicationRequest(request) is ShortTextComplicationData)
        WaterDataListener.store(service, """{"enabled":true,"temperatureC":4.2,"updatedAt":${now + 1},"expiresAt":1}""")
        assertTrue(service.onComplicationRequest(request) is NoDataComplicationData)
        WaterDataListener.store(service, """{"enabled":false,"updatedAt":${now + 2}}""")
        assertTrue(service.onComplicationRequest(request) is NoDataComplicationData)
        assertNull(service.createData(ComplicationType.RANGED_VALUE, 4.2f))
    }

    @Test fun wakeWithoutReadingAsksPhoneOncePerInterval() = runBlocking {
        val service = Robolectric.buildService(LiveWaterService::class.java).create().get()
        val request = ComplicationRequest(1, ComplicationType.SHORT_TEXT, false)
        assertTrue(service.onComplicationRequest(request) is NoDataComplicationData)
        val prefs = service.getSharedPreferences("live-water", Context.MODE_PRIVATE)
        val stamped = prefs.getLong("lastAutoRefreshAt", 0L)
        assertTrue("empty complication must stamp an auto refresh request", stamped > 0)
        service.onComplicationRequest(request)
        assertEquals("refreshes within the interval must be debounced",
            stamped, prefs.getLong("lastAutoRefreshAt", 0L))
        assertTrue(LiveWaterService.shouldAutoRefresh(stamped, stamped + LiveWaterService.AUTO_REFRESH_INTERVAL_MS))
        assertFalse(LiveWaterService.shouldAutoRefresh(stamped, stamped + 1))
    }
}
