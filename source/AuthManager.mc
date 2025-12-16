using Toybox.Communications;
using Toybox.Application.Storage;
using Toybox.Lang;
using Toybox.System;
using Toybox.Time;

class AuthManager {

    private const STORAGE_KEY_ACCESS_TOKEN = "access_token";
    private const STORAGE_KEY_REFRESH_TOKEN = "refresh_token";
    private const STORAGE_KEY_TOKEN_EXPIRY = "token_expiry";
    
    // Microsoft OAuth endpoints
    private const OAUTH_AUTHORIZE_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    private const OAUTH_TOKEN_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private const CLIENT_ID = "YOUR_CLIENT_ID_HERE"; // Replace with actual client ID
    private const REDIRECT_URI = "https://localhost/oauth/redirect"; // Replace with actual redirect URI
    private const SCOPES = "Calendars.Read offline_access";

    function initialize() {
        Communications.registerForOAuthMessages(method(:onOAuthMessage));
    }

    function isAuthenticated() as Boolean {
        var accessToken = Storage.getValue(STORAGE_KEY_ACCESS_TOKEN);
        var expiry = Storage.getValue(STORAGE_KEY_TOKEN_EXPIRY);
        
        if (accessToken == null) {
            return false;
        }
        
        // Check if token is expired
        if (expiry != null) {
            var now = Time.now().value();
            if (now >= expiry) {
                // Token expired, try to refresh
                refreshToken();
                return false;
            }
        }
        
        return true;
    }

    function getAccessToken() as String? {
        if (isAuthenticated()) {
            return Storage.getValue(STORAGE_KEY_ACCESS_TOKEN) as String;
        }
        return null;
    }

    function startOAuthFlow() as Void {
        var params = {
            "response_type" => "code",
            "client_id" => CLIENT_ID,
            "redirect_uri" => REDIRECT_URI,
            "scope" => SCOPES,
            "response_mode" => "query"
        };
        
        Communications.makeOAuthRequest(
            OAUTH_AUTHORIZE_URL,
            params,
            REDIRECT_URI,
            Communications.OAUTH_RESULT_TYPE_URL,
            {}
        );
    }

    function onOAuthMessage(message as Communications.OAuthMessage) as Void {
        var data = message.data;
        
        if (message.responseCode == 200 && data != null) {
            if (data.hasKey("code")) {
                // Exchange authorization code for access token
                exchangeCodeForToken(data["code"] as String);
            } else if (data.hasKey("access_token")) {
                // Direct token response (from companion app)
                handleOAuthResponse(data);
            }
        } else {
            System.println("OAuth error: " + message.responseCode);
        }
    }

    function exchangeCodeForToken(code as String) as Void {
        var params = {
            "grant_type" => "authorization_code",
            "code" => code,
            "redirect_uri" => REDIRECT_URI,
            "client_id" => CLIENT_ID,
            "scope" => SCOPES
        };
        
        var options = {
            :method => Communications.HTTP_REQUEST_METHOD_POST,
            :headers => {
                "Content-Type" => Communications.REQUEST_CONTENT_TYPE_URL_ENCODED
            },
            :responseType => Communications.HTTP_RESPONSE_CONTENT_TYPE_JSON
        };
        
        Communications.makeWebRequest(
            OAUTH_TOKEN_URL,
            params,
            options,
            method(:onTokenReceived)
        );
    }

    function onTokenReceived(responseCode as Number, data as Dictionary?) as Void {
        if (responseCode == 200 && data != null) {
            handleOAuthResponse(data);
        } else {
            System.println("Token exchange error: " + responseCode);
        }
    }

    function handleOAuthResponse(data as Dictionary) as Void {
        if (data.hasKey("access_token")) {
            Storage.setValue(STORAGE_KEY_ACCESS_TOKEN, data["access_token"]);
            
            if (data.hasKey("refresh_token")) {
                Storage.setValue(STORAGE_KEY_REFRESH_TOKEN, data["refresh_token"]);
            }
            
            if (data.hasKey("expires_in")) {
                var expiresIn = data["expires_in"] as Number;
                var expiry = Time.now().value() + expiresIn;
                Storage.setValue(STORAGE_KEY_TOKEN_EXPIRY, expiry);
            }
            
            System.println("Authentication successful");
        }
    }

    function refreshToken() as Void {
        var refreshToken = Storage.getValue(STORAGE_KEY_REFRESH_TOKEN);
        
        if (refreshToken == null) {
            System.println("No refresh token available");
            return;
        }
        
        var params = {
            "grant_type" => "refresh_token",
            "refresh_token" => refreshToken,
            "client_id" => CLIENT_ID,
            "scope" => SCOPES
        };
        
        var options = {
            :method => Communications.HTTP_REQUEST_METHOD_POST,
            :headers => {
                "Content-Type" => Communications.REQUEST_CONTENT_TYPE_URL_ENCODED
            },
            :responseType => Communications.HTTP_RESPONSE_CONTENT_TYPE_JSON
        };
        
        Communications.makeWebRequest(
            OAUTH_TOKEN_URL,
            params,
            options,
            method(:onTokenReceived)
        );
    }

    function clearTokens() as Void {
        Storage.deleteValue(STORAGE_KEY_ACCESS_TOKEN);
        Storage.deleteValue(STORAGE_KEY_REFRESH_TOKEN);
        Storage.deleteValue(STORAGE_KEY_TOKEN_EXPIRY);
    }
}
