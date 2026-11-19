package com.martin_thoma.drift.widget

import android.content.Context

/** Which alarm each placed widget shows. */
internal object AlarmWidgetPrefs {
    private const val FILE = "alarm_widgets"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun alarmId(
        context: Context,
        widgetId: Int,
    ): Long? = prefs(context).getLong(key(widgetId), -1L).takeIf { it > 0 }

    fun setAlarmId(
        context: Context,
        widgetId: Int,
        alarmId: Long,
    ) = prefs(context).edit().putLong(key(widgetId), alarmId).apply()

    fun remove(
        context: Context,
        widgetId: Int,
    ) = prefs(context).edit().remove(key(widgetId)).apply()

    private fun key(widgetId: Int) = "widget_$widgetId"
}
