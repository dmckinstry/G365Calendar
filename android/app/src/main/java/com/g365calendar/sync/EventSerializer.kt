package com.g365calendar.sync

import com.g365calendar.data.model.DisplayEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializes/deserializes DisplayEvent lists for transmission to the Garmin watch.
 * Uses Moshi JSON — the Garmin Connect Mobile SDK transfers data as key-value maps,
 * so we serialize the event list to a JSON string under a single key.
 */
@Singleton
class EventSerializer
    @Inject
    constructor(
        private val moshi: Moshi,
    ) {
        private companion object {
            const val MAX_DESCRIPTION_LENGTH = 1024
            const val MAX_JSON_LENGTH = 12 * 1024
            const val FOLLOWING_EVENT_COUNT = 16
        }

        private val clientEventListType = Types.newParameterizedType(List::class.java, ClientEvent::class.java)
        private val adapter = moshi.adapter<List<ClientEvent>>(clientEventListType).indent("  ")

        fun buildPayload(events: List<DisplayEvent>): ClientEventsPayload {
            val preparedEvents = prepareClientEvents(events)
            val json = adapter.toJson(preparedEvents)
            return ClientEventsPayload(
                json = json,
                events = preparedEvents,
                eventCount = preparedEvents.size,
            )
        }

        fun serialize(events: List<DisplayEvent>): String {
            return buildPayload(events).json
        }

        fun deserialize(json: String): List<ClientEvent> {
            return adapter.fromJson(json) ?: emptyList()
        }

        /**
         * Builds the data map to send via Garmin Connect Mobile SDK.
         * The watch app reads the "events" key to get the event payload.
         */
        fun toTransferMap(events: List<DisplayEvent>): Map<String, Any> {
            val payload = buildPayload(events)
            return mapOf(
                "events" to payload.events.map { event -> event.toTransferMap() },
                "syncTimestamp" to System.currentTimeMillis(),
                "eventCount" to payload.eventCount,
            )
        }

        private fun prepareClientEvents(events: List<DisplayEvent>): List<ClientEvent> {
            val normalizedEvents =
                events
                    .sortedBy(DisplayEvent::startDateTime)
                    .map { event ->
                        val description = event.description
                        val normalizedDescription =
                            if (description == null || description.length <= MAX_DESCRIPTION_LENGTH) {
                                description
                            } else {
                                description.take(MAX_DESCRIPTION_LENGTH)
                            }

                        ClientEvent(
                            title = event.title,
                            startDateTime = event.startDateTime,
                            startTimeZone = event.startTimeZone,
                            endDateTime = event.endDateTime,
                            endTimeZone = event.endTimeZone,
                            location = event.location,
                            isAllDay = event.isAllDay,
                            calendarName = event.calendarName,
                            calendarColor = event.calendarColor,
                            description = normalizedDescription,
                        )
                    }

            val fullJson = adapter.toJson(normalizedEvents)
            if (fullJson.length <= MAX_JSON_LENGTH) {
                return normalizedEvents
            }

            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val lastPastIndex =
                normalizedEvents.indexOfLast { event ->
                    event.startDateTime.toOffsetDateTimeOrNull()?.isBefore(now) == true
                }
            val startIndex = if (lastPastIndex >= 0) lastPastIndex else 0
            val endExclusive = minOf(normalizedEvents.size, startIndex + FOLLOWING_EVENT_COUNT + 1)
            return normalizedEvents.subList(startIndex, endExclusive)
        }

        private fun ClientEvent.toTransferMap(): Map<String, Any> {
            val values =
                linkedMapOf<String, Any>(
                    "title" to title,
                    "startDateTime" to startDateTime,
                    "startTimeZone" to startTimeZone,
                    "endDateTime" to endDateTime,
                    "endTimeZone" to endTimeZone,
                    "isAllDay" to isAllDay,
                    "calendarName" to calendarName,
                )

            location?.let { values["location"] = it }
            calendarColor?.let { values["calendarColor"] = it }
            description?.let { values["description"] = it }
            return values
        }

        private fun String.toOffsetDateTimeOrNull(): OffsetDateTime? {
            return try {
                OffsetDateTime.parse(this)
            } catch (_: Exception) {
                null
            }
        }
    }

data class ClientEventsPayload(
    val json: String,
    val events: List<ClientEvent>,
    val eventCount: Int,
)

data class ClientEvent(
    val title: String,
    val startDateTime: String,
    val startTimeZone: String,
    val endDateTime: String,
    val endTimeZone: String,
    val location: String?,
    val isAllDay: Boolean,
    val calendarName: String,
    val calendarColor: String?,
    val description: String? = null,
)
