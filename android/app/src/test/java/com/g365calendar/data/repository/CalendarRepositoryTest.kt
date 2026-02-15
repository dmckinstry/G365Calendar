package com.g365calendar.data.repository

import com.g365calendar.data.api.GraphCalendarApi
import com.g365calendar.data.model.Calendar
import com.g365calendar.data.model.CalendarEvent
import com.g365calendar.data.model.CalendarListResponse
import com.g365calendar.data.model.DateTimeTimeZone
import com.g365calendar.data.model.EventListResponse
import com.g365calendar.data.model.Location
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CalendarRepositoryTest {

    private lateinit var graphApi: GraphCalendarApi
    private lateinit var repository: CalendarRepository

    private val testCalendar = Calendar(
        id = "cal-1",
        name = "Work Calendar",
        color = "blue",
        hexColor = "#0078D4",
        isDefaultCalendar = true,
    )

    private val testEvent = CalendarEvent(
        id = "evt-1",
        subject = "Team Standup",
        start = DateTimeTimeZone("2026-02-15T09:00:00.0000000", "UTC"),
        end = DateTimeTimeZone("2026-02-15T09:30:00.0000000", "UTC"),
        location = Location("Conference Room A"),
        isAllDay = false,
        isCancelled = false,
        showAs = "busy",
    )

    @BeforeEach
    fun setup() {
        graphApi = mockk()
        repository = CalendarRepository(graphApi)
    }

    @Test
    fun `getCalendars returns calendar list from API`() = runTest {
        coEvery { graphApi.getCalendars() } returns CalendarListResponse(
            value = listOf(testCalendar),
        )

        val result = repository.getCalendars()

        assertEquals(1, result.size)
        assertEquals("Work Calendar", result[0].name)
        assertEquals("#0078D4", result[0].hexColor)
    }

    @Test
    fun `getCalendars throws on API failure`() = runTest {
        coEvery { graphApi.getCalendars() } throws RuntimeException("Network error")

        try {
            repository.getCalendars()
            assertTrue(false, "Should have thrown")
        } catch (e: RuntimeException) {
            assertEquals("Network error", e.message)
        }
    }

    @Test
    fun `getEvents returns display events sorted by start time`() = runTest {
        val laterEvent = testEvent.copy(
            id = "evt-2",
            subject = "Lunch",
            start = DateTimeTimeZone("2026-02-15T12:00:00.0000000", "UTC"),
            end = DateTimeTimeZone("2026-02-15T13:00:00.0000000", "UTC"),
        )
        coEvery {
            graphApi.getCalendarEvents(
                calendarId = "cal-1",
                startDateTime = any(),
                endDateTime = any(),
                select = any(),
                orderBy = any(),
                top = any(),
            )
        } returns EventListResponse(value = listOf(laterEvent, testEvent))

        val result = repository.getEvents(listOf(testCalendar))

        assertEquals(2, result.size)
        assertEquals("Team Standup", result[0].title)
        assertEquals("Lunch", result[1].title)
    }

    @Test
    fun `getEvents filters cancelled events`() = runTest {
        val cancelledEvent = testEvent.copy(id = "evt-cancelled", isCancelled = true)
        coEvery {
            graphApi.getCalendarEvents(any(), any(), any(), any(), any(), any())
        } returns EventListResponse(value = listOf(testEvent, cancelledEvent))

        val result = repository.getEvents(listOf(testCalendar))

        assertEquals(1, result.size)
        assertEquals("evt-1", result[0].id)
    }

    @Test
    fun `getEvents returns empty on API error for individual calendar`() = runTest {
        coEvery {
            graphApi.getCalendarEvents(any(), any(), any(), any(), any(), any())
        } throws RuntimeException("Timeout")

        val result = repository.getEvents(listOf(testCalendar))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `toDisplayEvent maps fields correctly`() {
        val displayEvent = testEvent.toDisplayEvent(testCalendar)

        assertEquals("evt-1", displayEvent.id)
        assertEquals("Team Standup", displayEvent.title)
        assertEquals("2026-02-15T09:00:00.0000000", displayEvent.startDateTime)
        assertEquals("UTC", displayEvent.startTimeZone)
        assertEquals("Conference Room A", displayEvent.location)
        assertEquals(false, displayEvent.isAllDay)
        assertEquals("Work Calendar", displayEvent.calendarName)
        assertEquals("#0078D4", displayEvent.calendarColor)
    }

    @Test
    fun `toDisplayEvent uses No title for null subject`() {
        val noSubject = testEvent.copy(subject = null)
        val displayEvent = noSubject.toDisplayEvent(testCalendar)

        assertEquals("(No title)", displayEvent.title)
    }

    @Test
    fun `toDisplayEvent returns null location for blank display name`() {
        val blankLocation = testEvent.copy(location = Location(""))
        val displayEvent = blankLocation.toDisplayEvent(testCalendar)

        assertNull(displayEvent.location)
    }
}
