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
   - [android/README.md](android/README.md) for Android setup, build, and test instructions
   - [garmin/README.md](garmin/README.md) for Garmin build and device-specific instructions
2. Configure the Microsoft 365 app registration for the Android app.
3. Set the Android calendar configuration through `local.properties`, Gradle properties, or environment variables as documented in [android/README.md](android/README.md).
4. Follow the Android and Garmin setup steps in their respective guides.

## Common prerequisites

- JDK 17 or later
- Android SDK with API 26+ and compile SDK 35
- Garmin Connect IQ SDK and a Garmin developer account
- A Microsoft Entra app registration with the `Calendars.Read` permission

## Configuration notes

- The Android companion generates its packaged MSAL config at build time rather than storing the Azure client ID in a checked-in resource file.
- Android development requires `android/local.properties` for `azureAppId`, `azureTenantId`, `graphScopes`, and `graphBaseUrl` in local builds and/or `AZURE_APP_ID`, `AZURE_TENANT_ID`, `GRAPH_SCOPES`, and `GRAPH_BASE_URL` for all builds.

## Build and test

From the project root, use the repo-level Make targets as follows:

| Target | Underlying command | Description |
| --- | --- | --- |
| `make build` | `make -C android build` and `make -C garmin build` | Builds both the Android and Garmin apps. |
| `make test` | `make -C android test` and `make -C garmin test` | Runs the Android and Garmin test workflows. |
| `make dev` | `make -C android dev` and `make -C garmin dev` | Starts the Android and Garmin development flows. |

## Notes

- The Android app performs background sync every 60 minutes via WorkManager.
- The Garmin app caches recent events locally for offline viewing.
- The current implementation targets Android and Garmin wearables; iOS support is not included.
- Full end-to-end companion communication debugging is limited by the Garmin Connect Mobile / Connect IQ ecosystem: Android emulators cannot fully emulate the required phone-to-watch companion path, so validating message exchange typically requires a real Android phone with Garmin Connect Mobile and a paired Garmin device.

## License

MIT — see [LICENSE](LICENSE) for details.

