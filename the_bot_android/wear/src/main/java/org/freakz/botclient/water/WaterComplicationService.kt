package org.freakz.botclient.water

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

open class WaterComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val value = reading()
        Log.i("BotWaterTest", "${javaClass.simpleName} request id=${request.complicationInstanceId} type=${request.complicationType} value=$value")
        return createData(request.complicationType, value)
    }

    protected open fun reading(): Float? = WaterReading.load(this)
    protected open fun tapIntent() = Intent(this, WaterTestActivity::class.java)

    override fun getPreviewData(type: ComplicationType): ComplicationData? = createData(type, WaterReading.DEFAULT)

    internal open fun createData(type: ComplicationType, reading: Float?): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT && type != ComplicationType.RANGED_VALUE) return null
        val value = WaterReading.valid(reading)
        val text = PlainComplicationText.Builder(WaterReading.text(value)).build()
        val description = PlainComplicationText.Builder(
            if (value == null) "Test water temperature unavailable" else "Test water temperature $value degrees Celsius"
        ).build()
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_water)).build()
        val tap = PendingIntent.getActivity(this, 0, tapIntent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val short = ShortTextComplicationData.Builder(text, description)
            .setMonochromaticImage(icon).setTapAction(tap).build()
        if (value == null) return NoDataComplicationData(short)
        return when (type) {
            ComplicationType.SHORT_TEXT -> short
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(value, WaterReading.MIN, WaterReading.MAX, description)
                .setText(text).setMonochromaticImage(icon).setTapAction(tap).build()
            else -> null
        }
    }
}
