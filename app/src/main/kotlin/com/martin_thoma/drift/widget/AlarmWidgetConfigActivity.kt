package com.martin_thoma.drift.widget

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import com.martin_thoma.drift.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

/** Asks which alarm a new widget should show. Also opens again from the launcher's widget settings. */
class AlarmWidgetConfigActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out without a choice must not leave an empty widget behind.
        setResult(RESULT_CANCELED, resultIntent())
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        scope.launch {
            val repo = AlarmWidgetProvider.repository(this@AlarmWidgetConfigActivity)
            val alarms = withContext(Dispatchers.IO) { repo.getAll() }
            if (alarms.isEmpty()) {
                Toast.makeText(this@AlarmWidgetConfigActivity, R.string.widget_no_alarms, Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            val pattern = if (DateFormat.is24HourFormat(this@AlarmWidgetConfigActivity)) "HH:mm" else "h:mm a"
            val formatter = DateTimeFormatter.ofPattern(pattern)
            val names =
                alarms
                    .map { "${it.time.format(formatter)}  ·  ${AlarmWidgetRenderer.labelLine(this@AlarmWidgetConfigActivity, it)}" }
                    .toTypedArray()
            AlertDialog
                .Builder(this@AlarmWidgetConfigActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.widget_choose_alarm)
                .setItems(names) { _, which ->
                    AlarmWidgetPrefs.setAlarmId(this@AlarmWidgetConfigActivity, widgetId, alarms[which].id)
                    AlarmWidgetRenderer.update(
                        this@AlarmWidgetConfigActivity,
                        AppWidgetManager.getInstance(this@AlarmWidgetConfigActivity),
                        widgetId,
                        alarms,
                    )
                    setResult(RESULT_OK, resultIntent())
                    finish()
                }.setOnCancelListener { finish() }
                .show()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
}
