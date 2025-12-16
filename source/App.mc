using Toybox.Application;
using Toybox.Lang;
using Toybox.WatchUi;
using Toybox.Communications;

class G365CalendarApp extends Application.AppBase {

    private var _authManager as AuthManager?;
    private var _apiClient as ApiClient?;

    function initialize() {
        AppBase.initialize();
    }

    // onStart() is called on application start up
    function onStart(state as Dictionary?) as Void {
        _authManager = new AuthManager();
        _apiClient = new ApiClient(_authManager);
    }

    // onStop() is called when your application is exiting
    function onStop(state as Dictionary?) as Void {
        // Save any necessary state
    }

    // Return the initial view of your application here
    function getInitialView() as Array<Views or InputDelegates>? {
        var view = new CalendarView();
        var delegate = new CalendarViewDelegate(_authManager, _apiClient);
        return [view, delegate] as Array<Views or InputDelegates>;
    }

    // Handle incoming communications
    function onMessage(msg as Communications.Message) as Void {
        var data = msg.data;
        
        if (data instanceof Dictionary) {
            if (data.hasKey("oauth_token")) {
                // Handle OAuth token from companion app
                _authManager.handleOAuthResponse(data);
            } else if (data.hasKey("calendar_events")) {
                // Handle calendar events from companion app
                _apiClient.cacheEvents(data["calendar_events"]);
            }
        }
    }

    function getAuthManager() as AuthManager {
        return _authManager;
    }

    function getApiClient() as ApiClient {
        return _apiClient;
    }
}

function getApp() as G365CalendarApp {
    return Application.getApp() as G365CalendarApp;
}
