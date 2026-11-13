package org.freakz.botclient

import android.content.Context

/** Plain (unencrypted) prefs for non-secret watch-link timestamps, like the watch's live-water prefs. */
class WatchLinkStore(context: Context) {
    private val prefs = context.getSharedPreferences("bot-watch-link", Context.MODE_PRIVATE)
    fun recordSync(nowMillis: Long = System.currentTimeMillis()) =
        prefs.edit().putLong(KEY_SYNC, nowMillis).apply()
    fun recordRequest(nowMillis: Long = System.currentTimeMillis()) =
        prefs.edit().putLong(KEY_REQUEST, nowMillis).apply()
    fun lastSyncAt(): Long = prefs.getLong(KEY_SYNC, 0L)
    fun lastRequestAt(): Long = prefs.getLong(KEY_REQUEST, 0L)
    companion object { private const val KEY_SYNC = "lastSyncAt"; private const val KEY_REQUEST = "lastRequestAt" }
}
