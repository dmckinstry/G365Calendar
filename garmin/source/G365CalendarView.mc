import Toybox.Graphics;
import Toybox.WatchUi;
import Toybox.Lang;

//! Main view displaying a scrollable list of calendar events.
//! Shows event title, time, and location with calendar color indicator.
class G365CalendarView extends WatchUi.View {

    private var _events as Array<Dictionary> = [] as Array<Dictionary>;
    private var _scrollOffset as Number = 0;
    private const ITEM_HEIGHT = 65;
    private const HEADER_HEIGHT = 40;

    function initialize() {
        View.initialize();
    }

    function onLayout(dc as Dc) as Void {
    }

    function onShow() as Void {
        _events = EventStore.getEvents();
    }

    function onUpdate(dc as Dc) as Void {
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();

        var width = dc.getWidth();
        var height = dc.getHeight();

        // Header
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            width / 2, 10,
            Graphics.FONT_SMALL,
            "G365 Calendar",
            Graphics.TEXT_JUSTIFY_CENTER
        );

        if (_events.size() == 0) {
            dc.drawText(
                width / 2, height / 2,
                Graphics.FONT_MEDIUM,
                "No events",
                Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
            );
            return;
        }

        // Draw event list
        var y = HEADER_HEIGHT - _scrollOffset;
        for (var i = 0; i < _events.size(); i++) {
            if (y + ITEM_HEIGHT > 0 && y < height) {
                drawEventItem(dc, _events[i], 10, y, width - 20);
            }
            y += ITEM_HEIGHT;
        }
    }

    //! Draws a single event item row.
    private function drawEventItem(dc as Dc, event as Dictionary, x as Number, y as Number, w as Number) as Void {
        // Calendar color indicator
        var colorStr = event.get("calendarColor");
        if (colorStr != null && colorStr instanceof String) {
            dc.setColor(parseColor(colorStr as String), Graphics.COLOR_TRANSPARENT);
            dc.fillRectangle(x, y + 4, 4, ITEM_HEIGHT - 8);
        }

        var textX = x + 12;

        // Event title
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        var title = event.get("title");
        if (title == null) { title = "(No title)"; }
        dc.drawText(textX, y + 2, Graphics.FONT_TINY, title as String, Graphics.TEXT_JUSTIFY_LEFT);

        // Time
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        var startTime = event.get("startDateTime");
        var timeStr = "";
        if (event.get("isAllDay") == true) {
            timeStr = "All day";
        } else if (startTime != null && startTime instanceof String) {
            timeStr = formatTime(startTime as String);
        }
        dc.drawText(textX, y + 22, Graphics.FONT_XTINY, timeStr, Graphics.TEXT_JUSTIFY_LEFT);

        // Location
        var location = event.get("location");
        if (location != null && location instanceof String) {
            dc.setColor(Graphics.COLOR_DK_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(textX, y + 40, Graphics.FONT_XTINY, location as String, Graphics.TEXT_JUSTIFY_LEFT);
        }

        // Separator line
        dc.setColor(Graphics.COLOR_DK_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawLine(x, y + ITEM_HEIGHT - 1, x + w, y + ITEM_HEIGHT - 1);
    }

    //! Scrolls the event list by the given delta.
    function scroll(delta as Number) as Void {
        var maxScroll = (_events.size() * ITEM_HEIGHT) - 200;
        if (maxScroll < 0) { maxScroll = 0; }
        _scrollOffset += delta;
        if (_scrollOffset < 0) { _scrollOffset = 0; }
        if (_scrollOffset > maxScroll) { _scrollOffset = maxScroll; }
        WatchUi.requestUpdate();
    }

    //! Returns the event at the given index for detail view.
    function getEventAt(index as Number) as Dictionary or Null {
        if (index >= 0 && index < _events.size()) {
            return _events[index];
        }
        return null;
    }

    //! Calculates which event index is at the given Y position.
    function getEventIndexAtY(y as Number) as Number {
        return ((y - HEADER_HEIGHT + _scrollOffset) / ITEM_HEIGHT).toNumber();
    }

    //! Extracts a displayable time string (HH:MM) from ISO datetime.
    private function formatTime(isoDateTime as String) as String {
        // Format: "2026-02-15T09:00:00.0000000"
        var tIndex = isoDateTime.find("T");
        if (tIndex != null) {
            var timePart = isoDateTime.substring(tIndex + 1, tIndex + 6);
            return timePart;
        }
        return isoDateTime;
    }

    //! Parses a hex color string to a Garmin color value.
    private function parseColor(hex as String) as Number {
        // Default to blue for calendar color
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
