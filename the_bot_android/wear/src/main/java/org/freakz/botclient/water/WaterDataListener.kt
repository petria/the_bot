package org.freakz.botclient.water

import android.content.ComponentName
import com.google.android.gms.wearable.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import org.json.JSONObject

class WaterDataListener : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED || event.dataItem.uri.path != "/water/latest") continue
            val payload = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("water") ?: continue
            store(this, payload)
        }
    }
    companion object {
        fun store(context: android.content.Context, payload: String) {
            try {
                val data = JSONObject(payload)
                val prefs = context.getSharedPreferences("live-water", MODE_PRIVATE)
                val previous = JSONObject(prefs.getString("water", "{}")!!)
                if (data.optLong("updatedAt") < previous.optLong("updatedAt")) return
                prefs.edit().putString("water", payload).apply()
                ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, LiveWaterService::class.java)).requestUpdateAll()
            } catch (e: Exception) { android.util.Log.w("BotWater", "Invalid water payload", e) }
        }
    }
}
