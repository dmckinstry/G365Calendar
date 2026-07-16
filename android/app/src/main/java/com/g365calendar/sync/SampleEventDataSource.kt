package com.g365calendar.sync

import android.content.Context
import com.g365calendar.BuildConfig
import com.g365calendar.R
import com.g365calendar.data.model.DisplayEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SampleEventDataSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        moshi: Moshi,
    ) {
        private companion object {
            val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        }

        private val listType = Types.newParameterizedType(List::class.java, DisplayEvent::class.java)
        private val adapter = moshi.adapter<List<DisplayEvent>>(listType)

        fun loadEvents(): List<DisplayEvent> {
            if (!BuildConfig.DEBUG) {
                return emptyList()
            }

            return try {
                val payload =
                    context.resources.openRawResource(R.raw.debug_events).bufferedReader().use {
                        it.readText()
                    }
                val now = LocalDateTime.now()
                adapter.fromJson(payload).orEmpty().map { event ->
                    event.copy(
                        startDateTime = resolveRelativeTime(event.startDateTime, now),
                        endDateTime = resolveRelativeTime(event.endDateTime, now),
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load sample events")
                emptyList()
            }
        }

        private fun resolveRelativeTime(
            value: String,
            now: LocalDateTime,
        ): String {
            val offsetMinutes = parseOffsetMinutes(value) ?: return value
            return now.plusMinutes(offsetMinutes.toLong()).format(DATE_TIME_FORMATTER)
        }

        private fun parseOffsetMinutes(value: String): Int? {
            if (value.length < 4) {
                return null
            }

            val sign =
                when (value.first()) {
                    '-' -> -1
                    '+' -> 1
                    else -> 1
                }
            val startIndex = if (value.first() == '-' || value.first() == '+') 1 else 0
            val colonIndex = value.indexOf(':')
            if (colonIndex <= startIndex || colonIndex >= value.lastIndex) {
                return null
            }

            val hours = value.substring(startIndex, colonIndex).toIntOrNull() ?: return null
            val minutes = value.substring(colonIndex + 1).toIntOrNull() ?: return null
            return sign * ((hours * 60) + minutes)
        }
    }
