package com.g365calendar.di

import android.content.Context
import com.g365calendar.R
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideMsalApp(
        @ApplicationContext context: Context,
    ): ISingleAccountPublicClientApplication {
        return PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.msal_config,
        )
    }
}
