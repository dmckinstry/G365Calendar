# G365Calendar

G365Calendar is a two-part project that bridges Microsoft 365 calendar data to Garmin wearable devices. The Android companion app handles authentication, calendar sync, and data exchange, while the Garmin app renders the events on the watch.

## Architecture

```text
Microsoft 365 Graph API
        │
        ▼
Android companion app (Kotlin + Jetpack Compose)
        │
        ▼
Garmin Connect Mobile SDK / Bluetooth
        │
        ▼
Garmin watch app (Monkey C / Connect IQ)
```

## Repository layout

- [android/](android/) — Android companion app and build instructions
- [garmin/](garmin/) — Garmin watch app and Connect IQ build instructions
- [docs/integration-test-plan.md](docs/integration-test-plan.md) — manual end-to-end validation checklist

## Quick start

1. Review the platform-specific guides:
   - [android/README.md](android/README.md)
   - [garmin/README.md](garmin/README.md)
2. Configure the Microsoft 365 app registration for the Android app.
3. Build and run the Android app with Gradle.
4. Build and install the Garmin app with Connect IQ tooling.

## Common prerequisites

- JDK 17 or later
- Android SDK with API 26+ and compile SDK 35
- Garmin Connect IQ SDK and a Garmin developer account
- A Microsoft Entra app registration with the `Calendars.Read` permission

## Notes

- The Android app performs background sync every 60 minutes via WorkManager.
- The Garmin app caches recent events locally for offline viewing.
- The current implementation targets Android and Garmin wearables; iOS support is not included.

## License

MIT — see [LICENSE](LICENSE) for details.

