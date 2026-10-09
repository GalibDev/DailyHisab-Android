package com.dailyhisab.android.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderSchedulerTest {
    private val zone = ZoneId.of("Asia/Dhaka")

    @Test fun `future reminder stays on same day`() {
        val now = ZonedDateTime.of(2026, 10, 9, 8, 30, 0, 0, zone)
        assertEquals(30 * 60L, nextReminderDelay(now, 9, 0).seconds)
    }

    @Test fun `past reminder rolls to tomorrow`() {
        val now = ZonedDateTime.of(2026, 10, 9, 21, 0, 0, 0, zone)
        val delay = nextReminderDelay(now, 20, 0)
        assertEquals(23 * 60 * 60L, delay.seconds)
        assertTrue(!delay.isNegative && !delay.isZero)
    }

    @Test fun `invalid clock values are safely clamped`() {
        val now = ZonedDateTime.of(2026, 10, 9, 22, 0, 0, 0, zone)
        assertEquals((1 * 60 + 59) * 60L, nextReminderDelay(now, 99, 99).seconds)
    }
}
