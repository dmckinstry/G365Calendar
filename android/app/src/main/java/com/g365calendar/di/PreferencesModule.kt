package com.g365calendar.di

import android.content.Context
import com.g365calendar.data.preferences.CalendarPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideCalendarPreferences(
        @ApplicationContext context: Context,
    ): CalendarPreferences {
        return CalendarPreferences(context)
    }
}
