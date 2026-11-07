package org.freakz.botclient.water

import android.content.Intent
import org.json.JSONObject
import androidx.wear.watchface.complications.data.*
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
