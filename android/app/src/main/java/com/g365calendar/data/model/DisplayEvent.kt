package com.g365calendar.data.model

/**
 * Locally enriched event combining Graph API event data with calendar metadata.
 * Used for display on both the Android companion app and Garmin watch.
 */
data class DisplayEvent(
    val id: String,
    val title: String,
    val startDateTime: String,
    val startTimeZone: String,
    val endDateTime: String,
    val endTimeZone: String,
    val location: String?,
    val isAllDay: Boolean,
    val calendarName: String,
    val calendarColor: String?,
)
