package com.g365calendar;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "G365Calendar";
    private static final String APP_ID = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6";
    
    private ConnectIQ connectIQ;
    private IQDevice device;
    private IQApp app;
    private ISingleAccountPublicClientApplication msalApp;
    
    private TextView statusText;
    private Button authButton;
    private Button syncButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        statusText = findViewById(R.id.statusText);
        authButton = findViewById(R.id.authButton);
        syncButton = findViewById(R.id.syncButton);
        
        initializeConnectIQ();
        initializeMSAL();
        
        authButton.setOnClickListener(v -> authenticate());
        syncButton.setOnClickListener(v -> syncCalendar());
    }
    
    private void initializeConnectIQ() {
        connectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS);
        
        connectIQ.initialize(this, true, new ConnectIQ.ConnectIQListener() {
            @Override
            public void onSdkReady() {
                Log.d(TAG, "Connect IQ SDK Ready");
                updateStatus("Connect IQ Ready");
                findDevices();
            }
            
            @Override
            public void onInitializeError(ConnectIQ.IQSdkErrorStatus status) {
                Log.e(TAG, "Connect IQ Init Error: " + status);
                updateStatus("Connect IQ Error: " + status);
            }
            
            @Override
            public void onSdkShutDown() {
                Log.d(TAG, "Connect IQ SDK Shutdown");
            }
        });
    }
    
    private void findDevices() {
        try {
            List<IQDevice> devices = connectIQ.getKnownDevices();
            if (devices != null && !devices.isEmpty()) {
                device = devices.get(0);
                app = new IQApp(APP_ID);
                updateStatus("Device found: " + device.getFriendlyName());
            } else {
                updateStatus("No devices found");
            }
        } catch (InvalidStateException | ServiceUnavailableException e) {
            Log.e(TAG, "Error finding devices", e);
            updateStatus("Error finding devices");
        }
    }
    
    private void initializeMSAL() {
        try {
            PublicClientApplication.createSingleAccountPublicClientApplication(
                getApplicationContext(),
                R.raw.auth_config,
                new PublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        msalApp = application;
                        Log.d(TAG, "MSAL initialized");
                    }
                    
                    @Override
                    public void onError(MsalException exception) {
                        Log.e(TAG, "MSAL initialization error", exception);
                        updateStatus("MSAL Error: " + exception.getMessage());
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "MSAL setup error", e);
        }
    }
    
    private void authenticate() {
        if (msalApp == null) {
            updateStatus("MSAL not initialized");
            return;
        }
        
        String[] scopes = {"Calendars.Read"};
        
        msalApp.signIn(this, "", Arrays.asList(scopes), new IAuthenticationResult() {
            // Note: Proper implementation would use AuthenticationCallback
            // This is a simplified placeholder
        });
    }
    
    private void syncCalendar() {
        if (device == null || app == null) {
            updateStatus("No device connected");
            return;
        }
        
        if (msalApp == null) {
            updateStatus("Not authenticated");
            return;
        }
        
        // Fetch calendar events and send to watch
        fetchAndSendCalendarEvents();
    }
    
    private void fetchAndSendCalendarEvents() {
        // This would integrate with Microsoft Graph API to fetch events
        // and then send them to the watch via Connect IQ messaging
        
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("calendar_events", new Object[0]); // Placeholder
            
            connectIQ.sendMessage(device, app, message, new ConnectIQ.IQSendMessageListener() {
                @Override
                public void onMessageStatus(IQDevice device, IQApp app, ConnectIQ.IQMessageStatus status) {
                    Log.d(TAG, "Message status: " + status);
                    updateStatus("Sync status: " + status);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error sending message", e);
            updateStatus("Sync error: " + e.getMessage());
        }
    }
    
    private void updateStatus(String message) {
        runOnUiThread(() -> {
            if (statusText != null) {
                statusText.setText(message);
            }
            Log.d(TAG, message);
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectIQ != null) {
            try {
                connectIQ.shutdown(this);
            } catch (InvalidStateException e) {
                Log.e(TAG, "Error shutting down Connect IQ", e);
            }
        }
    }
}
