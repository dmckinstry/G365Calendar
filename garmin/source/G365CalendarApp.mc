import Toybox.Application;
import Toybox.WatchUi;

//! Main application class for G365Calendar watch app.
//! Receives calendar event data from the Android companion app
//! and displays it on Garmin Venu series watches.
class G365CalendarApp extends Application.AppBase {

    function initialize() {
        AppBase.initialize();
    }

    function onStart(state as Dictionary?) as Void {
        DataReceiver.startListening();
    }

    function onStop(state as Dictionary?) as Void {
    }

    function getInitialView() as [Views] or [Views, InputDelegates] {
        var view = new G365CalendarView();
        return [view, new G365CalendarDelegate(view)];
    }
}
