package com.pattubook.app.ui.util

import java.util.Locale

object AmountInputUtil {
    /**
     * Parses a string representation of Rupees (e.g., "100", "100.5", "100.50", "0.50")
     * into exact Long paise without floating-point conversion.
     * Returns null if the input is invalid, zero, or negative.
     */
    fun parseRupeesToPaise(input: String): Long? {
        return parseRupeesToPaise(input, allowZero = false)
    }

    /**
     * Parses an addition-only amount expression to exact paise. Empty operands, trailing operators,
     * invalid decimals, negative amounts, and totals that overflow Long are rejected.
     */
    fun parseAdditionExpressionToPaise(expression: String): Long? {
        if (expression.isBlank()) return null

        val operands = expression.split('+')
        if (operands.any { it.isBlank() }) return null

        var totalPaise = 0L
        for (operand in operands) {
            val operandPaise = parseRupeesToPaise(operand, allowZero = true) ?: return null
            totalPaise = try {
                Math.addExact(totalPaise, operandPaise)
            } catch (_: ArithmeticException) {
                return null
            }
        }
        return totalPaise.takeIf { it > 0L }
    }

    /** Returns a subtotal while the user is entering an expression such as `58+`. */
    fun previewAdditionExpressionToPaise(expression: String): Long? {
        if (expression.isBlank()) return null

        val operands = expression.split('+')
        val hasTrailingOperator = expression.endsWith('+')
        val completedOperandCount = if (hasTrailingOperator) operands.size - 1 else operands.size
        if (completedOperandCount <= 0 || operands.take(completedOperandCount).any { it.isBlank() }) {
            return null
        }

        var totalPaise = 0L
        for (index in 0 until completedOperandCount) {
            val operandPaise = parseRupeesToPaise(operands[index], allowZero = true) ?: return null
            totalPaise = try {
                Math.addExact(totalPaise, operandPaise)
            } catch (_: ArithmeticException) {
                return null
            }
        }
        return totalPaise
    }

    fun appendDigit(expression: String, digit: Char): String {
        if (digit !in '0'..'9') return expression
        val currentOperand = expression.substringAfterLast('+')
        if (currentOperand.substringAfter('.', "").length >= 2 && currentOperand.contains('.')) {
            return expression
        }
        return expression + digit
    }

    fun appendDecimalPoint(expression: String): String {
        val currentOperand = expression.substringAfterLast('+')
        if (currentOperand.contains('.')) return expression
        return if (currentOperand.isEmpty()) expression + "0." else expression + "."
    }

    fun appendAdditionOperator(expression: String): String {
        val currentOperand = expression.substringAfterLast('+')
        if (parseRupeesToPaise(currentOperand, allowZero = true) == null) return expression
        return expression + "+"
    }

    fun removeLastInputCharacter(expression: String): String = expression.dropLast(1)

    fun formatAdditionExpression(expression: String): String = expression.replace("+", " + ")

    private fun parseRupeesToPaise(input: String, allowZero: Boolean): Long? {
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

        val totalPaise = try {
            Math.addExact(Math.multiplyExact(rupeesLong, 100L), paiseLong)
        } catch (_: ArithmeticException) {
            return null
        }
        return if (totalPaise > 0L || (allowZero && totalPaise == 0L)) totalPaise else null
    }

    /**
     * Formats Long paise back to a plain Rupees string for editing (e.g., 50000 -> "500", 50050 -> "500.50").
     */
    fun formatPaiseToPlainRupees(paise: Long): String {
        val rupees = paise / 100L
        val remainingPaise = paise % 100L
        return if (remainingPaise == 0L) {
            rupees.toString()
        } else {
            String.format(Locale.US, "%d.%02d", rupees, remainingPaise)
        }
    }
}
