import Toybox.Graphics;
import Toybox.Time;
import Toybox.Time.Gregorian;
import Toybox.WatchUi;
import Toybox.Lang;

//! Main view displaying a scrollable list of calendar events.
//! Shows event title and start time with calendar color indicator.
class G365CalendarView extends WatchUi.View {

    private const WEEKDAY_ABBR = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

    private var _events as Array<Dictionary> = [] as Array<Dictionary>;
    private var _scrollOffset as Number = 0;
    private var _viewWidth as Number = 390;
    private var _viewHeight as Number = 390;

    // Vertical padding/spacing between text lines, in pixels.
    private const LINE_SPACING = 4;
    private const CONTENT_SIDE_PADDING = 10;
    private const TEXT_LEFT_PADDING = 12;
    private const ROW_TOP_PADDING = 4;
    private const ROW_BOTTOM_PADDING = 6;
    private const HEADER_TOP_PADDING = 6;
    private const HEADER_BOTTOM_PADDING = 6;
    private const FOOTER_BOTTOM_PADDING = 18;
    private const MAX_VISIBLE_ROWS = 7;

    // Font heights are measured (not guessed) so rows never overlap
    // regardless of device font metrics/resolution.
    private var _titleFontHeight as Number = 0;
    private var _timeFontHeight as Number = 0;
    private var _headerFontHeight as Number = 0;

    private var ITEM_HEIGHT as Number = 65;
    private var HEADER_HEIGHT as Number = 40;
    private const STATUS_HEIGHT = 24;
    private const FOUR_HOURS_MS = 4 * 60 * 60 * 1000;
    private const DAY_MS = 24 * 60 * 60 * 1000;
    private const SIX_DAYS_MS = 6 * DAY_MS;

    function initialize() {
        View.initialize();
        initializeMetrics();
    }

    function onLayout(dc as Dc) as Void {
        _viewWidth = dc.getWidth();
        _viewHeight = dc.getHeight();
        initializeMetrics();
        setLayout(Rez.Layouts.MainLayout(dc));
        configureStaticDrawables();
    }

    function onShow() as Void {
        _events = EventStore.getEvents();
        WatchUi.requestUpdate();
    }

