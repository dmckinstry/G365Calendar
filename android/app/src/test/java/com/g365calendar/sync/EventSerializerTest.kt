package com.g365calendar.sync

import com.g365calendar.data.model.DisplayEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EventSerializerTest {

    private lateinit var serializer: EventSerializer

    private val testEvent = DisplayEvent(
        id = "evt-1",
        title = "Team Standup",
        startDateTime = "2026-02-15T09:00:00.0000000",
        startTimeZone = "UTC",
        endDateTime = "2026-02-15T09:30:00.0000000",
        endTimeZone = "UTC",
        location = "Conference Room A",
        isAllDay = false,
        calendarName = "Work",
        calendarColor = "#0078D4",
    )

    @BeforeEach
    fun setup() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        serializer = EventSerializer(moshi)
    }

    @Test
    fun `serialize and deserialize round-trips correctly`() {
        val events = listOf(testEvent)
        val json = serializer.serialize(events)
        val result = serializer.deserialize(json)

        assertEquals(1, result.size)
        assertEquals(testEvent.id, result[0].id)
        assertEquals(testEvent.title, result[0].title)
        assertEquals(testEvent.location, result[0].location)
        assertEquals(testEvent.calendarName, result[0].calendarName)
        assertEquals(testEvent.calendarColor, result[0].calendarColor)
    }

    @Test
    fun `serialize empty list returns empty JSON array`() {
        val json = serializer.serialize(emptyList())
        assertEquals("[]", json)
    }

    @Test
    fun `deserialize empty array returns empty list`() {
        val result = serializer.deserialize("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toTransferMap contains expected keys`() {
        val events = listOf(testEvent)
        val map = serializer.toTransferMap(events)

        assertTrue(map.containsKey("events"))
        assertTrue(map.containsKey("syncTimestamp"))
        assertTrue(map.containsKey("eventCount"))
        assertEquals(1, map["eventCount"])
    }

    @Test
    fun `serialize handles null location`() {
        val noLocation = testEvent.copy(location = null)
        val json = serializer.serialize(listOf(noLocation))
        val result = serializer.deserialize(json)

        assertEquals(1, result.size)
        assertEquals(null, result[0].location)
    }

    @Test
    fun `serialize handles multiple events`() {
        val events = listOf(
            testEvent,
            testEvent.copy(id = "evt-2", title = "Lunch"),
            testEvent.copy(id = "evt-3", title = "Review"),
        )
        val json = serializer.serialize(events)
        val result = serializer.deserialize(json)

        assertEquals(3, result.size)
        assertEquals("Team Standup", result[0].title)
        assertEquals("Lunch", result[1].title)
        assertEquals("Review", result[2].title)
    }
}
