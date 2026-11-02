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

    @Test
    fun parsesAdditionExpressionsToExactPaise() {
        assertEquals(11_400L, AmountInputUtil.parseAdditionExpressionToPaise("58+56"))
        assertEquals(17_500L, AmountInputUtil.parseAdditionExpressionToPaise("100+50+25"))
        assertEquals(53_500L, AmountInputUtil.parseAdditionExpressionToPaise("500+20+10+5"))
        assertEquals(3_075L, AmountInputUtil.parseAdditionExpressionToPaise("10.50+20.25"))
        assertEquals(30L, AmountInputUtil.parseAdditionExpressionToPaise("0.10+0.20"))
    }

    @Test
    fun rejectsEmptyIncompleteAndInvalidExpressions() {
        assertNull(AmountInputUtil.parseAdditionExpressionToPaise(""))
        assertNull(AmountInputUtil.parseAdditionExpressionToPaise("58+"))
        assertNull(AmountInputUtil.parseAdditionExpressionToPaise("+58"))
        assertNull(AmountInputUtil.parseAdditionExpressionToPaise("58++56"))
        assertNull(AmountInputUtil.parseAdditionExpressionToPaise("10.501+2"))
        assertNull(AmountInputUtil.parseAdditionExpressionToPaise("-10+2"))
        assertNull(AmountInputUtil.parseAdditionExpressionToPaise("0+0"))
        assertNull(
            AmountInputUtil.parseAdditionExpressionToPaise(
                "92233720368547758+92233720368547758"
            )
        )
    }

    @Test
    fun amountKeyOperationsSupportBackspaceAndIgnoreRepeatedPlus() {
        val afterFirstPlus = AmountInputUtil.appendAdditionOperator("58")
        assertEquals("58+", afterFirstPlus)
        assertEquals("58+", AmountInputUtil.appendAdditionOperator(afterFirstPlus))
        assertEquals("58", AmountInputUtil.removeLastInputCharacter(afterFirstPlus))

        val expression = AmountInputUtil.appendDigit("58+5", '6')
        assertEquals("58+56", expression)
        assertEquals("58+5", AmountInputUtil.removeLastInputCharacter(expression))
        assertEquals("58 + 56", AmountInputUtil.formatAdditionExpression(expression))
    }

    @Test
    fun decimalKeyBuildsAnOperandAndStopsAfterTwoPaiseDigits() {
        var expression = AmountInputUtil.appendDecimalPoint("")
        expression = AmountInputUtil.appendDigit(expression, '1')
        expression = AmountInputUtil.appendDigit(expression, '0')
        expression = AmountInputUtil.appendDecimalPoint(expression)
        expression = AmountInputUtil.appendDigit(expression, '5')
        expression = AmountInputUtil.appendDigit(expression, '0')
        expression = AmountInputUtil.appendDigit(expression, '1')

        assertEquals("0.10", expression)
        assertEquals(10L, AmountInputUtil.parseAdditionExpressionToPaise(expression))
    }

    @Test
    fun previewShowsCompletedSubtotalForTrailingPlusButSaveRemainsInvalid() {
        assertEquals(5_800L, AmountInputUtil.previewAdditionExpressionToPaise("58+"))
        assertNull(AmountInputUtil.parseAdditionExpressionToPaise("58+"))
        assertNull(AmountInputUtil.previewAdditionExpressionToPaise("58++"))
    }
}
