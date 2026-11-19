package com.martin_thoma.drift.widget

import com.martin_thoma.drift.core.data.AlarmRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets the widget classes, which Android creates, reach the alarm database. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AlarmWidgetEntryPoint {
    fun alarmRepository(): AlarmRepository
}
