import Toybox.Lang;
import Toybox.System;
import Toybox.Time;
import Toybox.Time.Gregorian;
import Toybox.WatchUi;
import Toybox.Application;

//! Loads local debug events from bundled resources in debug builds.
module DebugData {

    //! Initializes test events from resources/debug-events.json when debug mode is enabled.
    function initializeFromResourceIfDebug() as Void {
        if (!isDebugMode()) {
            return;
        }

        var parsed = WatchUi.loadResource(Rez.JsonData.DebugEvents);
        if (!(parsed instanceof Array)) {
            return;
        }

        var nowSec = Time.now().value();
        var normalizedEvents = [] as Array<Dictionary>;
        for (var i = 0; i < (parsed as Array).size(); i++) {
            var event = (parsed as Array)[i];
            if (!(event instanceof Dictionary)) {
                continue;
            }

            var normalized = copyDictionary(event as Dictionary);

            var start = normalized.get("startDateTime");
            if (start instanceof String) {
                normalized.put("startDateTime", resolveRelativeTime(start as String, nowSec));
            }

            var finish = normalized.get("endDateTime");
            if (finish instanceof String) {
                normalized.put("endDateTime", resolveRelativeTime(finish as String, nowSec));
            }

            normalizedEvents.add(normalized);
        }

        EventStore.parseAndStore(normalizedEvents, Time.now().value() * 1000, normalizedEvents.size());
    }

    function isDebugMode() as Boolean {
        return true; // TODO... Always return true for debug mode in this context.
    }

    function copyDictionary(input as Dictionary) as Dictionary {
        var out = {} as Dictionary;
        var keys = input.keys();
        for (var i = 0; i < keys.size(); i++) {
            var key = keys[i];
            out.put(key, input.get(key));
        }
        return out;
    }

    //! Converts +H:MM/-H:MM offsets to absolute local datetime strings.
    function resolveRelativeTime(value as String, nowSec as Number) as String {
        var offsetMinutes = parseOffsetMinutes(value);
        if (offsetMinutes == null) {
            return value;
        }

        var absoluteSec = nowSec + ((offsetMinutes as Number) * 60);
        var info = Gregorian.info(new Time.Moment(absoluteSec), Time.FORMAT_SHORT);
        return info.year + "-" + pad2(info.month) + "-" + pad2(info.day) + "T" + pad2(info.hour) + ":" + pad2(info.min) + ":00";
    }

    //! Parses offset strings like -1:00 or +0:30 into minutes.
    function parseOffsetMinutes(value as String) as Number or Null {
        if (value.length() < 4) {
            return null;
        }

        var sign = 1;
        var startIndex = 0;
        var first = value.substring(0, 1);
        if (first.equals("-")) {
            sign = -1;
            startIndex = 1;
        } else if (first.equals("+")) {
            startIndex = 1;
        }

        var colon = value.find(":");
        if (colon == null || colon <= startIndex || colon >= value.length() - 1) {
            return null;
        }

        var hours = value.substring(startIndex, colon).toNumber();
        var minutes = value.substring(colon + 1, value.length()).toNumber();
        if (hours == null || minutes == null) {
            return null;
        }

        return sign * (((hours as Number) * 60) + (minutes as Number));
    }

    function pad2(value as Number) as String {
        if (value < 10) {
            return "0" + value;
        }
        return value.toString();
    }
}
