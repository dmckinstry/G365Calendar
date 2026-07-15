import Toybox.Graphics;
import Toybox.WatchUi;
import Toybox.Lang;

//! Detail view for a single calendar event.
//! Shows full title, time range, location, description, and calendar name with color.
class EventDetailView extends WatchUi.View {

    private var _event as Dictionary;
    private var _titleFontHeight as Number = 0;
    private var _calendarNameFontHeight as Number = 0;
    private var _timeFontHeight as Number = 0;
    private var _dateFontHeight as Number = 0;
    private var _locationFontHeight as Number = 0;
    private var _descriptionFontHeight as Number = 0;
    private var _contentHeight as Number = 0;
    private var _scrollOffset as Number = 0;
    private var _viewHeight as Number = 0;
    private const TOP_PADDING = 18;
    private const SIDE_PADDING = 12;
    private const LINE_SPACING = 6;

    function initialize(event as Dictionary) {
        View.initialize();
        _event = event;

        _titleFontHeight = Graphics.getFontHeight(Graphics.FONT_SMALL);
        _calendarNameFontHeight = Graphics.getFontHeight(Graphics.FONT_XTINY);
        _timeFontHeight = Graphics.getFontHeight(Graphics.FONT_TINY);
        _dateFontHeight = Graphics.getFontHeight(Graphics.FONT_XTINY);
        _locationFontHeight = Graphics.getFontHeight(Graphics.FONT_XTINY);
        _descriptionFontHeight = Graphics.getFontHeight(Graphics.FONT_XTINY);
    }

    function onLayout(dc as Dc) as Void {
    }

