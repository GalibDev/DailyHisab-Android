package com.dailyhisab.android.feature.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BudgetPeriodTest {
    @Test fun `monthly period uses full calendar month`() {
        assertEquals(
            LocalDate.of(2026, 2, 1) to LocalDate.of(2026, 2, 28),
            periodRange(BudgetPeriod.Monthly, LocalDate.of(2026, 2, 12), LocalDate.of(2026, 2, 12)),
        )
    }

    @Test fun `custom period rejects reversed dates`() {
        assertNull(periodRange(BudgetPeriod.Custom, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1)))
    }
}
