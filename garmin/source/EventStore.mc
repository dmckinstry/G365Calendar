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
    function parseAndStore(eventsData, syncTimestamp, eventCount) as Void {
        Application.Storage.setValue(STORAGE_KEY_EVENTS, eventsData);

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
        var storedEvents = Application.Storage.getValue(STORAGE_KEY_EVENTS);
        if (storedEvents == null || !(storedEvents instanceof Array)) {
            return [] as Array<Dictionary>;
        }

        var result = [] as Array<Dictionary>;
        for (var i = 0; i < storedEvents.size() && i < MAX_EVENTS; i++) {
            if (storedEvents[i] instanceof Dictionary) {
                result.add(storedEvents[i] as Dictionary);
            }
        }

        return result;
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

}
