import Toybox.WatchUi;
import Toybox.Lang;

//! Input delegate for handling user interactions on the calendar event list.
//! Supports scrolling via swipe/buttons and tapping to view event details.
class G365CalendarDelegate extends WatchUi.BehaviorDelegate {

    private var _view as G365CalendarView;

    function initialize(view as G365CalendarView) {
        BehaviorDelegate.initialize();
        _view = view;
    }

    function onBack() as Boolean {
        WatchUi.popView(WatchUi.SLIDE_RIGHT);
        return true;
    }

    function onNextPage() as Boolean {
        _view.scroll(60);
        return true;
    }

    function onPreviousPage() as Boolean {
        _view.scroll(-60);
        return true;
    }

    function onSelect() as Boolean {
        // Show detail for the currently centered event
        var index = _view.getEventIndexAtY(195); // center of ~390px screen
        var event = _view.getEventAt(index);
        if (event != null) {
            WatchUi.pushView(
                new EventDetailView(event),
                new EventDetailDelegate(),
                WatchUi.SLIDE_LEFT
            );
        }
        return true;
    }
}
