package com.example.screentimeouttoggle

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.RemoteViews
import android.net.Uri

class TimeoutToggleWidget : AppWidgetProvider() {

    companion object {
        private const val ACTION_TOGGLE = "TOGGLE_TIMEOUT"
        private const val TIMEOUT_SHORT = 2 * 60 * 1000      // 2 minutes
        private const val TIMEOUT_LONG = 30 * 60 * 1000     // 30 minutes
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE) {

            if (!Settings.System.canWrite(context)) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
                return
            }

            val currentTimeout = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                TIMEOUT_SHORT
            )

            val newTimeout =
                if (currentTimeout <= TIMEOUT_SHORT) TIMEOUT_LONG else TIMEOUT_SHORT

            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                newTimeout
            )

            // Force widget refresh after toggle
            val manager = AppWidgetManager.getInstance(context)
            val widgetIds = manager.getAppWidgetIds(
                android.content.ComponentName(context, TimeoutToggleWidget::class.java)
            )

            for (id in widgetIds) {
                updateWidget(context, manager, id)
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.timeout_toggle_widget)

        val timeoutMs = Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            TIMEOUT_SHORT
        )

        val timeoutMinutes = timeoutMs / 60000

        views.setTextViewText(
            R.id.widgetText,
            "Screen Time Out: $timeoutMinutes min"
        )

        val intent = Intent(context, TimeoutToggleWidget::class.java).apply {
            action = ACTION_TOGGLE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.widgetText, pendingIntent)
        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
