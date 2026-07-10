import Toybox.Application;
import Toybox.Communications;
import Toybox.Lang;

//! Receives calendar event data from the Android companion app
//! via the Garmin Connect Mobile SDK.
class CommListener extends Communications.ConnectionListener {

    function initialize() {
        ConnectionListener.initialize();
    }

    function onComplete() as Void {
        // Data transfer complete
    }

    function onError() as Void {
        // Data transfer error
    }

    function startListening() as Void {
        Communications.registerForPhoneAppMessages(method(:onMessage));
    }

    function onMessage(msg as Communications.PhoneAppMessage) as Void {
        var data = msg.data;
        if (data == null) {
            return;
        }

        if (data instanceof Dictionary) {
            var eventsData = data.get("events");
            var syncTimestamp = data.get("syncTimestamp");
            var eventCount = data.get("eventCount");

            if (eventsData != null) {
                EventStore.parseAndStore(eventsData, syncTimestamp, eventCount);
            }
        }
    }
}

//! Handles incoming messages from the companion app.
//! Parses the event data and stores it locally.
module DataReceiver {

    var _listener as CommListener or Null;

    //! Register to receive messages from the phone.
    function startListening() as Void {
        if (_listener == null) {
            _listener = new CommListener();
        }

        (_listener as CommListener).startListening();
    }
}
