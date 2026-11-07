package org.freakz.botclient.water

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

class WaterTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (32 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
        }
        content.addView(TextView(this).apply { setText(R.string.test_heading); gravity = Gravity.CENTER })
        val current = TextView(this).apply { gravity = Gravity.CENTER }
        fun updateLabel() { current.text = getString(R.string.current_value, WaterReading.text(WaterReading.load(this))) }
        updateLabel()
        content.addView(current)
        listOf(4.2f, 12.5f, 0f, null).forEach { reading ->
            content.addView(Button(this).apply {
                text = if (reading == null) getString(R.string.unavailable) else WaterReading.text(reading)
                setOnClickListener {
                    WaterReading.save(this@WaterTestActivity, reading)
                    updateLabel()
                    ComplicationDataSourceUpdateRequester.create(this@WaterTestActivity,
                        ComponentName(this@WaterTestActivity, WaterComplicationService::class.java)).requestUpdateAll()
                    Log.i("BotWaterTest", "Selected $reading; refresh requested at ${System.currentTimeMillis()}")
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        setContentView(ScrollView(this).apply { addView(content) })
    }
}
