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

## Build and run

Build the app with:

```bash
monkeyc -f garmin/monkey.jungle -d venu3 -o garmin/bin/G365Calendar.prg
```

Run the Connect IQ test app with:

```bash
monkeyc -f garmin/monkey.jungle -d venu3 -t -o garmin/bin/G365Calendar-test.prg
connectiq && monkeydo garmin/bin/G365Calendar-test.prg venu3
```

## Project structure

- [source](source/) — app entry point, event views, local storage, and companion communication
- [test](test/) — Connect IQ unit tests
- [resources](resources/) — strings, drawables, layouts, and other assets
- [manifest.xml](manifest.xml) — device targeting and permissions
