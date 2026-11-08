package org.freakz.botclient.water

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.SharedPreferences
import android.view.Gravity
import android.widget.*
import com.google.android.gms.wearable.*
import org.json.JSONObject

class LiveWaterActivity : Activity(), MessageClient.OnMessageReceivedListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var detail: TextView
    private lateinit var status: TextView
    private lateinit var button: Button
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { finishRefresh("No response. Check your phone and try again.") }
    private val prefs by lazy { getSharedPreferences("live-water", MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        detail = TextView(this).apply { gravity = Gravity.CENTER }
        status = TextView(this).apply { gravity = Gravity.CENTER }
        button = Button(this).apply { text = "Update now"; setOnClickListener { refresh() } }
        setContentView(ScrollView(this).apply {
            addView(LinearLayout(this@LiveWaterActivity).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(32, 32, 32, 32)
                addView(detail); addView(button); addView(status)
            })
        })
        render()
    }
    override fun onStart() {
        super.onStart()
        Wearable.getMessageClient(this).addListener(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        render()
    }
    override fun onStop() {
        Wearable.getMessageClient(this).removeListener(this)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        handler.removeCallbacks(timeout)
        button.isEnabled = true
        super.onStop()
    }
    private fun render() {
        val data = runCatching { JSONObject(prefs.getString("water", "{}")!!) }.getOrDefault(JSONObject())
        val time = data.optLong("retrievedAt")
        val stamp = if (time > 0) java.text.DateFormat.getDateTimeInstance().format(java.util.Date(time)) else "No reading received"
        detail.text = "${data.optString("station", "Water temperature")}\nRetrieved: $stamp"
    }
    private fun refresh() {
        button.isEnabled = false; status.text = "Refreshing…"
        handler.postDelayed(timeout, 120000)
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            val phone = nodes.firstOrNull()
            if (phone == null) finishRefresh("Phone not connected")
            else Wearable.getMessageClient(this).sendMessage(phone.id, "/water/refresh", byteArrayOf())
                .addOnFailureListener { finishRefresh("Could not contact phone") }
        }.addOnFailureListener { finishRefresh("Could not contact phone") }
    }
    private fun finishRefresh(message: String) {
        handler.removeCallbacks(timeout); button.isEnabled = true; status.text = message; render()
    }
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == "/water/refresh-result") runOnUiThread { finishRefresh(String(event.data)) }
    }
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) { runOnUiThread { render() } }
}
