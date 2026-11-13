package org.freakz.botclient

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchLinkStatusTest {
    private val t = 1_700_000_000_000L  // fixed base, avoids locale/timezone dependence
    @Test fun neverCommunicated() {
        assertEquals("No communication with the watch yet", WatchLinkStatus.render(0L, t))
        assertEquals("No communication with the watch yet", WatchLinkStatus.render(-1L, t))
    }
    @Test fun recencyBucketsAreDeterministic() {
        assertEquals("just now", WatchLinkStatus.render(t, t))
        assertEquals("just now", WatchLinkStatus.render(t - 59_999, t))
        assertEquals("1 min ago", WatchLinkStatus.render(t - 60_000, t))
        assertEquals("59 min ago", WatchLinkStatus.render(t - 59 * 60_000, t))
        assertEquals("1 h ago", WatchLinkStatus.render(t - 3_600_000, t))
        assertEquals("23 h ago", WatchLinkStatus.render(t - 23 * 3_600_000, t))
        assertEquals("1 d ago", WatchLinkStatus.render(t - 86_400_000, t))
        assertEquals("3 d ago", WatchLinkStatus.render(t - 3 * 86_400_000, t))
    }
    @Test fun futureTimestampIsJustNow() {
        assertEquals("just now", WatchLinkStatus.render(t + 120_000, t))  // clock skew guard
    }
}
