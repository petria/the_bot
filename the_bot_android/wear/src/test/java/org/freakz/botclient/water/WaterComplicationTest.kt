package org.freakz.botclient.water

import android.view.ViewGroup
import android.widget.Button
import androidx.wear.watchface.complications.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class WaterComplicationTest {
    private val service = Robolectric.buildService(WaterComplicationService::class.java).create().get()

    @Test fun formatsIndependentlyOfPhoneLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("4.2°C", WaterReading.text(4.2f))
            assertEquals("0.0°C", WaterReading.text(0f))
            assertEquals("12.5°C", WaterReading.text(12.5f))
        } finally { Locale.setDefault(original) }
    }
    @Test fun missingAndInvalidValuesAreUnavailable() {
        listOf(null, Float.NaN, Float.POSITIVE_INFINITY, -11f, 46f).forEach {
            assertEquals("--", WaterReading.text(it))
            for (type in listOf(ComplicationType.SHORT_TEXT, ComplicationType.RANGED_VALUE)) {
                val data = service.createData(type, it) as NoDataComplicationData
                val placeholder = data.placeholder as ShortTextComplicationData
                assertEquals("--", placeholder.text.getTextAt(service.resources, Instant.EPOCH).toString())
            }
        }
    }
    @Test fun suppliesBothSlotTypesAndTapAction() {
        val short = service.createData(ComplicationType.SHORT_TEXT, 4.2f) as ShortTextComplicationData
        assertEquals("4.2°C", short.text.getTextAt(service.resources, Instant.EPOCH).toString())
        assertNotNull(short.monochromaticImage)
        assertNotNull(short.tapAction)
        val ranged = service.createData(ComplicationType.RANGED_VALUE, 12.5f) as RangedValueComplicationData
        assertEquals(12.5f, ranged.value)
        assertEquals(-10f, ranged.min)
        assertEquals(45f, ranged.max)
        assertNotNull(ranged.tapAction)
        assertNull(service.createData(ComplicationType.LONG_TEXT, 4.2f))
    }
    @Test fun testScreenButtonsSaveReadings() {
        val activity = Robolectric.buildActivity(WaterTestActivity::class.java).setup().get()
        fun buttons(group: ViewGroup): List<Button> = (0 until group.childCount).flatMap {
            when (val child = group.getChildAt(it)) {
                is Button -> listOf(child)
                is ViewGroup -> buttons(child)
                else -> emptyList()
            }
        }
        val controls = buttons(activity.findViewById(android.R.id.content))
        assertEquals(4, controls.size)
        controls.first { it.text == "12.5°C" }.performClick()
        assertEquals(12.5f, WaterReading.load(activity))
        controls.first { it.text == "0.0°C" }.performClick()
        assertEquals(0f, WaterReading.load(activity))
        controls.last().performClick()
        assertNull(WaterReading.load(activity))
        activity.finish()
    }
    @Test fun persistsZeroAndUnavailableWithoutRestoringDefault() {
        WaterReading.save(service, 0f)
        assertEquals(0f, WaterReading.load(service))
        WaterReading.save(service, null)
        assertNull(WaterReading.load(service))
    }
}