    function onUpdate(dc as Dc) as Void {
        if (_viewWidth != dc.getWidth() || _viewHeight != dc.getHeight()) {
            _viewWidth = dc.getWidth();
            _viewHeight = dc.getHeight();
            configureStaticDrawables();
        }

        syncDrawables();
        View.onUpdate(dc);
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

    private function initializeMetrics() as Void {
        _titleFontHeight = Graphics.getFontHeight(Graphics.FONT_TINY);
        _timeFontHeight = Graphics.getFontHeight(Graphics.FONT_XTINY);
        _headerFontHeight = Graphics.getFontHeight(Graphics.FONT_SMALL);

        HEADER_HEIGHT = HEADER_TOP_PADDING + _headerFontHeight + HEADER_BOTTOM_PADDING;
        ITEM_HEIGHT = ROW_TOP_PADDING
            + _titleFontHeight + LINE_SPACING
            + _timeFontHeight + ROW_BOTTOM_PADDING;
    }

    private function configureStaticDrawables() as Void {
        var noEventsLabel = getTextDrawable("NoEventsLabel");
        if (noEventsLabel != null) {
            noEventsLabel.setText("No events");
            noEventsLabel.setLocation(_viewWidth / 2, _viewHeight / 2);
        }

        var syncStatusLabel = getTextDrawable("SyncStatusLabel");
        if (syncStatusLabel != null) {
            syncStatusLabel.setLocation(_viewWidth / 2, _viewHeight - 20);
        }
    }

    private function syncDrawables() as Void {
        var noEventsLabel = getTextDrawable("NoEventsLabel");
        var syncStatusLabel = getTextDrawable("SyncStatusLabel");

        hideAllRows();

        if (_events.size() == 0) {
            if (noEventsLabel != null) {
                noEventsLabel.setVisible(true);
            }
            updateSyncStatusLabel(syncStatusLabel, _viewHeight - 26);
            return;
        }

        if (noEventsLabel != null) {
            noEventsLabel.setVisible(false);
        }

        var rowY = HEADER_HEIGHT - _scrollOffset;
        var slot = 0;
        for (var i = 0; i < _events.size() && slot < MAX_VISIBLE_ROWS; i++) {
            if (rowY + ITEM_HEIGHT > 0 && rowY < _viewHeight) {
                updateRowSlot(slot, _events[i], rowY);
                slot += 1;
            }
            rowY += ITEM_HEIGHT;
        }

        if (rowY + STATUS_HEIGHT + FOOTER_BOTTOM_PADDING > 0 && rowY < _viewHeight) {
            updateSyncStatusLabel(syncStatusLabel, rowY + 4);
        } else if (syncStatusLabel != null) {
            syncStatusLabel.setVisible(false);
        }
    }

    private function updateRowSlot(slot as Number, event as Dictionary, rowY as Number) as Void {
        var rowWidth = _viewWidth - (CONTENT_SIDE_PADDING * 2);
        var textX = CONTENT_SIDE_PADDING + TEXT_LEFT_PADDING;

        var decor = getRowDecor(slot);
        if (decor != null) {
            decor.setLocation(CONTENT_SIDE_PADDING, rowY);
            decor.setSize(rowWidth, ITEM_HEIGHT);
            decor.configure(ITEM_HEIGHT, getEventColor(event));
            decor.setVisible(true);
        }

        var titleLabel = getTextDrawable(getRowDrawableId(slot, "Title"));
        if (titleLabel != null) {
            titleLabel.setText(getEventTitle(event));
            titleLabel.setLocation(textX, rowY + ROW_TOP_PADDING);
            titleLabel.setVisible(true);
        }

        var timeLabel = getTextDrawable(getRowDrawableId(slot, "Time"));
        if (timeLabel != null) {
            timeLabel.setText(getEventTime(event));
            timeLabel.setLocation(textX, rowY + ROW_TOP_PADDING + _titleFontHeight + LINE_SPACING);
            timeLabel.setVisible(true);
        }
    }

    private function updateSyncStatusLabel(syncStatusLabel as WatchUi.Text or Null, y as Number) as Void {
        if (syncStatusLabel == null) {
            return;
        }

        var status = getSyncStatus();
        syncStatusLabel.setText(status.get("text") as String);
        syncStatusLabel.setColor(status.get("color") as Number);
        syncStatusLabel.setLocation(_viewWidth / 2, y);
        syncStatusLabel.setVisible(true);
    }

    private function hideAllRows() as Void {
        for (var slot = 0; slot < MAX_VISIBLE_ROWS; slot++) {
            hideRowSlot(slot);
        }
    }

    private function hideRowSlot(slot as Number) as Void {
        var decor = getRowDecor(slot);
        if (decor != null) {
            decor.setVisible(false);
        }

        var titleLabel = getTextDrawable(getRowDrawableId(slot, "Title"));
        if (titleLabel != null) {
            titleLabel.setVisible(false);
        }

        var timeLabel = getTextDrawable(getRowDrawableId(slot, "Time"));
        if (timeLabel != null) {
            timeLabel.setVisible(false);
        }

    }

    private function getRowDecor(slot as Number) as EventRowDecorationDrawable or Null {
        var drawable = findDrawableById(getRowDrawableId(slot, "Decor"));
        if (drawable != null && drawable instanceof EventRowDecorationDrawable) {
            return drawable as EventRowDecorationDrawable;
        }
        return null;
    }

    private function getTextDrawable(id as String) as WatchUi.Text or Null {
        var drawable = findDrawableById(id);
        if (drawable != null && drawable instanceof WatchUi.Text) {
            return drawable as WatchUi.Text;
        }
        return null;
    }

    private function getRowDrawableId(slot as Number, suffix as String) as String {
        return "Row" + slot.toString() + suffix;
    }

    private function getEventColor(event as Dictionary) as Number {
        var colorStr = event.get("calendarColor");
        if (colorStr != null && colorStr instanceof String) {
            return parseColor(colorStr as String);
        }
        return Graphics.COLOR_BLUE;
    }

    private function getEventTitle(event as Dictionary) as String {
        var title = event.get("title");
        if (title == null) {
            return "(No title)";
        }
        return title as String;
    }

    private function getEventTime(event as Dictionary) as String {
        if (event.get("isAllDay") == true) {
            return "All day";
        }

        var startTime = event.get("startDateTime");
        if (startTime != null && startTime instanceof String) {
            return formatEventStart(startTime as String);
        }

        return "";
    }

    private function formatEventStart(isoDateTime as String) as String {
        var parts = parseIsoDateParts(isoDateTime);
        if (parts == null) {
            return isoDateTime;
        }

        var month = parts.get("month") as Number;
        var day = parts.get("day") as Number;
        var hour = parts.get("hour") as Number;
        var min = parts.get("min") as Number;
        var weekday = parts.get("weekday") as Number;
        var eventDateValue = parts.get("dateValue") as Number;

        var nowInfo = Gregorian.info(Time.now(), Time.FORMAT_SHORT);
        var todayValue = encodeDateValue(nowInfo.year, nowInfo.month, nowInfo.day);

        if (eventDateValue > todayValue && daysBetweenDates(todayValue, eventDateValue) > 6) {
            return pad2(month) + "-" + pad2(day) + " " + pad2(hour) + ":" + pad2(min);
        }

        return WEEKDAY_ABBR[weekday] + " " + pad2(hour) + ":" + pad2(min);
    }

    private function parseIsoDateParts(isoDateTime as String) as Dictionary or Null {
        var tIndex = isoDateTime.find("T");
        if (tIndex == null || tIndex < 10 || isoDateTime.length() < (tIndex as Number) + 6) {
            return null;
        }

        try {
            var year = isoDateTime.substring(0, 4).toNumber();
            var month = isoDateTime.substring(5, 7).toNumber();
            var day = isoDateTime.substring(8, 10).toNumber();
            var hour = isoDateTime.substring((tIndex as Number) + 1, (tIndex as Number) + 3).toNumber();
            var min = isoDateTime.substring((tIndex as Number) + 4, (tIndex as Number) + 6).toNumber();

            if (year == null || month == null || day == null || hour == null || min == null) {
                return null;
            }

            return {
                "year" => year,
                "month" => month,
                "day" => day,
                "hour" => hour,
                "min" => min,
                "weekday" => getWeekday(year as Number, month as Number, day as Number),
                "dateValue" => encodeDateValue(year as Number, month as Number, day as Number)
            };
        } catch (e) {
            return null;
        }
    }

    private function getWeekday(year as Number, month as Number, day as Number) as Number {
        var adjustedMonth = month;
        var adjustedYear = year;
        if (adjustedMonth < 3) {
            adjustedMonth += 12;
            adjustedYear -= 1;
        }

        var century = adjustedYear / 100;
        var yearOfCentury = adjustedYear % 100;
        var h = (day
            + (((13 * (adjustedMonth + 1)) / 5).toNumber())
            + yearOfCentury
            + ((yearOfCentury / 4).toNumber())
            + ((century / 4).toNumber())
            + (5 * century)) % 7;

        return ((h + 6) % 7).toNumber();
    }

    private function daysBetweenDates(startValue as Number, endValue as Number) as Number {
        if (endValue <= startValue) {
            return 0;
        }

        var startParts = decodeDateValue(startValue);
        var endParts = decodeDateValue(endValue);
        return toOrdinalDay(endParts.get("year") as Number, endParts.get("month") as Number, endParts.get("day") as Number)
            - toOrdinalDay(startParts.get("year") as Number, startParts.get("month") as Number, startParts.get("day") as Number);
    }

    private function encodeDateValue(year as Number, month as Number, day as Number) as Number {
        return (year * 10000) + (month * 100) + day;
    }

    private function decodeDateValue(value as Number) as Dictionary {
        return {
            "year" => (value / 10000).toNumber(),
            "month" => ((value / 100) % 100).toNumber(),
            "day" => (value % 100).toNumber()
        };
    }

    private function toOrdinalDay(year as Number, month as Number, day as Number) as Number {
        var days = day;
        for (var currentMonth = 1; currentMonth < month; currentMonth++) {
            days += getDaysInMonth(year, currentMonth);
        }

        return days + daysBeforeYear(year);
    }

    private function daysBeforeYear(year as Number) as Number {
        var previousYear = year - 1;
        return (previousYear * 365)
            + ((previousYear / 4).toNumber())
            - ((previousYear / 100).toNumber())
            + ((previousYear / 400).toNumber());
    }

    private function getDaysInMonth(year as Number, month as Number) as Number {
        if (month == 2) {
            if (isLeapYear(year)) {
                return 29;
            }
            return 28;
        }

        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }

        return 31;
    }

    private function isLeapYear(year as Number) as Boolean {
        if ((year % 400) == 0) {
            return true;
        }
        if ((year % 100) == 0) {
            return false;
        }
        return (year % 4) == 0;
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
