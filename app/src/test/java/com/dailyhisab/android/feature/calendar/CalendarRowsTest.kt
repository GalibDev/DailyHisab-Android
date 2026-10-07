package com.dailyhisab.android.feature.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarRowsTest {
    @Test
    fun `calendar starts on Monday and contains every day`() {
        val rows = calendarRows(YearMonth.of(2026, 10))

        assertEquals(5, rows.size)
        assertNull(rows.first().first())
        assertEquals(LocalDate.of(2026, 10, 1), rows.first()[3])
        assertEquals(LocalDate.of(2026, 10, 31), rows.last()[5])
    }
}
