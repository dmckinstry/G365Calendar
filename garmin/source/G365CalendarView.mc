import Toybox.Graphics;
import Toybox.Time;
import Toybox.Time.Gregorian;
import Toybox.WatchUi;
import Toybox.Lang;

//! Main view displaying a scrollable list of calendar events.
//! Shows event title, time, and location with calendar color indicator.
class G365CalendarView extends WatchUi.View {

    private var _events as Array<Dictionary> = [] as Array<Dictionary>;
    private var _scrollOffset as Number = 0;
    private var _viewHeight as Number = 390;

    // Vertical padding/spacing between text lines, in pixels.
    private const LINE_SPACING = 4;
    private const ROW_TOP_PADDING = 4;
    private const ROW_BOTTOM_PADDING = 6;
    private const HEADER_TOP_PADDING = 6;
    private const HEADER_BOTTOM_PADDING = 6;
    private const FOOTER_BOTTOM_PADDING = 18;

    // Font heights are measured (not guessed) so rows never overlap
    // regardless of device font metrics/resolution.
    private var _titleFontHeight as Number = 0;
    private var _timeFontHeight as Number = 0;
    private var _locationFontHeight as Number = 0;
    private var _headerFontHeight as Number = 0;

    private var ITEM_HEIGHT as Number = 65;
    private var HEADER_HEIGHT as Number = 40;
    private const STATUS_HEIGHT = 24;
    private const FOUR_HOURS_MS = 4 * 60 * 60 * 1000;
    private const DAY_MS = 24 * 60 * 60 * 1000;

    function initialize() {
        View.initialize();

        _titleFontHeight = Graphics.getFontHeight(Graphics.FONT_TINY);
        _timeFontHeight = Graphics.getFontHeight(Graphics.FONT_XTINY);
        _locationFontHeight = Graphics.getFontHeight(Graphics.FONT_XTINY);
        _headerFontHeight = Graphics.getFontHeight(Graphics.FONT_SMALL);

        HEADER_HEIGHT = HEADER_TOP_PADDING + _headerFontHeight + HEADER_BOTTOM_PADDING;
        ITEM_HEIGHT = ROW_TOP_PADDING
            + _titleFontHeight + LINE_SPACING
            + _timeFontHeight + LINE_SPACING
            + _locationFontHeight + ROW_BOTTOM_PADDING;
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
        _viewHeight = height;

        // Header
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            width / 2, HEADER_TOP_PADDING,
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
            drawSyncStatus(dc, 10, height - 26, width - 20);
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

        if (y + STATUS_HEIGHT + FOOTER_BOTTOM_PADDING > 0 && y < height) {
            drawSyncStatus(dc, 10, y, width - 20);
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
        var titleY = y + ROW_TOP_PADDING;
        dc.drawText(textX, titleY, Graphics.FONT_TINY, title as String, Graphics.TEXT_JUSTIFY_LEFT);

        // Time
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        var startTime = event.get("startDateTime");
        var timeStr = "";
        if (event.get("isAllDay") == true) {
            timeStr = "All day";
        } else if (startTime != null && startTime instanceof String) {
            timeStr = formatTime(startTime as String);
        }
        var timeY = titleY + _titleFontHeight + LINE_SPACING;
        dc.drawText(textX, timeY, Graphics.FONT_XTINY, timeStr, Graphics.TEXT_JUSTIFY_LEFT);

        // Location
        var location = event.get("location");
        if (location != null && location instanceof String) {
            dc.setColor(Graphics.COLOR_DK_GRAY, Graphics.COLOR_TRANSPARENT);
            var locationY = timeY + _timeFontHeight + LINE_SPACING;
            dc.drawText(textX, locationY, Graphics.FONT_XTINY, location as String, Graphics.TEXT_JUSTIFY_LEFT);
        }

        // Separator line
        dc.setColor(Graphics.COLOR_DK_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawLine(x, y + ITEM_HEIGHT - 1, x + w, y + ITEM_HEIGHT - 1);
    }

    //! Returns the pixel height of a single event row, for scroll increments.
    function getRowHeight() as Number {
        return ITEM_HEIGHT;
    }

    //! Returns the last known view height, for centering calculations.
    function getViewHeight() as Number {
        return _viewHeight;
    }

    //! Scrolls the event list by the given delta.
    function scroll(delta as Number) as Void {
        var contentHeight = HEADER_HEIGHT + (_events.size() * ITEM_HEIGHT) + STATUS_HEIGHT + FOOTER_BOTTOM_PADDING;
        var minScroll = getMinScroll();
        var maxScroll = getMaxScroll(contentHeight);
        _scrollOffset += delta;
        if (_scrollOffset < minScroll) { _scrollOffset = minScroll; }
        if (_scrollOffset > maxScroll) { _scrollOffset = maxScroll; }
        WatchUi.requestUpdate();
    }

    private function getMinScroll() as Number {
        var centerY = _viewHeight / 2;
        var minScroll = HEADER_HEIGHT - centerY;
        if (minScroll > 0) {
            return 0;
        }
        return minScroll;
    }

    private function getMaxScroll(contentHeight as Number) as Number {
        var centerY = _viewHeight / 2;
        var maxScroll = contentHeight - centerY;
        if (maxScroll < 0) {
            return 0;
        }
        return maxScroll;
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

    //! Draws sync status footer message.
    private function drawSyncStatus(dc as Dc, x as Number, y as Number, w as Number) as Void {
        var status = getSyncStatus();
        dc.setColor(status.get("color") as Number, Graphics.COLOR_TRANSPARENT);
        dc.drawText(
            x + w,
            y + 4,
            Graphics.FONT_XTINY,
            status.get("text") as String,
            Graphics.TEXT_JUSTIFY_RIGHT
        );
    }

    //! Returns sync status text and color based on age.
    private function getSyncStatus() as Dictionary {
        var lastSync = EventStore.getLastSyncTime();
        if (lastSync == null) {
            return { "text" => "Disconnected", "color" => Graphics.COLOR_RED };
        }

        var nowMs = Time.now().value() * 1000;
        var ageMs = nowMs - (lastSync as Number);
        if (ageMs < 0) { ageMs = 0; }

        if (ageMs > DAY_MS) {
            return {
                "text" => "Last sync " + formatShortDateTime(lastSync as Number),
                "color" => Graphics.COLOR_RED
            };
        } else if (ageMs > FOUR_HOURS_MS) {
            return {
                "text" => "Last sync " + formatSyncTime(lastSync as Number),
                "color" => Graphics.COLOR_YELLOW
            };
        }

        return {
            "text" => "Last sync " + formatSyncTime(lastSync as Number),
            "color" => Graphics.COLOR_GREEN
        };
    }

    private function formatSyncTime(timestampMs as Number) as String {
        var info = Gregorian.info(new Time.Moment(timestampMs / 1000), Time.FORMAT_SHORT);
        return pad2(info.hour) + ":" + pad2(info.min);
    }

    private function formatShortDateTime(timestampMs as Number) as String {
        var info = Gregorian.info(new Time.Moment(timestampMs / 1000), Time.FORMAT_SHORT);
        return info.month + "/" + info.day + " " + pad2(info.hour) + ":" + pad2(info.min);
    }

    private function pad2(value as Number) as String {
        if (value < 10) {
            return "0" + value;
        }
        return value.toString();
    }

    function onHide() as Void {
    }
}
