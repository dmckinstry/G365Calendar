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
        _view.scroll(_view.getRowHeight());
        return true;
    }

    function onPreviousPage() as Boolean {
        _view.scroll(-_view.getRowHeight());
        return true;
    }

    function onTap(clickEvent as WatchUi.ClickEvent) as Boolean {
        var coordinates = clickEvent.getCoordinates();
        return openEventAtY(coordinates[1]);
    }

    function onKey(keyEvent as WatchUi.KeyEvent) as Boolean {
        if (keyEvent.getKey() == WatchUi.KEY_ENTER) {
            return openEventAtY(_view.getViewHeight() / 2);
        }
        return false;
    }

    function onSelect() as Boolean {
        return false;
    }

    private function openEventAtY(y as Number) as Boolean {
        var index = _view.getEventIndexAtY(y);
        var event = _view.getEventAt(index);
        if (event != null) {
            var detailView = new EventDetailView(event);
            WatchUi.pushView(
                detailView,
                new EventDetailDelegate(detailView),
                WatchUi.SLIDE_LEFT
            );
            return true;
        }
        return false;
    }
}
