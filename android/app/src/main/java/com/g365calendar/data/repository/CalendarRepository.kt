package com.g365calendar.data.repository

import com.g365calendar.auth.AuthManager
import com.g365calendar.data.api.GraphCalendarApi
import com.g365calendar.data.model.Calendar
import com.g365calendar.data.model.CalendarEvent
import com.g365calendar.data.model.DisplayEvent
import okhttp3.ResponseBody
import retrofit2.HttpException
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches calendar list and events from Microsoft Graph API.
 * Sync window: 24 hours past → 7 days future.
 */
@Singleton
class CalendarRepository
    @Inject
    constructor(
        private val graphApi: GraphCalendarApi,
        private val authManager: AuthManager,
    ) {
        companion object {
            private const val PAST_HOURS = 24L
            private const val FUTURE_DAYS = 7L
            private val ISO_FORMATTER: DateTimeFormatter =
                DateTimeFormatter.ISO_INSTANT
        }

        /** Fetches all calendars available to the user. */
        suspend fun getCalendars(accountId: String): List<Calendar> {
            return try {
                graphApi.getCalendars(requireAuthorizationHeader(accountId)).value
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch calendars")
                throw e.toCalendarAccessException()
            }
        }

        /**
         * Fetches events for the given calendars within the sync window
         * (24h past → 7 days future). Merges results into DisplayEvent list.
         */
        suspend fun getEvents(
            accountId: String,
            calendars: List<Calendar>,
        ): List<DisplayEvent> {
            val now = Instant.now()
            val start = now.minus(PAST_HOURS, ChronoUnit.HOURS)
            val end = now.plus(FUTURE_DAYS, ChronoUnit.DAYS)
            val startStr = ISO_FORMATTER.format(start)
            val endStr = ISO_FORMATTER.format(end)
            val authorizationHeader = requireAuthorizationHeader(accountId)

            return calendars.flatMap { calendar ->
                fetchEventsForCalendar(calendar, startStr, endStr, authorizationHeader)
            }.sortedBy { it.startDateTime }
        }

        private suspend fun fetchEventsForCalendar(
            calendar: Calendar,
            startDateTime: String,
            endDateTime: String,
            authorizationHeader: String,
        ): List<DisplayEvent> {
            return try {
                val response =
                    graphApi.getCalendarEvents(
                        authorization = authorizationHeader,
                        calendarId = calendar.id,
                        startDateTime = startDateTime,
                        endDateTime = endDateTime,
                    )
                response.value
                    .filter { !it.isCancelled }
                    .map { it.toDisplayEvent(calendar) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch events for calendar: ${calendar.name}")
                emptyList()
            }
        }

        private suspend fun requireAuthorizationHeader(accountId: String): String {
            val accessToken = authManager.getAccessToken(accountId)
                ?: throw IllegalStateException("Authentication expired for this account. Please sign in again.")
            return "Bearer $accessToken"
        }
    }

private fun Exception.toCalendarAccessException(): Exception {
    if (this !is HttpException) {
        return this
    }

    val errorBody = response()?.errorBody()?.string().orEmpty()
    if (errorBody.contains("MailboxNotEnabledForRESTAPI", ignoreCase = true)) {
        return IllegalStateException(
            "This account does not have an Exchange Online mailbox available to Microsoft Graph calendar APIs. Sign in with a Microsoft 365 work or school account that has an active mailbox.",
            this,
        )
    }

    return this
}

internal fun CalendarEvent.toDisplayEvent(calendar: Calendar): DisplayEvent {
    return DisplayEvent(
        id = id,
        title = subject ?: "(No title)",
        startDateTime = start?.dateTime ?: "",
        startTimeZone = start?.timeZone ?: ZoneOffset.UTC.id,
        endDateTime = end?.dateTime ?: "",
        endTimeZone = end?.timeZone ?: ZoneOffset.UTC.id,
        location = location?.displayName?.takeIf { it.isNotBlank() },
        isAllDay = isAllDay,
        calendarName = calendar.name,
        calendarColor = calendar.hexColor ?: calendar.color,
        description = bodyPreview?.takeIf { it.isNotBlank() },
    )
}
