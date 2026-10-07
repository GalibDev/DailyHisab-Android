package com.dailyhisab.android.feature.loan

import com.dailyhisab.android.data.local.entity.LoanEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LoanStatusTest {
    private val today = LocalDate.of(2026, 10, 8)

    @Test fun `fully repaid loan is paid even after due date`() {
        assertEquals(LoanStatus.Paid, loanStatus(loan(today.minusDays(2), 10_000), today))
    }

    @Test fun `unpaid past loan is overdue`() {
        assertEquals(LoanStatus.Overdue, loanStatus(loan(today.minusDays(1), 0), today))
    }

    @Test fun `loan within three days is due soon`() {
        assertEquals(LoanStatus.DueSoon, loanStatus(loan(today.plusDays(3), 0), today))
    }

    private fun loan(due: LocalDate, repaid: Long) = LoanEntity(
        personName = "Test", amountMinor = 10_000, repaidMinor = repaid,
        direction = LoanDirection.Borrowed.name, dueEpochDay = due.toEpochDay(),
    )
}
