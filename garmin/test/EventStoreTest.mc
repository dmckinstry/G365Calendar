import Toybox.Test;
import Toybox.Lang;

//! Unit tests for EventStore module.
(:test)
class EventStoreTest {

    (:test)
    function testGetEventCountReturnsZeroWhenEmpty(logger as Logger) as Boolean {
        EventStore.clearEvents();
        var count = EventStore.getEventCount();
        logger.debug("Event count: " + count);
        return count == 0;
    }

    (:test)
    function testGetLastSyncTimeReturnsNullWhenNoSync(logger as Logger) as Boolean {
        EventStore.clearEvents();
        var syncTime = EventStore.getLastSyncTime();
        return syncTime == null;
    }

    (:test)
    function testClearEventsRemovesAllData(logger as Logger) as Boolean {
        EventStore.parseAndStore("[]", 12345, 0);
        EventStore.clearEvents();

        var count = EventStore.getEventCount();
        var syncTime = EventStore.getLastSyncTime();
        return count == 0 && syncTime == null;
    }

    (:test)
    function testParseAndStoreSetsSyncTimestamp(logger as Logger) as Boolean {
        EventStore.clearEvents();
        EventStore.parseAndStore("[]", 99999, 0);

        var syncTime = EventStore.getLastSyncTime();
        return syncTime == 99999;
    }

    (:test)
    function testParseAndStoreSetsEventCount(logger as Logger) as Boolean {
        EventStore.clearEvents();
        EventStore.parseAndStore("[]", 12345, 5);

        var count = EventStore.getEventCount();
        return count == 5;
    }

    (:test)
    function testGetEventsReturnsEmptyForEmptyJson(logger as Logger) as Boolean {
        EventStore.clearEvents();
        EventStore.parseAndStore("[]", 12345, 0);

        var events = EventStore.getEvents();
        return events.size() == 0;
    }

    (:test)
    function testSeedDebugEventsIfNeededPopulatesInitialData(logger as Logger) as Boolean {
        EventStore.clearEvents();
        EventStore.seedDebugEventsIfNeeded();

        var events = EventStore.getEvents();
        return events.size() > 0
            && EventStore.getEventCount() == events.size()
            && EventStore.getLastSyncTime() != null;
    }

    (:test)
    function testParseAndStoreOverridesSeededDebugData(logger as Logger) as Boolean {
        EventStore.clearEvents();
        EventStore.seedDebugEventsIfNeeded();
        EventStore.parseAndStore(
            [
                {
                    "id" => "real-1",
                    "title" => "Production Event",
                    "startDateTime" => "2026-07-19T09:00:00",
                    "startTimeZone" => "Local",
                    "endDateTime" => "2026-07-19T09:30:00",
                    "endTimeZone" => "Local",
                    "location" => "Room A",
                    "isAllDay" => false,
                    "calendarName" => "Work",
                    "calendarColor" => "#1A73E8"
                }
            ],
            98765,
            1
        );

        var events = EventStore.getEvents();
        return events.size() == 1
            && (events[0].get("id") as String).equals("real-1")
            && (events[0].get("title") as String).equals("Production Event")
            && EventStore.getEventCount() == 1
            && EventStore.getLastSyncTime() == 98765;
    }
}
