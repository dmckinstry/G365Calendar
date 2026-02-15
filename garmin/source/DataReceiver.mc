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
}

//! Handles incoming messages from the companion app.
//! Parses the event data and stores it locally.
module DataReceiver {

    //! Register to receive messages from the phone.
    function startListening() as Void {
        Communications.registerForPhoneAppMessages(method(:onMessage));
    }

    //! Callback when a message is received from the companion app.
    function onMessage(msg as Communications.Message) as Void {
        var data = msg.data;
        if (data == null) {
            return;
        }

        if (data instanceof Dictionary) {
            var eventsJson = data.get("events");
            var syncTimestamp = data.get("syncTimestamp");
            var eventCount = data.get("eventCount");

            if (eventsJson != null && eventsJson instanceof String) {
                EventStore.parseAndStore(eventsJson as String, syncTimestamp, eventCount);
            }
        }
    }
}
