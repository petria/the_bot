package org.freakz.botclient.water

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.view.Gravity
import org.json.JSONObject

class LiveWaterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = runCatching { JSONObject(getSharedPreferences("live-water", MODE_PRIVATE).getString("water", "{}")!!) }.getOrDefault(JSONObject())
        val timestamp = data.optLong("retrievedAt")
        val detail = if (timestamp > 0) java.text.DateFormat.getDateTimeInstance().format(java.util.Date(timestamp)) else "No reading received"
        setContentView(TextView(this).apply {
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            text = "${data.optString("station", "Water temperature")}\nRetrieved: $detail\nReadings expire automatically."
        })
    }
}
