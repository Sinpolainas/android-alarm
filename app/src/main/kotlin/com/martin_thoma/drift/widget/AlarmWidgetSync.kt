package com.martin_thoma.drift.widget

import android.content.Context
import com.martin_thoma.drift.core.data.AlarmRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/** Redraws the alarm widgets whenever an alarm changes in the app. Started once by the Application. */
@Singleton
class AlarmWidgetSync
    @Inject
    constructor(
        private val repository: AlarmRepository,
        @param:ApplicationContext private val context: Context,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun start() {
            repository.alarms.onEach { AlarmWidgetRenderer.updateAll(context, it) }.launchIn(scope)
        }
    }
