package com.martin_thoma.drift.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** One widget per alarm: its time, the time left until it rings, and a button to turn it on or off. */
class AlarmWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        scope.launch {
            try {
                val alarms = repository(context).getAll()
                appWidgetIds.forEach { AlarmWidgetRenderer.update(context, appWidgetManager, it, alarms) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_TOGGLE) {
            super.onReceive(context, intent)
            return
        }
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0) return
        val pending = goAsync()
        scope.launch {
            try {
                val repo = repository(context)
                val alarm = repo.get(alarmId)
                if (alarm != null) {
                    // On if it was off; off (and any snooze ended) if it was on.
                    repo.setEnabled(alarmId, !alarm.isActive)
                }
                AlarmWidgetRenderer.updateAll(context, repo.getAll())
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { AlarmWidgetPrefs.remove(context, it) }
    }

    companion object {
        const val ACTION_TOGGLE = "com.martin_thoma.drift.widget.TOGGLE"
        const val EXTRA_ALARM_ID = "alarm_id"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        internal fun repository(context: Context) =
            EntryPointAccessors
                .fromApplication(context.applicationContext, AlarmWidgetEntryPoint::class.java)
                .alarmRepository()
    }
}
