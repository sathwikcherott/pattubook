package com.pattubook.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountInputUtilTest {
    @Test
    fun parsesValidRupeesWithoutFloatingPointRounding() {
        assertEquals(25_050L, AmountInputUtil.parseRupeesToPaise("250.5"))
        assertEquals(50L, AmountInputUtil.parseRupeesToPaise(".50"))
    }

    @Test
    fun rejectsValuesThatOverflowPaiseStorage() {
        assertNull(AmountInputUtil.parseRupeesToPaise("92233720368547759"))
    }
}
