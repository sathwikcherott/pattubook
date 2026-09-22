package com.pattubook.app.ui.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {
    /**
     * Formats an amount in paise to a clean Indian Rupee string (e.g. ₹12,450 or -₹200).
     */
    fun formatPaiseToRupees(paise: Long, showPaiseDigits: Boolean = false): String {
        val isNegative = paise < 0
        val absPaise = abs(paise)
        val rupees = absPaise / 100
        val remainingPaise = absPaise % 100

        val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }

        val formattedRupees = numberFormat.format(rupees)
        val prefix = if (isNegative) "-₹" else "₹"

        return if (showPaiseDigits || remainingPaise > 0) {
            val paiseString = remainingPaise.toString().padStart(2, '0')
            "$prefix$formattedRupees.$paiseString"
        } else {
            "$prefix$formattedRupees"
        }
    }
}
