# Integration Test Plan — G365Calendar

This document describes manual integration test procedures for the G365Calendar system.
Automated E2E testing is limited by the need for physical Garmin hardware and M365 accounts.

## Prerequisites
- Android device with Garmin Connect app installed
- Garmin Venu 2, 3, or 4 series watch paired via Bluetooth
- M365 account with at least one calendar containing events
- Azure AD app registration configured with `Calendars.Read` permission

## Test Cases

### TC-01: Fresh Install & Sign-In
1. Install the Android companion app
2. Launch the app — should show "Sign in with Microsoft" button
3. Tap sign in — MSAL browser should open for authentication
4. Complete M365 login with valid credentials
5. **Expected**: App shows authenticated state with user display name

### TC-02: Calendar Selection
1. After sign-in, tap "Select Calendars"
2. **Expected**: List of M365 calendars appears with checkboxes
3. Toggle calendars on/off
4. Navigate back — selections should persist
5. Kill and restart app — selections should still persist

### TC-03: Manual Sync
1. With calendars selected, tap "Sync Now"
2. **Expected**: Sync status shows "Syncing…" then "X events synced"
3. Check watch — events should appear in the G365 Calendar app

### TC-04: Event List Display (Watch)
1. Open G365 Calendar on the watch
2. **Expected**: Scrollable list of events with title, time, location
3. Events should show calendar color indicators on the left
4. Scroll up/down through the list using swipe or buttons

### TC-05: Event Detail View (Watch)
1. From the event list, tap/select an event
2. **Expected**: Detail view shows full title, time range, date, location, calendar name
3. Calendar color bar at top
4. Press back to return to list

### TC-06: Background Sync (60 min)
1. Complete initial sync and note the time
2. Wait 60+ minutes (or adjust WorkManager for testing)
3. **Expected**: Events update automatically without manual intervention
4. "Last sync" timestamp should update

### TC-07: No Network Handling
1. Disable network connectivity on the Android device
2. Tap "Sync Now"
3. **Expected**: Error message displayed, no crash

### TC-08: Watch Disconnected
1. Disconnect the Garmin watch (turn off Bluetooth)
2. Tap "Sync Now"
3. **Expected**: "Watch not connected" error message

### TC-09: Token Expiry & Refresh
1. Wait for the MSAL token to expire (typically 1 hour)
2. Trigger a sync
3. **Expected**: Token silently refreshes, sync succeeds

### TC-10: Sign Out
1. Tap "Sign Out"
2. **Expected**: App returns to sign-in screen
3. Background sync should stop
4. Watch data remains until next sync

### TC-11: Multi-Device (Venu 2, 3, 4)
1. Install the watch app on each target device
2. Verify event list renders correctly on each screen size
3. **Expected**: No layout overflow or cut-off text on any device

## Notes
- WorkManager minimum interval is 15 minutes; 60-minute sync may be delayed by Android Doze mode
- Watch storage limit is ~128KB; events are capped at 50 in EventStore
- MSAL tokens are cached automatically; explicit refresh only on expiry