    function onUpdate(dc as Dc) as Void {
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();

        var width = dc.getWidth();
        var height = dc.getHeight();
        _viewHeight = height;

        var contentHeight = _titleFontHeight;
        var hasCalendarName = false;
        var hasDate = false;
        var hasLocation = false;
        var descriptionLines = getDescriptionLines(dc, width);

        var calendarNameValue = _event.get("calendarName");
        if (calendarNameValue != null && calendarNameValue instanceof String) {
            hasCalendarName = true;
            contentHeight += _calendarNameFontHeight + LINE_SPACING;
        }

        contentHeight += _timeFontHeight + LINE_SPACING;

        var startDateValue = _event.get("startDateTime");
        if (startDateValue != null && startDateValue instanceof String) {
            hasDate = true;
            contentHeight += _dateFontHeight + LINE_SPACING;
        }

        var locationValue = _event.get("location");
        if (locationValue != null && locationValue instanceof String) {
            hasLocation = true;
            contentHeight += _locationFontHeight + LINE_SPACING;
        }

        if (descriptionLines.size() > 0) {
            contentHeight += (descriptionLines.size() * _descriptionFontHeight) + LINE_SPACING;
        }

        _contentHeight = contentHeight;

        var minScroll = getMinScroll();
        var maxScroll = getMaxScroll();
        if (_scrollOffset < minScroll) {
            _scrollOffset = minScroll;
        }
        if (_scrollOffset > maxScroll) {
            _scrollOffset = maxScroll;
        }

        var y = TOP_PADDING - _scrollOffset;
        if (contentHeight < height - (TOP_PADDING * 2) && _scrollOffset == 0) {
            y = (height - contentHeight) / 2;
            if (y < TOP_PADDING) {
                y = TOP_PADDING;
            }
        }

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
        y += _titleFontHeight + LINE_SPACING;

        // Calendar name
        var calName = _event.get("calendarName");
        if (calName != null && calName instanceof String) {
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(width / 2, y, Graphics.FONT_XTINY, calName as String, Graphics.TEXT_JUSTIFY_CENTER);
            y += _calendarNameFontHeight + LINE_SPACING;
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
        y += _timeFontHeight + LINE_SPACING;

        // Date
        var startDt = _event.get("startDateTime");
        if (startDt != null && startDt instanceof String) {
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            dc.drawText(width / 2, y, Graphics.FONT_XTINY, formatDate(startDt as String), Graphics.TEXT_JUSTIFY_CENTER);
            y += _dateFontHeight + LINE_SPACING;
        }

        // Location
        var location = _event.get("location");
        if (location != null && location instanceof String) {
            dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
            dc.drawText(width / 2, y, Graphics.FONT_XTINY, location as String, Graphics.TEXT_JUSTIFY_CENTER);
            y += _locationFontHeight + LINE_SPACING;
        }

        if (descriptionLines.size() > 0) {
            dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
            for (var i = 0; i < descriptionLines.size(); i++) {
                if (y + _descriptionFontHeight >= 0 && y <= height) {
                    dc.drawText(
                        SIDE_PADDING,
                        y,
                        Graphics.FONT_XTINY,
                        descriptionLines[i],
                        Graphics.TEXT_JUSTIFY_LEFT
                    );
                }
                y += _descriptionFontHeight;
            }
        }
    }

    function scroll(delta as Number) as Void {
        var minScroll = getMinScroll();
        var maxScroll = getMaxScroll();
        _scrollOffset += delta;
        if (_scrollOffset < minScroll) {
            _scrollOffset = minScroll;
        }
        if (_scrollOffset > maxScroll) {
            _scrollOffset = maxScroll;
        }
        WatchUi.requestUpdate();
    }

    function getScrollStep() as Number {
        var step = _viewHeight - (TOP_PADDING * 2);
        if (step < _descriptionFontHeight) {
            return _descriptionFontHeight;
        }
        return step;
    }

    private function getMinScroll() as Number {
        var centerY = _viewHeight / 2;
        var minScroll = TOP_PADDING - centerY;
        if (minScroll > 0) {
            return 0;
        }
        return minScroll;
    }

    private function getMaxScroll() as Number {
        var centerY = _viewHeight / 2;
        var maxScroll = TOP_PADDING + _contentHeight - centerY;
        if (maxScroll < 0) {
            return 0;
        }
        return maxScroll;
    }

    private function getDescriptionLines(dc as Dc, width as Number) as Array<String> {
        var description = _event.get("description");
        if (!(description instanceof String) || (description as String).length() == 0) {
            return [] as Array<String>;
        }

        return wrapText(dc, description as String, width - (SIDE_PADDING * 2));
    }

    private function wrapText(dc as Dc, text as String, maxWidth as Number) as Array<String> {
        var lines = [] as Array<String>;
        var paragraphs = splitText(text, "\n");
        for (var i = 0; i < paragraphs.size(); i++) {
            appendWrappedParagraph(dc, paragraphs[i], maxWidth, lines);
            if (i < paragraphs.size() - 1) {
                lines.add("");
            }
        }
        return lines;
    }

    private function appendWrappedParagraph(dc as Dc, paragraph as String, maxWidth as Number, lines as Array<String>) as Void {
        if (paragraph.length() == 0) {
            lines.add("");
            return;
        }

        var words = splitText(paragraph, " ");
        var currentLine = "";
        for (var i = 0; i < words.size(); i++) {
            var word = words[i];
            if (word.length() == 0) {
                continue;
            }

            var candidate = currentLine;
            if (candidate.length() > 0) {
                candidate += " ";
            }
            candidate += word;

            if (currentLine.length() == 0 || dc.getTextWidthInPixels(candidate, Graphics.FONT_XTINY) <= maxWidth) {
                currentLine = candidate;
            } else {
                lines.add(currentLine);
                currentLine = word;
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine);
        }
    }

    private function splitText(text as String, delimiter as String) as Array<String> {
        var parts = [] as Array<String>;
        var start = 0;
        var delimiterLength = delimiter.length();
        var index = text.find(delimiter);

        while (index != null) {
            parts.add(text.substring(start, index));
            start = index + delimiterLength;
            if (start > text.length()) {
                break;
            }

            var remaining = text.substring(start, text.length());
            var nextIndex = remaining.find(delimiter);
            if (nextIndex == null) {
                index = null;
            } else {
                index = start + (nextIndex as Number);
            }
        }

        parts.add(text.substring(start, text.length()));
        return parts;
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

    private var _view as EventDetailView;

    function initialize(view as EventDetailView) {
        BehaviorDelegate.initialize();
        _view = view;
    }

    function onBack() as Boolean {
        WatchUi.popView(WatchUi.SLIDE_RIGHT);
        return true;
    }

    function onNextPage() as Boolean {
        _view.scroll(_view.getScrollStep());
        return true;
    }

    function onPreviousPage() as Boolean {
        _view.scroll(-_view.getScrollStep());
        return true;
    }

    function onSwipe(swipeEvent as WatchUi.SwipeEvent) as Boolean {
        var direction = swipeEvent.getDirection();
        if (direction == WatchUi.SWIPE_UP) {
            _view.scroll(_view.getScrollStep());
            return true;
        }
        if (direction == WatchUi.SWIPE_DOWN) {
            _view.scroll(-_view.getScrollStep());
            return true;
        }
        return false;
    }
}
