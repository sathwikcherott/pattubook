package com.pattubook.app.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    fun formatEpochMillis(epochMillis: Long): String {
        val date = Date(epochMillis)
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return sdf.format(date)
    }
}
