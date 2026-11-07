package org.freakz.botclient.water

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType

/** Separate provider so faces cannot choose a gauge that hides temperature text. */
class WaterTextComplicationService : WaterComplicationService() {
    override fun createData(type: ComplicationType, reading: Float?): ComplicationData? =
        if (type == ComplicationType.SHORT_TEXT) super.createData(type, reading) else null
}
