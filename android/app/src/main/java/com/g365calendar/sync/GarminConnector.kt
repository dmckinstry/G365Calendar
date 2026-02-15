package com.g365calendar.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages communication with the Garmin watch via the Connect Mobile SDK.
 *
 * The Garmin Connect Mobile SDK (connectiq-mobile-sdk) provides IQDevice discovery
 * and IQApp messaging. This class wraps the SDK to send serialized calendar events
 * to the watch app.
 *
 * NOTE: The actual Garmin Connect Mobile SDK dependency must be added manually
 * as it is distributed via the Garmin developer portal, not Maven Central.
 * See: https://developer.garmin.com/connect-iq/sdk/
 */
@Singleton
class GarminConnector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventSerializer: EventSerializer,
) {
    companion object {
        const val WATCH_APP_ID = "g365-calendar-app"
    }

    private var isInitialized = false

    /**
     * Initializes the Garmin Connect Mobile SDK.
     * Must be called when the Activity is available.
     */
    fun initialize() {
        if (isInitialized) return
        try {
            // TODO: Initialize ConnectIQ SDK when dependency is added
            // connectIQ = ConnectIQ.getInstance(context, ConnectIQ.IQConnectType.WIRELESS)
            // connectIQ.initialize(context, true, connectIQListener)
            isInitialized = true
            Timber.d("GarminConnector initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Garmin Connect SDK")
        }
    }

    /**
     * Sends calendar events to the connected Garmin watch.
     * Returns true if the data was successfully queued for delivery.
     */
    suspend fun sendEvents(events: List<com.g365calendar.data.model.DisplayEvent>): Boolean {
        if (!isInitialized) {
            Timber.w("GarminConnector not initialized, cannot send events")
            return false
        }

        return try {
            val transferMap = eventSerializer.toTransferMap(events)
            Timber.d("Sending ${events.size} events to watch")

            // TODO: Send via ConnectIQ SDK when dependency is added
            // val devices = connectIQ.knownDevices
            // for (device in devices) {
            //     connectIQ.sendMessage(device, watchApp, transferMap, sendMessageCallback)
            // }

            Timber.d("Events queued for delivery to watch")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to send events to watch")
            false
        }
    }

    /** Returns whether the connector is initialized and ready. */
    fun isReady(): Boolean = isInitialized

    fun shutdown() {
        if (!isInitialized) return
        try {
            // TODO: connectIQ.shutdown(context)
            isInitialized = false
            Timber.d("GarminConnector shut down")
        } catch (e: Exception) {
            Timber.e(e, "Failed to shut down Garmin Connect SDK")
        }
    }
}
