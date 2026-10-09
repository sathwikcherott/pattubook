package com.pattubook.app.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {
    fun formatEpochMillis(epochMillis: Long): String {
        val date = Date(epochMillis)
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return sdf.format(date)
    }

    fun formatShortDate(epochMillis: Long): String {
        val date = Date(epochMillis)
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        return sdf.format(date)
    }

    fun formatRelativeDay(epochMillis: Long): String {
        val entryCalendar = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val todayCalendar = Calendar.getInstance()
        val yesterdayCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        return when {
            entryCalendar.isSameDay(todayCalendar) -> "Today"
            entryCalendar.isSameDay(yesterdayCalendar) -> "Yesterday"
            else -> formatShortDate(epochMillis)
        }
    }

    fun formatRelativeUpdate(epochMillis: Long): String {
        return "Updated ${formatRelativeDay(epochMillis).lowercase(Locale.getDefault())}"
    }

    private fun Calendar.isSameDay(other: Calendar): Boolean {
        return get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    }
}
