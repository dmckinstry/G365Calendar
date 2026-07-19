package com.g365calendar.sync

import android.content.Context
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages communication with the Garmin watch via the Connect Mobile SDK.
 *
 * The Garmin Connect Mobile SDK (ciq-companion-app-sdk) provides IQDevice discovery
 * and IQApp messaging. This class wraps the SDK to send serialized calendar events
 * to the watch app.
 */
@Singleton
class GarminConnector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val eventSerializer: EventSerializer,
    ) {
        companion object {
            // Matches the application id declared in garmin/manifest.xml.
            const val WATCH_APP_ID = "7664af60b29d4f949abb291b11194419"
        }

        private val lock = Any()
        private var connectIQ: ConnectIQ? = null
        private var isInitialized = false
        private var isInitializing = false
        private var sdkReadyDeferred = CompletableDeferred<Boolean>()
        private val registeredDeviceIds = mutableSetOf<Long>()

        /**
         * Initializes the Garmin Connect Mobile SDK.
         * Passing an activity context with autoUi enabled lets Garmin show the
         * install/upgrade prompt for Garmin Connect Mobile when needed.
         */
        fun initialize(
            sdkContext: Context = context,
            autoUi: Boolean = false,
        ) {
            synchronized(lock) {
                if (isInitialized || isInitializing) {
                    return
                }

                isInitializing = true
                sdkReadyDeferred = CompletableDeferred()
            }

            try {
                val sdk = ConnectIQ.getInstance(sdkContext, ConnectIQ.IQConnectType.WIRELESS)
                connectIQ = sdk
                sdk.initialize(sdkContext, autoUi, connectIQListener)
                Timber.d("GarminConnector initialization started")
            } catch (e: Exception) {
                synchronized(lock) {
                    isInitializing = false
                    if (!sdkReadyDeferred.isCompleted) {
                        sdkReadyDeferred.complete(false)
                    }
                }
                Timber.e(e, "Failed to initialize Garmin Connect SDK")
            }
        }

        /**
         * Sends calendar events to the connected Garmin watch.
         * Returns true if the data was successfully queued for delivery.
         */
        suspend fun sendEvents(events: List<com.g365calendar.data.model.DisplayEvent>): Boolean {
            if (!ensureReady()) {
                Timber.w("GarminConnector not ready, cannot send events")
                return false
            }

            return try {
                val transferMap = eventSerializer.toTransferMap(events)
                val sdk = connectIQ ?: return false
                val devices = sdk.knownDevices.orEmpty()
                if (devices.isEmpty()) {
                    Timber.w("No Garmin devices are paired in Garmin Connect Mobile")
                    return false
                }

                val connectedDevices =
                    devices.filter { device ->
                        sdk.getDeviceStatus(device) == IQDevice.IQDeviceStatus.CONNECTED
                    }

                if (connectedDevices.isEmpty()) {
                    Timber.w("No connected Garmin devices available for sync")
                    return false
                }

                var delivered = false
                for (device in connectedDevices) {
                    registerForEvents(device)
                    val watchApp = getInstalledWatchApp(device) ?: continue
                    if (sendMessage(device, watchApp, transferMap)) {
                        delivered = true
                    }
                }

                if (delivered) {
                    Timber.d("Queued ${events.size} events for Garmin delivery")
                } else {
                    Timber.w("Unable to deliver events to any connected Garmin device")
                }
                delivered
            } catch (e: InvalidStateException) {
                Timber.e(e, "Garmin Connect SDK is not ready to send events")
                false
            } catch (e: ServiceUnavailableException) {
                Timber.e(e, "Garmin Connect Mobile service is unavailable")
                false
            } catch (e: Exception) {
                Timber.e(e, "Failed to send events to watch")
                false
            }
        }

        /** Returns whether the connector is initialized and ready. */
        fun isReady(): Boolean = isInitialized

        fun shutdown() {
            val sdk = connectIQ ?: return
            try {
                sdk.unregisterAllForEvents()
                sdk.shutdown(context)
                synchronized(lock) {
                    isInitialized = false
                    isInitializing = false
                    registeredDeviceIds.clear()
                    connectIQ = null
                    sdkReadyDeferred = CompletableDeferred()
                }
                Timber.d("GarminConnector shut down")
            } catch (e: InvalidStateException) {
                Timber.w(e, "Garmin Connect SDK was already shut down")
            } catch (e: Exception) {
                Timber.e(e, "Failed to shut down Garmin Connect SDK")
            }
        }

        private val connectIQListener =
            object : ConnectIQ.ConnectIQListener {
                override fun onSdkReady() {
                    synchronized(lock) {
                        isInitialized = true
                        isInitializing = false
                        if (!sdkReadyDeferred.isCompleted) {
                            sdkReadyDeferred.complete(true)
                        }
                    }
                    Timber.d("Garmin Connect SDK ready")
                }

                override fun onInitializeError(errStatus: ConnectIQ.IQSdkErrorStatus) {
                    synchronized(lock) {
                        isInitialized = false
                        isInitializing = false
                        if (!sdkReadyDeferred.isCompleted) {
                            sdkReadyDeferred.complete(false)
                        }
                    }
                    Timber.w("Garmin Connect SDK initialization failed: ${errStatus.name}")
                }

                override fun onSdkShutDown() {
                    synchronized(lock) {
                        isInitialized = false
                        isInitializing = false
                        registeredDeviceIds.clear()
                        connectIQ = null
                        sdkReadyDeferred = CompletableDeferred()
                    }
                    Timber.d("Garmin Connect SDK shut down callback received")
                }
            }

        private suspend fun ensureReady(): Boolean {
            if (isInitialized) {
                return true
            }

            initialize()
            return sdkReadyDeferred.await()
        }

        private fun registerForEvents(device: IQDevice) {
            val sdk = connectIQ ?: return
            val deviceId = device.deviceIdentifier
            if (!registeredDeviceIds.add(deviceId)) {
                return
            }

            try {
                sdk.unregisterForDeviceEvents(device)
                sdk.registerForDeviceEvents(device) { changedDevice, status ->
                    Timber.d(
                        "Garmin device status changed: ${changedDevice.friendlyName} -> ${status.name}",
                    )
                    if (status != IQDevice.IQDeviceStatus.CONNECTED) {
                        registeredDeviceIds.remove(changedDevice.deviceIdentifier)
                    }
                }

                sdk.unregisterForApplicationEvents(device, IQApp(WATCH_APP_ID))
                sdk.registerForAppEvents(device, IQApp(WATCH_APP_ID)) { changedDevice, _, message, status ->
                    Timber.d(
                        "Received Garmin app event from ${changedDevice.friendlyName}: ${status.name}, payloadSize=${message.size}",
                    )
                }
            } catch (e: InvalidStateException) {
                registeredDeviceIds.remove(deviceId)
                Timber.w(e, "Failed to register Garmin device/app listeners")
            }
        }

        private suspend fun getInstalledWatchApp(device: IQDevice): IQApp? =
            suspendCancellableCoroutine { continuation ->
                val sdk = connectIQ
                if (sdk == null) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                try {
                    sdk.getApplicationInfo(
                        WATCH_APP_ID,
                        device,
                        object : ConnectIQ.IQApplicationInfoListener {
                            override fun onApplicationInfoReceived(app: IQApp) {
                                val installedApp =
                                    app.takeIf { candidate ->
                                        candidate.status == IQApp.IQAppStatus.INSTALLED
                                    }
                                if (installedApp == null) {
                                    Timber.w(
                                        "Garmin watch app is not installed on ${device.friendlyName}",
                                    )
                                }
                                if (continuation.isActive) {
                                    continuation.resume(installedApp)
                                }
                            }

                            override fun onApplicationNotInstalled(applicationId: String) {
                                Timber.w(
                                    "Garmin watch app $applicationId is not installed on ${device.friendlyName}",
                                )
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        },
                    )
                } catch (e: InvalidStateException) {
                    Timber.w(e, "Garmin Connect SDK is not ready for app info lookup")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                } catch (e: ServiceUnavailableException) {
                    Timber.w(e, "Garmin Connect Mobile service unavailable during app lookup")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }

        private suspend fun sendMessage(
            device: IQDevice,
            watchApp: IQApp,
            payload: Map<String, Any>,
        ): Boolean =
            suspendCancellableCoroutine { continuation ->
                val sdk = connectIQ
                if (sdk == null) {
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                }

                try {
                    sdk.sendMessage(device, watchApp, payload) { _, _, status ->
                        if (status != ConnectIQ.IQMessageStatus.SUCCESS) {
                            Timber.w(
                                "Garmin sendMessage failed for ${device.friendlyName}: ${status.name}",
                            )
                        }
                        if (continuation.isActive) {
                            continuation.resume(status == ConnectIQ.IQMessageStatus.SUCCESS)
                        }
                    }
                } catch (e: InvalidStateException) {
                    Timber.w(e, "Garmin Connect SDK is not ready to send a message")
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                } catch (e: ServiceUnavailableException) {
                    Timber.w(e, "Garmin Connect Mobile service unavailable during send")
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }
    }
