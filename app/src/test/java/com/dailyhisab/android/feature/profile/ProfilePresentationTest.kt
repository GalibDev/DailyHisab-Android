package com.dailyhisab.android.feature.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfilePresentationTest {
    @Test fun `initials use first two name parts`() {
        assertEquals("MG", initials("Mirza Galib Palash"))
    }

    @Test fun `blank name has guest fallback`() {
        assertEquals("G", initials("  "))
    }
}
