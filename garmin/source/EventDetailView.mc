import Toybox.Graphics;
import Toybox.WatchUi;
import Toybox.Lang;

//! Detail view for a single calendar event.
//! Shows full title, time range, location, and calendar name with color.
class EventDetailView extends WatchUi.View {

    private var _event as Dictionary;

    function initialize(event as Dictionary) {
        View.initialize();
        _event = event;
    }

    function onLayout(dc as Dc) as Void {
    }

    function onUpdate(dc as Dc) as Void {
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();

        var width = dc.getWidth();
        var y = 30;
        var margin = 20;

        // Calendar color bar at top
        var colorStr = _event.get("calendarColor");
        if (colorStr != null && colorStr instanceof String) {
            dc.setColor(parseColor(colorStr as String), Graphics.COLOR_TRANSPARENT);
            dc.fillRectangle(0, 0, width, 6);
        }

        // Title
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        var title = _event.get("title");
        if (title == null) { title = "(No title)"; }
        dc.drawText(width / 2, y, Graphics.FONT_SMALL, title as String, Graphics.TEXT_JUSTIFY_CENTER);
        y += 35;

        // Calendar name
        var calName = _event.get("calendarName");
        if (calName != null && calName instanceof String) {
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(width / 2, y, Graphics.FONT_XTINY, calName as String, Graphics.TEXT_JUSTIFY_CENTER);
            y += 25;
        }

        // Time
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        if (_event.get("isAllDay") == true) {
            dc.drawText(width / 2, y, Graphics.FONT_TINY, "All Day", Graphics.TEXT_JUSTIFY_CENTER);
        } else {
            var startTime = _event.get("startDateTime");
            var endTime = _event.get("endDateTime");
            var timeStr = "";
            if (startTime != null && startTime instanceof String) {
                timeStr = formatTime(startTime as String);
            }
            if (endTime != null && endTime instanceof String) {
                timeStr += " - " + formatTime(endTime as String);
            }
            dc.drawText(width / 2, y, Graphics.FONT_TINY, timeStr, Graphics.TEXT_JUSTIFY_CENTER);
        }
        y += 30;

        // Date
        var startDt = _event.get("startDateTime");
        if (startDt != null && startDt instanceof String) {
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(width / 2, y, Graphics.FONT_XTINY, formatDate(startDt as String), Graphics.TEXT_JUSTIFY_CENTER);
            y += 25;
        }

        // Location
        var location = _event.get("location");
        if (location != null && location instanceof String) {
            dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
            dc.drawText(width / 2, y, Graphics.FONT_XTINY, location as String, Graphics.TEXT_JUSTIFY_CENTER);
        }
    }

    private function formatTime(isoDateTime as String) as String {
        var tIndex = isoDateTime.find("T");
        if (tIndex != null) {
            return isoDateTime.substring(tIndex + 1, tIndex + 6);
        }
        return isoDateTime;
    }

    private function formatDate(isoDateTime as String) as String {
        var tIndex = isoDateTime.find("T");
        if (tIndex != null) {
            return isoDateTime.substring(0, tIndex);
        }
        return isoDateTime;
    }

    private function parseColor(hex as String) as Number {
        if (hex.length() < 6) {
            return Graphics.COLOR_BLUE;
        }
        try {
            var str = hex;
            if (str.substring(0, 1).equals("#")) {
                str = str.substring(1, str.length());
            }
            var r = str.substring(0, 2).toNumberWithBase(16);
            var g = str.substring(2, 4).toNumberWithBase(16);
            var b = str.substring(4, 6).toNumberWithBase(16);
            if (r != null && g != null && b != null) {
                return (r << 16) | (g << 8) | b;
            }
        } catch (e) {
            // Fall through
        }
        return Graphics.COLOR_BLUE;
    }

    function onHide() as Void {
    }
}

//! Simple back-navigation delegate for the detail view.
class EventDetailDelegate extends WatchUi.BehaviorDelegate {

    function initialize() {
        BehaviorDelegate.initialize();
    }

    function onBack() as Boolean {
        WatchUi.popView(WatchUi.SLIDE_RIGHT);
        return true;
    }
}
