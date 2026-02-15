package com.g365calendar.sync

import com.g365calendar.data.model.DisplayEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializes/deserializes DisplayEvent lists for transmission to the Garmin watch.
 * Uses Moshi JSON — the Garmin Connect Mobile SDK transfers data as key-value maps,
 * so we serialize the event list to a JSON string under a single key.
 */
@Singleton
class EventSerializer @Inject constructor(
    private val moshi: Moshi,
) {
    private val listType = Types.newParameterizedType(List::class.java, DisplayEvent::class.java)
    private val adapter = moshi.adapter<List<DisplayEvent>>(listType)

    fun serialize(events: List<DisplayEvent>): String {
        return adapter.toJson(events)
    }

    fun deserialize(json: String): List<DisplayEvent> {
        return adapter.fromJson(json) ?: emptyList()
    }

    /**
     * Builds the data map to send via Garmin Connect Mobile SDK.
     * The watch app reads the "events" key to get the JSON payload.
     */
    fun toTransferMap(events: List<DisplayEvent>): Map<String, Any> {
        return mapOf(
            "events" to serialize(events),
            "syncTimestamp" to System.currentTimeMillis(),
            "eventCount" to events.size,
        )
    }
}
