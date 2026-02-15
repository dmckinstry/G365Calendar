package com.g365calendar.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CalendarListResponse(
    val value: List<Calendar>,
)

@JsonClass(generateAdapter = true)
data class Calendar(
    val id: String,
    val name: String,
    val color: String?,
    @Json(name = "hexColor") val hexColor: String?,
    @Json(name = "isDefaultCalendar") val isDefaultCalendar: Boolean = false,
    @Json(name = "canEdit") val canEdit: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class EventListResponse(
    val value: List<CalendarEvent>,
    @Json(name = "@odata.nextLink") val nextLink: String? = null,
)

@JsonClass(generateAdapter = true)
data class CalendarEvent(
    val id: String,
    val subject: String?,
    @Json(name = "bodyPreview") val bodyPreview: String? = null,
    val start: DateTimeTimeZone?,
    val end: DateTimeTimeZone?,
    val location: Location? = null,
    @Json(name = "isAllDay") val isAllDay: Boolean = false,
    @Json(name = "isCancelled") val isCancelled: Boolean = false,
    @Json(name = "showAs") val showAs: String? = null,
)

@JsonClass(generateAdapter = true)
data class DateTimeTimeZone(
    val dateTime: String,
    val timeZone: String,
)

@JsonClass(generateAdapter = true)
data class Location(
    val displayName: String?,
)
