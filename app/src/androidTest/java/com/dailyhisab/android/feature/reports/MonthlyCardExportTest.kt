package com.dailyhisab.android.feature.reports

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.YearMonth
import android.graphics.Bitmap

@RunWith(AndroidJUnit4::class)
class MonthlyCardExportTest {
    @Test fun pngCardRendersAtStoreReadyResolution() {
        val bitmap = MonthlyCardExporter.render(YearMonth.of(2026, 10), 250000, 125000, false, "BDT")
        assertEquals(1080, bitmap.width)
        assertEquals(1350, bitmap.height)
        val bytes = ByteArrayOutputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            output.toByteArray()
        }
        assertTrue(bytes.size > 10_000)
        bitmap.recycle()
    }
}
