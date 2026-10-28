package com.pattubook.app.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatterTest {
    @Test
    fun pickerDateMillisPreservesLocalCalendarDayInPositiveOffsetZone() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
            val localDate = LocalDate.of(2026, 10, 19)
            val localTimestamp = localDate.atTime(2, 15)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val pickerDate = Instant.ofEpochMilli(DateFormatter.toUtcDateMillis(localTimestamp))
                .atZone(ZoneOffset.UTC)
                .toLocalDate()

            assertEquals(localDate, pickerDate)
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
}
