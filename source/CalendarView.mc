using Toybox.WatchUi;
using Toybox.Graphics;
using Toybox.System;
using Toybox.Lang;
using Toybox.Time;
using Toybox.Time.Gregorian;

class CalendarView extends WatchUi.View {

    private var _events as Array?;
    private var _currentIndex as Number;
    private var _isLoading as Boolean;

    function initialize() {
        View.initialize();
        _currentIndex = 0;
        _isLoading = false;
        _events = null;
    }

    // Load your resources here
    function onLayout(dc as Dc) as Void {
        setLayout(Rez.Layouts.MainLayout(dc));
    }

    // Called when this View is brought to the foreground
    function onShow() as Void {
        loadEvents();
    }

    // Update the view
    function onUpdate(dc as Dc) as Void {
        View.onUpdate(dc);
        
        var width = dc.getWidth();
        var height = dc.getHeight();
        
        dc.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
        dc.clear();
        
        if (_isLoading) {
            drawLoadingScreen(dc, width, height);
        } else if (_events != null && _events.size() > 0) {
            drawEvent(dc, width, height);
        } else {
            drawNoEventsScreen(dc, width, height);
        }
    }

    function drawLoadingScreen(dc as Dc, width as Number, height as Number) as Void {
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(width / 2, height / 2, Graphics.FONT_MEDIUM, "Loading...", Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER);
    }

    function drawNoEventsScreen(dc as Dc, width as Number, height as Number) as Void {
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(width / 2, height / 2, Graphics.FONT_MEDIUM, "No upcoming events", Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER);
    }

    function drawEvent(dc as Dc, width as Number, height as Number) as Void {
        if (_events == null || _currentIndex >= _events.size()) {
            return;
        }
        
        var event = _events[_currentIndex] as Dictionary;
        var subject = event.get("subject") as String;
        var startTime = event.get("start") as String;
        var endTime = event.get("end") as String;
        var location = event.get("location") as String;
        
        var yPos = 40;
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        
        // Draw event counter
        var counterText = (_currentIndex + 1) + "/" + _events.size();
        dc.drawText(width / 2, 10, Graphics.FONT_TINY, counterText, Graphics.TEXT_JUSTIFY_CENTER);
        
        // Draw subject
        dc.drawText(width / 2, yPos, Graphics.FONT_MEDIUM, subject, Graphics.TEXT_JUSTIFY_CENTER);
        yPos += 35;
        
        // Draw time
        var timeText = formatTime(startTime) + " - " + formatTime(endTime);
        dc.drawText(width / 2, yPos, Graphics.FONT_SMALL, timeText, Graphics.TEXT_JUSTIFY_CENTER);
        yPos += 25;
        
        // Draw location if available
        if (location != null && !location.equals("")) {
            dc.drawText(width / 2, yPos, Graphics.FONT_SMALL, location, Graphics.TEXT_JUSTIFY_CENTER);
        }
        
        // Draw navigation hints
        dc.drawText(width / 2, height - 30, Graphics.FONT_TINY, "Swipe to scroll", Graphics.TEXT_JUSTIFY_CENTER);
    }

    function formatTime(isoString as String) as String {
        // Simple time formatting - extract HH:MM from ISO 8601 string
        // Expected format: "2024-01-15T14:30:00Z"
        if (isoString.length() >= 16) {
            return isoString.substring(11, 16);
        }
        return isoString;
    }

    function loadEvents() as Void {
        _isLoading = true;
        WatchUi.requestUpdate();
        
        var app = getApp();
        var apiClient = app.getApiClient();
        
        if (apiClient != null) {
            _events = apiClient.getCachedEvents();
            apiClient.fetchEvents(method(:onEventsReceived));
        }
        
        _isLoading = false;
        WatchUi.requestUpdate();
    }

    function onEventsReceived(events as Array?) as Void {
        _events = events;
        _currentIndex = 0;
        _isLoading = false;
        WatchUi.requestUpdate();
    }

    function scrollNext() as Void {
        if (_events != null && _currentIndex < _events.size() - 1) {
            _currentIndex++;
            WatchUi.requestUpdate();
        }
    }

    function scrollPrevious() as Void {
        if (_currentIndex > 0) {
            _currentIndex--;
            WatchUi.requestUpdate();
        }
    }
}

class CalendarViewDelegate extends WatchUi.BehaviorDelegate {

    private var _authManager as AuthManager;
    private var _apiClient as ApiClient;

    function initialize(authManager as AuthManager, apiClient as ApiClient) {
        BehaviorDelegate.initialize();
        _authManager = authManager;
        _apiClient = apiClient;
    }

    function onSwipe(swipeEvent as SwipeEvent) as Boolean {
        var view = WatchUi.getCurrentView()[0];
        
        if (view instanceof CalendarView) {
            if (swipeEvent.getDirection() == WatchUi.SWIPE_UP) {
                view.scrollNext();
                return true;
            } else if (swipeEvent.getDirection() == WatchUi.SWIPE_DOWN) {
                view.scrollPrevious();
                return true;
            }
        }
        
        return false;
    }

    function onSelect() as Boolean {
        // Trigger authentication flow if not authenticated
        if (!_authManager.isAuthenticated()) {
            _authManager.startOAuthFlow();
            return true;
        }
        
        // Refresh events
        var view = WatchUi.getCurrentView()[0];
        if (view instanceof CalendarView) {
            view.loadEvents();
        }
        
        return true;
    }
}
