package com.g365calendar.ui

import com.g365calendar.auth.AuthAccount
import com.g365calendar.auth.AuthManager
import com.g365calendar.auth.AuthState
import com.g365calendar.data.model.Calendar
import com.g365calendar.data.model.DisplayEvent
import com.g365calendar.data.preferences.CalendarPreferences
import com.g365calendar.data.repository.CalendarRepository
import com.g365calendar.sync.EventSerializer
import com.g365calendar.sync.SelectedEventsLoadResult
import com.g365calendar.sync.SelectedEventsProvider
import com.g365calendar.sync.SyncManager
import com.g365calendar.sync.SyncResult
import com.g365calendar.sync.SyncScheduler
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private lateinit var authManager: AuthManager
    private lateinit var authState: AuthState
    private lateinit var calendarPreferences: CalendarPreferences
    private lateinit var calendarRepository: CalendarRepository
    private lateinit var selectedEventsProvider: SelectedEventsProvider
    private lateinit var eventSerializer: EventSerializer
    private lateinit var syncManager: SyncManager
    private lateinit var syncScheduler: SyncScheduler

    private val dispatcher = StandardTestDispatcher()

    private val testAccount = AuthAccount(id = "account-1", displayName = "Test User", username = "user@example.com")
    private val testCalendar =
        Calendar(
            id = "calendar-1",
            name = "Work",
            color = "blue",
            hexColor = "#0078D4",
        )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authManager = mockk()
        authState = AuthState()
        calendarPreferences = mockk()
        calendarRepository = mockk()
        selectedEventsProvider = mockk()
        eventSerializer = EventSerializer(com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build())
        syncManager = mockk(relaxed = true)
        syncScheduler = mockk(relaxed = true)
    }

    @Test
    fun `init triggers sync when accounts are restored on launch`() =
        runTest(dispatcher) {
            coEvery { authManager.getAccounts() } returns listOf(testAccount)
            every { calendarPreferences.selectedCalendarIds(testAccount.id) } returns flowOf(setOf(testCalendar.id))
            coEvery { calendarRepository.getCalendars(testAccount.id) } returns listOf(testCalendar)
            coEvery { syncManager.performSync() } returns SyncResult.Success(1)

            MainViewModel(
                authManager = authManager,
                authState = authState,
                calendarPreferences = calendarPreferences,
                calendarRepository = calendarRepository,
                selectedEventsProvider = selectedEventsProvider,
                eventSerializer = eventSerializer,
                syncManager = syncManager,
                syncScheduler = syncScheduler,
            )

            advanceUntilIdle()

            coVerify(exactly = 1) { syncManager.performSync() }
        }

    @Test
    fun `refreshAccount triggers sync when requested after calendar changes`() =
        runTest(dispatcher) {
            coEvery { authManager.getAccounts() } returns listOf(testAccount)
            every { calendarPreferences.selectedCalendarIds(testAccount.id) } returns flowOf(setOf(testCalendar.id))
            coEvery { calendarRepository.getCalendars(testAccount.id) } returns listOf(testCalendar)
            coEvery { syncManager.performSync() } returns SyncResult.Success(1)

            val viewModel =
                MainViewModel(
                    authManager = authManager,
                    authState = authState,
                    calendarPreferences = calendarPreferences,
                    calendarRepository = calendarRepository,
                    selectedEventsProvider = selectedEventsProvider,
                    eventSerializer = eventSerializer,
                    syncManager = syncManager,
                    syncScheduler = syncScheduler,
                )

            advanceUntilIdle()

            viewModel.refreshAccount(testAccount.id, triggerSyncAfterRefresh = true)
            advanceUntilIdle()

            coVerify(exactly = 2) { syncManager.performSync() }
        }

    @Test
    fun `loadDebugEvents trims oversized payload to most recent prior event and next 16 events`() =
        runTest(dispatcher) {
            coEvery { authManager.getAccounts() } returns listOf(testAccount)
            every { calendarPreferences.selectedCalendarIds(testAccount.id) } returns flowOf(setOf(testCalendar.id))
            coEvery { calendarRepository.getCalendars(testAccount.id) } returns listOf(testCalendar)
            coEvery { syncManager.performSync() } returns SyncResult.Success(1)

            val now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0)
            val oversizedEvents = buildOversizedEvents(now)
            coEvery { selectedEventsProvider.loadSelectedEvents() } returns
                SelectedEventsLoadResult(events = oversizedEvents, hasAvailableSelectedCalendars = true)

            val viewModel =
                MainViewModel(
                    authManager = authManager,
                    authState = authState,
                    calendarPreferences = calendarPreferences,
                    calendarRepository = calendarRepository,
                    selectedEventsProvider = selectedEventsProvider,
                    eventSerializer = eventSerializer,
                    syncManager = syncManager,
                    syncScheduler = syncScheduler,
                )

            advanceUntilIdle()

            viewModel.refreshAccount(testAccount.id)
            advanceUntilIdle()

            viewModel.loadDebugEvents()
            advanceUntilIdle()

            val state = viewModel.debugEventsState.value
            assertTrue(state is DebugEventsUiState.Success)

            val fullJson = eventSerializer.serialize(oversizedEvents.sortedBy { it.startDateTime }.take(oversizedEvents.size))
            val json = (state as DebugEventsUiState.Success).json
            assertTrue(json.length <= fullJson.length, "Expected emitted debug JSON payload to be no larger than the full payload")
            assertFalse(json.contains("\"id\""), "Debug JSON should not include event IDs")
            assertFalse(json.contains("event-3"), "Older past events should be trimmed out")
            assertTrue(json.contains("event-4"), "The most recent prior event should be retained")
            assertTrue(json.contains("event-20"), "The next 16 events after the prior event should be retained")
            assertFalse(json.contains("event-21"), "Events beyond the retained forward window should be trimmed out")
        }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildOversizedEvents(now: OffsetDateTime): List<DisplayEvent> {
        val description = "x".repeat(480)

        val pastEvents =
            (0..4).map { index ->
                buildEvent(
                    title = "event-$index",
                    start = now.minusDays((5 - index).toLong()),
                    description = description,
                )
            }

        val futureEvents =
            (5..24).map { index ->
                buildEvent(
                    title = "event-$index",
                    start = now.plusHours((index - 4).toLong()),
                    description = description,
                )
            }

        return (pastEvents + futureEvents).sortedBy { it.startDateTime }
    }

    private fun buildEvent(
        title: String,
        start: OffsetDateTime,
        description: String,
    ): DisplayEvent {
        return DisplayEvent(
            id = "$title-id",
            title = title,
            startDateTime = start.toString(),
            startTimeZone = "UTC",
            endDateTime = start.plusMinutes(30).toString(),
            endTimeZone = "UTC",
            location = null,
            isAllDay = false,
            calendarName = testCalendar.name,
            calendarColor = testCalendar.hexColor,
            description = description,
        )
    }

}