package com.g365calendar.sync

import com.g365calendar.data.model.Calendar
import com.g365calendar.data.model.DisplayEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SyncManagerTest {
    private lateinit var selectedEventsProvider: SelectedEventsProvider
    private lateinit var eventSerializer: EventSerializer
    private lateinit var garminConnector: GarminConnector
    private lateinit var syncManager: SyncManager

    private val testCalendar =
        Calendar(
            id = "cal-1",
            name = "Work",
            color = "blue",
            hexColor = "#0078D4",
        )

    private val testEvent =
        DisplayEvent(
            id = "evt-1",
            title = "Standup",
            startDateTime = "2026-02-15T09:00:00",
            startTimeZone = "UTC",
            endDateTime = "2026-02-15T09:30:00",
            endTimeZone = "UTC",
            location = "Room A",
            isAllDay = false,
            calendarName = "Work",
            calendarColor = "#0078D4",
        )

    private val testPayload =
        ClientEventsPayload(
            json = "[{\"title\":\"Standup\"}]",
            events =
                listOf(
                    ClientEvent(
                        title = "Standup",
                        startDateTime = "2026-02-15T09:00:00",
                        startTimeZone = "UTC",
                        endDateTime = "2026-02-15T09:30:00",
                        endTimeZone = "UTC",
                        location = "Room A",
                        isAllDay = false,
                        calendarName = "Work",
                        calendarColor = "#0078D4",
                        description = null,
                    ),
                ),
            eventCount = 1,
        )

    @BeforeEach
    fun setup() {
        selectedEventsProvider = mockk()
        eventSerializer = mockk()
        garminConnector = mockk()
        syncManager = SyncManager(selectedEventsProvider, eventSerializer, garminConnector)
    }

    @Test
    fun `performSync returns NoCalendarsSelected when none selected`() =
        runTest {
            coEvery { selectedEventsProvider.loadSelectedEvents() } returns
                SelectedEventsLoadResult(events = emptyList(), hasAvailableSelectedCalendars = false)

            val result = syncManager.performSync()

            assertTrue(result is SyncResult.NoCalendarsSelected)
        }

    @Test
    fun `performSync returns NoCalendarsSelected when selected calendars are unavailable`() =
        runTest {
            coEvery { selectedEventsProvider.loadSelectedEvents() } returns
                SelectedEventsLoadResult(events = emptyList(), hasAvailableSelectedCalendars = false)

            val result = syncManager.performSync()

            assertTrue(result is SyncResult.NoCalendarsSelected)
        }

    @Test
    fun `performSync returns Success when events sent`() =
        runTest {
            coEvery { selectedEventsProvider.loadSelectedEvents() } returns
                SelectedEventsLoadResult(events = listOf(testEvent), hasAvailableSelectedCalendars = true)
            every { eventSerializer.buildPayload(listOf(testEvent)) } returns testPayload
            coEvery { garminConnector.sendPayload(testPayload) } returns true

            val result = syncManager.performSync()

            assertTrue(result is SyncResult.Success)
            assertEquals(1, (result as SyncResult.Success).eventCount)
        }

    @Test
    fun `performSync returns WatchNotConnected when send fails`() =
        runTest {
            coEvery { selectedEventsProvider.loadSelectedEvents() } returns
                SelectedEventsLoadResult(events = listOf(testEvent), hasAvailableSelectedCalendars = true)
            every { eventSerializer.buildPayload(listOf(testEvent)) } returns testPayload
            coEvery { garminConnector.sendPayload(testPayload) } returns false

            val result = syncManager.performSync()

            assertTrue(result is SyncResult.WatchNotConnected)
        }

    @Test
    fun `performSync returns Error when calendar fetch fails`() =
        runTest {
            coEvery { selectedEventsProvider.loadSelectedEvents() } throws RuntimeException("Network error")

            val result = syncManager.performSync()

            assertTrue(result is SyncResult.Error)
            assertEquals("Network error", (result as SyncResult.Error).message)
        }

    @Test
    fun `performSync only sends events for selected calendars`() =
        runTest {
            coEvery { selectedEventsProvider.loadSelectedEvents() } returns
                SelectedEventsLoadResult(events = listOf(testEvent), hasAvailableSelectedCalendars = true)
            every { eventSerializer.buildPayload(listOf(testEvent)) } returns testPayload
            coEvery { garminConnector.sendPayload(testPayload) } returns true

            syncManager.performSync()

            coVerify { selectedEventsProvider.loadSelectedEvents() }
            coVerify { garminConnector.sendPayload(testPayload) }
        }
}
