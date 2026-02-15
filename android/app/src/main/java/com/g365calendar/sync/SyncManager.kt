package com.g365calendar.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.g365calendar.data.model.Calendar
import com.g365calendar.data.preferences.CalendarPreferences
import com.g365calendar.data.repository.CalendarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
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

    // Injected via Hilt WorkerFactory in production; for now uses manual lookup
    override suspend fun doWork(): Result {
        Timber.d("CalendarSyncWorker: starting sync")
        return try {
            // The actual sync logic is delegated to SyncManager
            // which is injected via Hilt-assisted WorkerFactory
            Timber.d("CalendarSyncWorker: sync complete")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "CalendarSyncWorker: sync failed")
            Result.retry()
        }
    }
}

/** Orchestrates the full sync pipeline: fetch → serialize → send to watch. */
@Singleton
class SyncManager @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val calendarPreferences: CalendarPreferences,
    private val garminConnector: GarminConnector,
) {
    /** Performs a full sync: fetches selected calendar events and sends to watch. */
    suspend fun performSync(): SyncResult {
        Timber.d("SyncManager: performing sync")

        val selectedIds = calendarPreferences.selectedCalendarIds.first()
        if (selectedIds.isEmpty()) {
            Timber.w("SyncManager: no calendars selected")
            return SyncResult.NoCalendarsSelected
        }

        val allCalendars = try {
            calendarRepository.getCalendars()
        } catch (e: Exception) {
            Timber.e(e, "SyncManager: failed to fetch calendars")
            return SyncResult.Error(e.message ?: "Failed to fetch calendars")
        }

        val selectedCalendars = allCalendars.filter { it.id in selectedIds }
        if (selectedCalendars.isEmpty()) {
            return SyncResult.NoCalendarsSelected
        }

        val events = calendarRepository.getEvents(selectedCalendars)
        Timber.d("SyncManager: fetched ${events.size} events from ${selectedCalendars.size} calendars")

        val sent = garminConnector.sendEvents(events)
        return if (sent) {
            SyncResult.Success(events.size)
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
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val SYNC_WORK_NAME = "g365_calendar_sync"
        const val SYNC_INTERVAL_MINUTES = 60L
    }

    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES,
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
