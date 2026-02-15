package com.g365calendar.data.api

import com.g365calendar.data.model.CalendarListResponse
import com.g365calendar.data.model.EventListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit interface for Microsoft Graph Calendar API endpoints. */
interface GraphCalendarApi {

    @GET("me/calendars")
    suspend fun getCalendars(): CalendarListResponse

    @GET("me/calendars/{calendarId}/calendarView")
    suspend fun getCalendarEvents(
        @Path("calendarId") calendarId: String,
        @Query("startDateTime") startDateTime: String,
        @Query("endDateTime") endDateTime: String,
        @Query("\$select") select: String = "id,subject,bodyPreview,start,end,location,isAllDay,isCancelled,showAs",
        @Query("\$orderby") orderBy: String = "start/dateTime asc",
        @Query("\$top") top: Int = 100,
    ): EventListResponse
}
