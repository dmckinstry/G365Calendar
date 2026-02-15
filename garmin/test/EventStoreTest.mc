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
}
