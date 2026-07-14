# Garmin watch app

The Garmin app is the watch-side portion of G365Calendar. It displays calendar events from the Android companion app and supports browsing the event list and detail views on supported Garmin devices.

## Supported devices

| Device | Connect IQ ID | Screen |
| --- | --- | --- |
| Venu 2 | `venu2` | 416×416 |
| Venu 2S | `venu2s` | 360×360 |
| Venu 2 Plus | `venu2plus` | 416×416 |
| Venu 3 | `venu3` | 390×390 |
| Venu 3S | `venu3s` | 360×360 |
| Venu 4 | `venu4` | TBD |
| Venu 4S | `venu4s` | TBD |

## Prerequisites

- Garmin Connect IQ SDK 4.0.0+
- A Garmin developer account
- The Connect IQ command-line tools available on your `PATH`

## Build and test

From the garmin folder, use `make` as follows:

| Target | Underlying command | Description |
| --- | --- | --- |
| `make build-garmin` or `make build` | `monkeyc -f garmin/monkey.jungle`... | Builds the Garmin app. |
| `make test-garmin` or `make test` | `monkeyc -f garmin/monkey.jungle -t`... and `connectiq && monkeydo -t`... | Builds and runs the Garmin unit tests. |
| `make dev-garmin` or `make dev` | `connectiq && monkeydo`... | Builds the Garmin app and launches it in the simulator. |

## Project structure

- [source](source/) — app entry point, event views, local storage, and companion communication
- [test](test/) — Connect IQ unit tests
- [resources](resources/) — strings, drawables, layouts, and other assets
- [manifest.xml](manifest.xml) — device targeting and permissions
