package org.freakz.botclient.water

import android.content.Context
import java.util.Locale

object WaterReading {
    const val DEFAULT = 4.2f
    const val MIN = -10f
    const val MAX = 45f
    fun valid(value: Float?): Float? = value?.takeIf { it.isFinite() && it in MIN..MAX }
    fun text(value: Float?): String = valid(value)?.let { String.format(Locale.ROOT, "%.1f°C", it) } ?: "--"
    fun load(context: Context): Float? = valid(context.getSharedPreferences("water-test", Context.MODE_PRIVATE)
        .getFloat("temperature", DEFAULT))
    fun save(context: Context, value: Float?) {
        context.getSharedPreferences("water-test", Context.MODE_PRIVATE).edit()
            .putFloat("temperature", valid(value) ?: Float.NaN).apply()
    }
}
