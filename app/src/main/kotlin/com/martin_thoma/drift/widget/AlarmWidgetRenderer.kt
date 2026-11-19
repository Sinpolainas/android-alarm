package com.martin_thoma.drift.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import com.martin_thoma.drift.MainActivity
import com.martin_thoma.drift.R
import com.martin_thoma.drift.core.domain.NextTrigger
import com.martin_thoma.drift.core.model.Alarm
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Draws the alarm widgets: purple and counting down when the alarm is on, grey when it is off. */
internal object AlarmWidgetRenderer {
    fun updateAll(
        context: Context,
        alarms: List<Alarm>,
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, AlarmWidgetProvider::class.java))
        ids.forEach { update(context, manager, it, alarms) }
    }

    fun update(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        alarms: List<Alarm>,
    ) {
        val alarmId = AlarmWidgetPrefs.alarmId(context, widgetId)
        val alarm = alarms.find { it.id == alarmId }
        val views = RemoteViews(context.packageName, R.layout.widget_alarm)

        if (alarm == null) {
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_off)
            views.setTextViewText(R.id.widget_time, "--:--")
            views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_pick_alarm))
            views.setViewVisibility(R.id.widget_countdown_row, View.GONE)
            views.setViewVisibility(R.id.widget_status, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.widget_root, configureIntent(context, widgetId))
            views.setOnClickPendingIntent(R.id.widget_toggle, configureIntent(context, widgetId))
            manager.updateAppWidget(widgetId, views)
            return
        }

        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        views.setTextViewText(R.id.widget_time, alarm.time.format(DateTimeFormatter.ofPattern(pattern)))
        views.setTextViewText(R.id.widget_label, labelLine(context, alarm))

        val trigger = NextTrigger.of(alarm, ZonedDateTime.now())
        if (alarm.isActive && trigger != null) {
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_on)
            views.setImageViewResource(R.id.widget_toggle, R.drawable.ic_widget_toggle_on)
            val msLeft = trigger.toInstant().toEpochMilli() - System.currentTimeMillis()
            views.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime() + msLeft, null, true)
            views.setChronometerCountDown(R.id.widget_countdown, true)
            views.setViewVisibility(R.id.widget_countdown_row, View.VISIBLE)
            views.setViewVisibility(R.id.widget_status, View.GONE)
        } else {
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_off)
            views.setImageViewResource(R.id.widget_toggle, R.drawable.ic_widget_toggle_off)
            views.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime(), null, false)
            views.setViewVisibility(R.id.widget_countdown_row, View.GONE)
            views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_off))
            views.setViewVisibility(R.id.widget_status, View.VISIBLE)
        }

        views.setOnClickPendingIntent(R.id.widget_toggle, toggleIntent(context, widgetId, alarm.id))
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context, widgetId))
        manager.updateAppWidget(widgetId, views)
    }

    fun labelLine(
        context: Context,
        alarm: Alarm,
    ): String {
        val days =
            when {
                !alarm.isRepeating -> context.getString(R.string.widget_once)
                alarm.repeatDays.size == 7 -> context.getString(R.string.widget_every_day)
                else ->
                    DayOfWeek.entries
                        .filter { it in alarm.repeatDays }
                        .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
            }
        return if (alarm.label.isBlank()) days else "${alarm.label} · $days"
    }

    private fun toggleIntent(
        context: Context,
        widgetId: Int,
        alarmId: Long,
    ): PendingIntent {
        val intent =
            Intent(context, AlarmWidgetProvider::class.java)
                .setAction(AlarmWidgetProvider.ACTION_TOGGLE)
                .putExtra(AlarmWidgetProvider.EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppIntent(
        context: Context,
        widgetId: Int,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            widgetId,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun configureIntent(
        context: Context,
        widgetId: Int,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            widgetId + 100_000,
            Intent(context, AlarmWidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
