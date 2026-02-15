import Toybox.Application;
import Toybox.Lang;

//! Local storage for calendar events on the watch.
//! Uses Application.Storage for persistence across app sessions.
//! Storage is limited (~128KB), so we keep event data compact.
module EventStore {

    const STORAGE_KEY_EVENTS = "calEvents";
    const STORAGE_KEY_SYNC_TIME = "lastSync";
    const STORAGE_KEY_COUNT = "eventCount";
    const MAX_EVENTS = 50;

    //! Stores event data received from the companion app.
    function parseAndStore(eventsJson as String, syncTimestamp, eventCount) as Void {
        // Store raw JSON — parsed on demand for display
        Application.Storage.setValue(STORAGE_KEY_EVENTS, eventsJson);

        if (syncTimestamp != null) {
            Application.Storage.setValue(STORAGE_KEY_SYNC_TIME, syncTimestamp);
        }
        if (eventCount != null) {
            Application.Storage.setValue(STORAGE_KEY_COUNT, eventCount);
        }
    }

    //! Retrieves stored events as an array of dictionaries.
    //! Each event dict has: id, title, startDateTime, startTimeZone,
    //! endDateTime, endTimeZone, location, isAllDay, calendarName, calendarColor
    function getEvents() as Array<Dictionary> {
        var json = Application.Storage.getValue(STORAGE_KEY_EVENTS);
        if (json == null || !(json instanceof String)) {
            return [] as Array<Dictionary>;
        }

        // Parse JSON array of event objects
        var events = parseJsonEvents(json as String);
        return events;
    }

    //! Returns the timestamp of the last successful sync, or null.
    function getLastSyncTime() as Number or Null {
        return Application.Storage.getValue(STORAGE_KEY_SYNC_TIME) as Number or Null;
    }

    //! Returns the count of stored events.
    function getEventCount() as Number {
        var count = Application.Storage.getValue(STORAGE_KEY_COUNT);
        if (count != null && count instanceof Number) {
            return count as Number;
        }
        return 0;
    }

    //! Clears all stored events.
    function clearEvents() as Void {
        Application.Storage.deleteValue(STORAGE_KEY_EVENTS);
        Application.Storage.deleteValue(STORAGE_KEY_SYNC_TIME);
        Application.Storage.deleteValue(STORAGE_KEY_COUNT);
    }

    //! Simple JSON array parser for event objects.
    //! Garmin Monkey C lacks a built-in JSON parser, so we use a
    //! lightweight approach suitable for the known data format.
    function parseJsonEvents(json as String) as Array<Dictionary> {
        // Use Communications.parseMimeType for JSON parsing if available,
        // otherwise the data arrives pre-parsed as Dictionary from the SDK
        var result = [] as Array<Dictionary>;

        try {
            // The Connect Mobile SDK typically delivers data as native types,
            // so this is a fallback for string-encoded JSON
            var parsed = Communications.parseMimeType("application/json", {:data => json});
            if (parsed != null && parsed instanceof Array) {
                for (var i = 0; i < parsed.size() && i < MAX_EVENTS; i++) {
                    if (parsed[i] instanceof Dictionary) {
                        result.add(parsed[i] as Dictionary);
                    }
                }
            }
        } catch (e) {
            // Parse error — return empty
        }

        return result;
    }
}
