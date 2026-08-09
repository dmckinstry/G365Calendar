package com.g365calendar.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager periodic worker that syncs M365 calendar events to the Garmin watch.
 * Runs every 60 minutes with network connectivity constraint.
 */
class CalendarSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        Timber.d("CalendarSyncWorker: starting sync")
        return try {
            val entryPoint =
                EntryPointAccessors.fromApplication(
                    applicationContext,
                    CalendarSyncWorkerEntryPoint::class.java,
                )
            when (val result = entryPoint.syncManager().performSync()) {
                is SyncResult.Success -> {
                    Timber.d("CalendarSyncWorker: synced ${result.eventCount} events")
                    Result.success()
                }
                SyncResult.NoCalendarsSelected -> {
                    Timber.d("CalendarSyncWorker: no calendars selected")
                    Result.success()
                }
                SyncResult.WatchNotConnected -> {
                    Timber.d("CalendarSyncWorker: watch not connected")
                    Result.success()
                }
                is SyncResult.Error -> {
                    Timber.w("CalendarSyncWorker: sync failed: ${result.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "CalendarSyncWorker: sync failed")
            Result.retry()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CalendarSyncWorkerEntryPoint {
    fun syncManager(): SyncManager
}

/** Orchestrates the full sync pipeline: fetch → serialize → send to watch. */
@Singleton
class SyncManager
    @Inject
    constructor(
        private val selectedEventsProvider: SelectedEventsProvider,
        private val eventSerializer: EventSerializer,
        private val garminConnector: GarminConnector,
    ) {
        /** Performs a full sync: fetches selected calendar events and sends to watch. */
        suspend fun performSync(): SyncResult {
            Timber.d("SyncManager: performing sync")

            val selectedEvents =
                try {
                    selectedEventsProvider.loadSelectedEvents()
                } catch (e: Exception) {
                    Timber.e(e, "SyncManager: failed to fetch selected events")
                    return SyncResult.Error(e.message ?: "Failed to fetch calendars")
                }

            if (!selectedEvents.hasAvailableSelectedCalendars) {
                Timber.w("SyncManager: no calendars selected across any account")
                return SyncResult.NoCalendarsSelected
            }

            val payload = eventSerializer.buildPayload(selectedEvents.events)
            val sent = garminConnector.sendPayload(payload)
            return if (sent) {
                SyncResult.Success(payload.eventCount)
            } else {
                SyncResult.WatchNotConnected
            }
        }
    }

sealed class SyncResult {
    data class Success(val eventCount: Int) : SyncResult()

    data object NoCalendarsSelected : SyncResult()

    data object WatchNotConnected : SyncResult()

    data class Error(val message: String) : SyncResult()
}

/** Schedules and manages the periodic sync WorkManager job. */
@Singleton
class SyncScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            const val SYNC_WORK_NAME = "g365_calendar_sync"
            const val SYNC_INTERVAL_MINUTES = 60L
        }

        fun schedulePeriodicSync() {
            val constraints =
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val syncRequest =
                PeriodicWorkRequestBuilder<CalendarSyncWorker>(
                    SYNC_INTERVAL_MINUTES,
                    TimeUnit.MINUTES,
                )
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest,
            )

            Timber.d("Periodic sync scheduled every $SYNC_INTERVAL_MINUTES minutes")
        }

        fun cancelPeriodicSync() {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
            Timber.d("Periodic sync cancelled")
        }
    }
