package com.pattubook.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object PattubookWidgetUpdater {
    suspend fun updateWidget(context: Context) {
        try {
            PattubookWidget().updateAll(context)
        } catch (_: Exception) {
        }
    }
}
