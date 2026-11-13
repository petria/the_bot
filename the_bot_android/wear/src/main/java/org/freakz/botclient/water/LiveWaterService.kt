package org.freakz.botclient.water

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import java.time.Instant

class LiveWaterService : WaterComplicationService() {
    override fun onCreate() {
        super.onCreate()
        com.google.android.gms.wearable.Wearable.getDataClient(this).dataItems.addOnSuccessListener { items ->
            try {
                for (item in items) if (item.uri.path == "/water/latest") {
                    com.google.android.gms.wearable.DataMapItem.fromDataItem(item).dataMap.getString("water")?.let {
                        WaterDataListener.store(this, it)
                    }
                }
            } finally { items.release() }
        }
    }
    override fun tapIntent() = Intent(this, LiveWaterActivity::class.java)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val data = super.onComplicationRequest(request)
        if (data is NoDataComplicationData) autoRefreshWhenEmpty()
        return data
    }

    /** The watch face re-requests this provider whenever the screen wakes, the same refresh point the
     * air weather widget uses. When there is no valid reading on screen, ask the phone once per
     * interval for a fresh reading; the urgent /water/latest push then re-renders the complication
     * through WaterDataListener while the screen is still on. */
    private fun autoRefreshWhenEmpty() {
        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences("live-water", MODE_PRIVATE)
        if (!shouldAutoRefresh(prefs.getLong("lastAutoRefreshAt", 0L), now)) return
        prefs.edit().putLong("lastAutoRefreshAt", now).apply()
        runCatching {
            Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
                nodes.firstOrNull()?.let { phone -> runCatching {
                    Wearable.getMessageClient(this).sendMessage(phone.id, "/water/refresh", byteArrayOf())
                }.onFailure { Log.w("BotWater", "Auto refresh send failed", it) } }
            }
        }.onFailure { Log.w("BotWater", "Auto refresh lookup failed", it) }
    }

    companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 5 * 60_000L
        fun shouldAutoRefresh(lastAutoRefreshAt: Long, now: Long): Boolean =
            now - lastAutoRefreshAt >= AUTO_REFRESH_INTERVAL_MS
    }

    override fun reading(): Float? = try {
        val data = JSONObject(getSharedPreferences("live-water", MODE_PRIVATE).getString("water", "{}")!!)
        if (!data.optBoolean("enabled") || data.optLong("expiresAt") <= System.currentTimeMillis()) null
        else WaterReading.valid(data.optDouble("temperatureC", Double.NaN).toFloat())
    } catch (e: Exception) { null }
    override fun createData(type: ComplicationType, reading: Float?): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        val data = super.createData(type, reading)
        val expiry = try { JSONObject(getSharedPreferences("live-water", MODE_PRIVATE).getString("water", "{}")!!).optLong("expiresAt") } catch (e: Exception) { 0L }
        return if (data is ShortTextComplicationData && expiry > System.currentTimeMillis())
            ShortTextComplicationData.Builder(data.text, data.contentDescription!!)
                .setMonochromaticImage(data.monochromaticImage!!).setTapAction(data.tapAction!!)
                .setValidTimeRange(TimeRange.between(Instant.EPOCH, Instant.ofEpochMilli(expiry))).build()
            else data
    }
}
