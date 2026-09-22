package com.pattubook.app.ui.util

object AmountInputUtil {
    /**
     * Parses a string representation of Rupees (e.g., "100", "100.5", "100.50", "0.50")
     * into exact Long paise without floating-point conversion.
     * Returns null if the input is invalid, zero, or negative.
     */
    fun parseRupeesToPaise(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        if (trimmed.contains("-") || trimmed.contains("+")) return null

        val parts = trimmed.split(".")
        if (parts.size > 2) return null

        val rupeesPart = parts[0]
        val paisePart = if (parts.size == 2) parts[1] else ""

        if (paisePart.length > 2) return null

        val rupeesLong = if (rupeesPart.isEmpty()) 0L else rupeesPart.toLongOrNull() ?: return null

        val paiseLong = when (paisePart.length) {
            0 -> 0L
            1 -> (paisePart + "0").toLongOrNull() ?: return null
            2 -> paisePart.toLongOrNull() ?: return null
            else -> return null
        }

        val totalPaise = rupeesLong * 100L + paiseLong
        return if (totalPaise > 0L) totalPaise else null
    }
}
