using Toybox.Communications;
using Toybox.Application.Storage;
using Toybox.Lang;
using Toybox.System;
using Toybox.Time;
using Toybox.Time.Gregorian;

class ApiClient {

    private const STORAGE_KEY_CACHED_EVENTS = "cached_events";
    private const STORAGE_KEY_CACHE_TIMESTAMP = "cache_timestamp";
    
    private const GRAPH_API_ENDPOINT = "https://graph.microsoft.com/v1.0";
    private const CACHE_DURATION = 300; // 5 minutes in seconds
    
    // Configurable event limits
    private const DAYS_PAST = 1;
    private const DAYS_FUTURE = 7;
    
    private var _authManager as AuthManager;
    private var _callback as Method?;

    function initialize(authManager as AuthManager) {
        _authManager = authManager;
        _callback = null;
    }

    function fetchEvents(callback as Method?) as Void {
        _callback = callback;
        
        if (!_authManager.isAuthenticated()) {
            System.println("Not authenticated - cannot fetch events");
            if (_callback != null) {
                _callback.invoke(null);
            }
            return;
        }
        
        // Check cache validity
        if (isCacheValid()) {
            System.println("Using cached events");
            if (_callback != null) {
                _callback.invoke(getCachedEvents());
            }
            return;
        }
        
        // Build calendar query with time window
        var now = Time.now();
        var startTime = now.add(new Time.Duration(-DAYS_PAST * 24 * 60 * 60));
        var endTime = now.add(new Time.Duration(DAYS_FUTURE * 24 * 60 * 60));
        
        var startDateTime = Gregorian.info(startTime, Time.FORMAT_SHORT);
        var endDateTime = Gregorian.info(endTime, Time.FORMAT_SHORT);
        
        var startStr = formatDateTime(startDateTime);
        var endStr = formatDateTime(endDateTime);
        
        var url = GRAPH_API_ENDPOINT + "/me/calendar/events" +
                  "?$select=subject,start,end,location" +
                  "&$orderby=start/dateTime" +
                  "&$filter=start/dateTime ge '" + startStr + "' and end/dateTime le '" + endStr + "'" +
                  "&$top=50";
        
        var accessToken = _authManager.getAccessToken();
        
        if (accessToken == null) {
            System.println("No access token available");
            if (_callback != null) {
                _callback.invoke(null);
            }
            return;
        }
        
        var options = {
            :method => Communications.HTTP_REQUEST_METHOD_GET,
            :headers => {
                "Authorization" => "Bearer " + accessToken,
                "Content-Type" => Communications.REQUEST_CONTENT_TYPE_JSON
            },
            :responseType => Communications.HTTP_RESPONSE_CONTENT_TYPE_JSON
        };
        
        Communications.makeWebRequest(
            url,
            {},
            options,
            method(:onEventsReceived)
        );
    }

    function onEventsReceived(responseCode as Number, data as Dictionary?) as Void {
        if (responseCode == 200 && data != null && data.hasKey("value")) {
            var events = data["value"] as Array;
            var processedEvents = processEvents(events);
            
            // Cache the events
            cacheEvents(processedEvents);
            
            System.println("Received " + processedEvents.size() + " events");
            
            if (_callback != null) {
                _callback.invoke(processedEvents);
            }
        } else if (responseCode == 401) {
            // Unauthorized - token might be expired
            System.println("Unauthorized - refreshing token");
            _authManager.refreshToken();
            if (_callback != null) {
                _callback.invoke(null);
            }
        } else {
            System.println("Error fetching events: " + responseCode);
            if (_callback != null) {
                _callback.invoke(null);
            }
        }
    }

    function processEvents(rawEvents as Array) as Array {
        var processed = [] as Array;
        
        for (var i = 0; i < rawEvents.size(); i++) {
            var event = rawEvents[i] as Dictionary;
            var processedEvent = {
                "subject" => event.hasKey("subject") ? event["subject"] : "Untitled",
                "start" => extractDateTime(event, "start"),
                "end" => extractDateTime(event, "end"),
                "location" => extractLocation(event)
            };
            processed.add(processedEvent);
        }
        
        return processed;
    }

    function extractDateTime(event as Dictionary, key as String) as String {
        if (event.hasKey(key)) {
            var timeObj = event[key] as Dictionary;
            if (timeObj.hasKey("dateTime")) {
                return timeObj["dateTime"] as String;
            }
        }
        return "";
    }

    function extractLocation(event as Dictionary) as String {
        if (event.hasKey("location")) {
            var location = event["location"] as Dictionary;
            if (location.hasKey("displayName")) {
                return location["displayName"] as String;
            }
        }
        return "";
    }

    function formatDateTime(dateInfo as Gregorian.Info) as String {
        // Format as ISO 8601: YYYY-MM-DDTHH:MM:SSZ
        return dateInfo.year.format("%04d") + "-" +
               dateInfo.month.format("%02d") + "-" +
               dateInfo.day.format("%02d") + "T" +
               dateInfo.hour.format("%02d") + ":" +
               dateInfo.min.format("%02d") + ":" +
               dateInfo.sec.format("%02d") + "Z";
    }

    function cacheEvents(events as Array) as Void {
        Storage.setValue(STORAGE_KEY_CACHED_EVENTS, events);
        Storage.setValue(STORAGE_KEY_CACHE_TIMESTAMP, Time.now().value());
    }

    function getCachedEvents() as Array? {
        return Storage.getValue(STORAGE_KEY_CACHED_EVENTS) as Array?;
    }

    function isCacheValid() as Boolean {
        var cacheTimestamp = Storage.getValue(STORAGE_KEY_CACHE_TIMESTAMP);
        
        if (cacheTimestamp == null) {
            return false;
        }
        
        var now = Time.now().value();
        var age = now - (cacheTimestamp as Number);
        
        return age < CACHE_DURATION;
    }

    function clearCache() as Void {
        Storage.deleteValue(STORAGE_KEY_CACHED_EVENTS);
        Storage.deleteValue(STORAGE_KEY_CACHE_TIMESTAMP);
    }
}
