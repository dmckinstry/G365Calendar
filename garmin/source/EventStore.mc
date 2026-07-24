import Toybox.Application;
import Toybox.Lang;
import Toybox.Time;
import Toybox.Time.Gregorian;

//! Local storage for calendar events on the watch.
//! Uses Application.Storage for persistence across app sessions.
//! Storage is limited (~128KB), so we keep event data compact.
module EventStore {

    const STORAGE_KEY_EVENTS = "calEvents";
    const STORAGE_KEY_SYNC_TIME = "lastSync";
    const STORAGE_KEY_COUNT = "eventCount";
    const MAX_EVENTS = 50;
    const MAX_DESCRIPTION_LENGTH = 1024;

    //! Seeds local debug events once so the app has data before the first phone sync.
    function seedDebugEventsIfNeeded() as Void {
        if (!shouldSeedDebugEvents()) {
            return;
        }

        var debugEvents = buildDebugEvents();
        Application.Storage.setValue(STORAGE_KEY_EVENTS, debugEvents);
        Application.Storage.setValue(STORAGE_KEY_SYNC_TIME, Time.now().value() * 1000);
        Application.Storage.setValue(STORAGE_KEY_COUNT, debugEvents.size());
    }

    //! Stores event data received from the companion app.
    function parseAndStore(eventsData, syncTimestamp, eventCount) as Void {
        Application.Storage.setValue(STORAGE_KEY_EVENTS, normalizeEvents(eventsData));

        if (syncTimestamp != null) {
            Application.Storage.setValue(STORAGE_KEY_SYNC_TIME, syncTimestamp);
        }
        if (eventCount != null) {
            Application.Storage.setValue(STORAGE_KEY_COUNT, eventCount);
        }
    }

    //! Retrieves stored events as an array of dictionaries.
    //! Each event dict has: id, title, startDateTime, startTimeZone,
    //! endDateTime, endTimeZone, location, isAllDay, calendarName, calendarColor, description
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

    function normalizeEvents(eventsData) as Array<Dictionary> {
        if (!(eventsData instanceof Array)) {
            return [] as Array<Dictionary>;
        }

        var result = [] as Array<Dictionary>;
        for (var i = 0; i < (eventsData as Array).size() && i < MAX_EVENTS; i++) {
            var event = (eventsData as Array)[i];
            if (event instanceof Dictionary) {
                result.add(normalizeEvent(event as Dictionary));
            }
        }

        return result;
    }

    function normalizeEvent(event as Dictionary) as Dictionary {
        var normalized = {} as Dictionary;
        var keys = event.keys();
        for (var i = 0; i < keys.size(); i++) {
            var key = keys[i];
            normalized.put(key, event.get(key));
        }

        var description = normalized.get("description");
        if (description != null && description instanceof String) {
            normalized.put("description", truncateDescription(description as String));
        }

        return normalized;
    }

    function truncateDescription(description as String) as String {
        if (description.length() <= MAX_DESCRIPTION_LENGTH) {
            return description;
        }
        return description.substring(0, MAX_DESCRIPTION_LENGTH);
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

    function shouldSeedDebugEvents() as Boolean {
        var storedEvents = Application.Storage.getValue(STORAGE_KEY_EVENTS);
        if (storedEvents instanceof Array && (storedEvents as Array).size() > 0) {
            return false;
        }

        return Application.Storage.getValue(STORAGE_KEY_SYNC_TIME) == null;
    }

    function buildDebugEvents() as Array<Dictionary> {
        return [
            buildDebugEvent(
                "debug-1",
                "Retrospective Follow-up",
                -60,
                -30,
                "Conference Room A",
                "Review action items from the sprint retro and confirm owners for the follow-up tasks.",
                "Work",
                "#1A73E8"
            ),
            buildDebugEvent(
                "debug-2",
                "Current Sprint Planning",
                -15,
                45,
                "Teams",
                "Finalize scope for the next sprint, review carry-over work, and capture any dependencies that need follow-up before kickoff.",
                "Engineering",
                "#34A853"
            ),
            buildDebugEvent(
                "debug-3",
                "Evening Run",
                120,
                180,
                "City Park",
                null,
                "Personal",
                "#FBBC05"
            ),
            buildDebugEvent(
                "debug-4",
                "Design Review",
                210,
                240,
                "Room 204",
                null,
                "Product",
                "#8E24AA"
            ),
            buildDebugEvent(
                "debug-5",
                "1:1 with Manager",
                270,
                300,
                "Teams",
                null,
                "Work",
                "#1A73E8"
            ),
            buildDebugEvent(
                "debug-6",
                "Lunch with Alex",
                315,
                360,
                "Cafeteria",
                null,
                "Personal",
                "#FBBC05"
            ),
            buildDebugEvent(
                "debug-7",
                "Quarterly Planning",
                390,
                450,
                "Conference Room B",
                null,
                "Engineering",
                "#34A853"
            ),
            buildDebugEvent(
                "debug-8",
                "Yoga Class",
                480,
                540,
                "Studio",
                null,
                "Personal",
                "#FBBC05"
            )
        ];
    }

    function buildDebugEvent(
        id as String,
        title as String,
        startOffsetMinutes as Number,
        endOffsetMinutes as Number,
        location as String,
        description as String or Null,
        calendarName as String,
        calendarColor as String
    ) as Dictionary {
        var event = {
            "id" => id,
            "title" => title,
            "startDateTime" => formatRelativeDateTime(startOffsetMinutes),
            "startTimeZone" => "Local",
            "endDateTime" => formatRelativeDateTime(endOffsetMinutes),
            "endTimeZone" => "Local",
            "location" => location,
            "isAllDay" => false,
            "calendarName" => calendarName,
            "calendarColor" => calendarColor
        };

        if (description != null) {
            event.put("description", description);
        }

        return event;
    }

    function formatRelativeDateTime(offsetMinutes as Number) as String {
        var timestamp = Time.now().value() + (offsetMinutes * 60);
        var info = Gregorian.info(new Time.Moment(timestamp), Time.FORMAT_SHORT);
        return info.year.toString() + "-" + pad2(info.month) + "-" + pad2(info.day)
            + "T" + pad2(info.hour) + ":" + pad2(info.min) + ":00";
    }

    function pad2(value as Number) as String {
        if (value < 10) {
            return "0" + value;
        }

        return value.toString();
    }

}
