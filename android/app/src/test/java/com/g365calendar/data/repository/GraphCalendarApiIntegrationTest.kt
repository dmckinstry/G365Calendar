package com.g365calendar.data.repository

import com.g365calendar.BuildConfig
import com.g365calendar.data.api.GraphCalendarApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class GraphCalendarApiIntegrationTest {
    @Test
    @Tag("integration")
    fun `getCalendars retrieves calendars from live microsoft graph`() =
        runBlocking {
            val repository = createRepository()

            val calendars = repository.getCalendars()

            assertFalse(calendars.isEmpty(), "Expected at least one calendar from Microsoft Graph")
            assertTrue(calendars.all { it.id.isNotBlank() }, "Expected calendar IDs to be populated")
            assertTrue(calendars.all { it.name.isNotBlank() }, "Expected calendar names to be populated")

            println("Retrieved ${calendars.size} calendars from Microsoft Graph:")
            calendars.forEach { calendar ->
                println(" - ${calendar.name}") 
            }
        }

    @Test
    @Tag("integration")
    fun `getEvents retrieves events from calendar named Calendar`() =
        runBlocking {
            val repository = createRepository()

            val calendar =
                repository.getCalendars().firstOrNull { it.name == TARGET_CALENDAR_NAME }
                    ?: fail("Expected to find a calendar named $TARGET_CALENDAR_NAME in Microsoft Graph")

            val events = repository.getEvents(listOf(calendar))

            assertTrue(events.all { it.calendarName == TARGET_CALENDAR_NAME }, "Expected all events to come from $TARGET_CALENDAR_NAME")

            println("Retrieved ${events.size} events from calendar $TARGET_CALENDAR_NAME")
            events.forEach { event ->
                println(" - ${event.title} @ ${event.startDateTime}")
            }
        }

    private fun createRepository(): CalendarRepository {
        return CalendarRepository(createGraphCalendarApi(requireAccessToken()))
    }

    private fun createGraphCalendarApi(accessToken: String): GraphCalendarApi {
        val client =
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request =
                        chain.request()
                            .newBuilder()
                            .header("Authorization", "Bearer $accessToken")
                            .build()
                    chain.proceed(request)
                }
                .build()

        val moshi =
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

        val retrofit =
            Retrofit.Builder()
                .baseUrl(BuildConfig.GRAPH_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

        return retrofit.create(GraphCalendarApi::class.java)
    }

    private fun requireAccessToken(): String {
        return System.getenv(GRAPH_ACCESS_TOKEN_ENV)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: fail(
                "Set $GRAPH_ACCESS_TOKEN_ENV to a Microsoft Graph bearer token with Calendars.Read before running integrationTest.",
            )
    }

    companion object {
        private const val GRAPH_ACCESS_TOKEN_ENV = "GRAPH_ACCESS_TOKEN"
        private const val TARGET_CALENDAR_NAME = "Calendar"
    }
}