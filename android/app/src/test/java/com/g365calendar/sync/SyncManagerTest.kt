package com.g365calendar.sync

import com.g365calendar.auth.AuthAccount
import com.g365calendar.auth.AuthManager
import com.g365calendar.data.model.Calendar
import com.g365calendar.data.model.DisplayEvent
import com.g365calendar.data.preferences.CalendarPreferences
import com.g365calendar.data.repository.CalendarRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SyncManagerTest {
    private lateinit var authManager: AuthManager
    private lateinit var calendarRepository: CalendarRepository
    private lateinit var calendarPreferences: CalendarPreferences
    private lateinit var garminConnector: GarminConnector
    private lateinit var syncManager: SyncManager

    private val testAccount = AuthAccount(id = "account-1", displayName = "Work User", username = "user@example.com")

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

    @BeforeEach
    fun setup() {
        authManager = mockk()
        calendarRepository = mockk()
        calendarPreferences = mockk()
        garminConnector = mockk()
        syncManager = SyncManager(authManager, calendarRepository, calendarPreferences, garminConnector)
    }

    @Test
    fun `performSync returns NoCalendarsSelected when none selected`() =
        runTest {
            coEvery { authManager.getAccounts() } returns listOf(testAccount)
            every { calendarPreferences.selectedCalendarIds(testAccount.id) } returns flowOf(emptySet())

            val result = syncManager.performSync()

            assertTrue(result is SyncResult.NoCalendarsSelected)
        }

    @Test
    fun `performSync returns NoCalendarsSelected when selected calendars are unavailable`() =
        runTest {
            coEvery { authManager.getAccounts() } returns listOf(testAccount)
            every { calendarPreferences.selectedCalendarIds(testAccount.id) } returns flowOf(setOf("missing-calendar"))
            coEvery { calendarRepository.getCalendars(testAccount.id) } returns listOf(testCalendar)

            val result = syncManager.performSync()

            assertTrue(result is SyncResult.NoCalendarsSelected)
        }

    @Test
    fun `performSync returns Success when events sent`() =
        runTest {
            coEvery { authManager.getAccounts() } returns listOf(testAccount)
            every { calendarPreferences.selectedCalendarIds(testAccount.id) } returns flowOf(setOf("cal-1"))
            coEvery { calendarRepository.getCalendars(testAccount.id) } returns listOf(testCalendar)
            coEvery { calendarRepository.getEvents(testAccount.id, any()) } returns listOf(testEvent)
            coEvery { garminConnector.sendEvents(any()) } returns true

            val result = syncManager.performSync()

            assertTrue(result is SyncResult.Success)
            assertEquals(1, (result as SyncResult.Success).eventCount)
        }

    @Test
    fun `performSync returns WatchNotConnected when send fails`() =
        runTest {
            coEvery { authManager.getAccounts() } returns listOf(testAccount)
            every { calendarPreferences.selectedCalendarIds(testAccount.id) } returns flowOf(setOf("cal-1"))
            coEvery { calendarRepository.getCalendars(testAccount.id) } returns listOf(testCalendar)
            coEvery { calendarRepository.getEvents(testAccount.id, any()) } returns listOf(testEvent)
            coEvery { garminConnector.sendEvents(any()) } returns false

            val result = syncManager.performSync()

            assertTrue(result is SyncResult.WatchNotConnected)
        }

    @Test
    fun `performSync returns Error when calendar fetch fails`() =
        runTest {
            coEvery { authManager.getAccounts() } returns listOf(testAccount)
            every { calendarPreferences.selectedCalendarIds(testAccount.id) } returns flowOf(setOf("cal-1"))
            coEvery { calendarRepository.getCalendars(testAccount.id) } throws RuntimeException("Network error")

            val result = syncManager.performSync()

            assertTrue(result is SyncResult.Error)
            assertEquals("Network error", (result as SyncResult.Error).message)
        }

    @Test
    fun `performSync only sends events for selected calendars`() =
        runTest {
            val otherCalendar = testCalendar.copy(id = "cal-2", name = "Personal")
            coEvery { authManager.getAccounts() } returns listOf(testAccount)
            every { calendarPreferences.selectedCalendarIds(testAccount.id) } returns flowOf(setOf("cal-1"))
            coEvery { calendarRepository.getCalendars(testAccount.id) } returns listOf(testCalendar, otherCalendar)
            coEvery {
                calendarRepository.getEvents(
                    testAccount.id,
                    match { calendars ->
                        calendars.size == 1 && calendars[0].id == "cal-1"
                    },
                )
            } returns listOf(testEvent)
            coEvery { garminConnector.sendEvents(any()) } returns true

            syncManager.performSync()

            coVerify {
                calendarRepository.getEvents(
                    testAccount.id,
                    match { calendars -> calendars.size == 1 && calendars[0].id == "cal-1" },
                )
            }
        }
}
