package org.freakz.botclient

/** Pure, locale-free relative label for "last communicated with the watch".
 *  storedMillis is 0 when never communicated; nowMillis is injected so it is unit-testable. */
object WatchLinkStatus {
    private const val MIN = 60_000L
    private const val HOUR = 3_600_000L
    private const val DAY = 86_400_000L
    fun render(storedMillis: Long, nowMillis: Long): String {
        if (storedMillis <= 0L) return "No communication with the watch yet"
        val delta = nowMillis - storedMillis
        if (delta < MIN) return "just now"
        if (delta < HOUR) return "${delta / MIN} min ago"
        if (delta < DAY) return "${delta / HOUR} h ago"
        return "${delta / DAY} d ago"
    }
}
