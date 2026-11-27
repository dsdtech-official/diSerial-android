# diSerial for Android

diSerial by DSD TECH is a USB serial terminal for Android phones and tablets. Connect a compatible single-port USB serial adapter to send and receive data, reuse saved commands, plot incoming values, and record serial sessions. The wide tablet layout shows the terminal and command shortcuts side by side.

| Requirement | Details |
|---|---|
| Android | Android 7.0 (API 24) or later |
| Serial connection | USB Host (OTG) support and a compatible single-port USB serial adapter |
| Verified hardware | Android 1.0.0 was exercised on a Galaxy S22 with a CP2102N adapter. Compatibility varies by device and adapter. |

## Download diSerial

- [Get diSerial on Google Play](https://play.google.com/store/apps/details?id=com.dsdtech.diserial_mobile)

## Features

- **USB serial terminal.** Configure baud rate, data bits, stop bits and parity. Send ASCII with optional line endings or hexadecimal data with an optional frame checksum. Repeat a send at a set interval. View received data as text or hexadecimal with filters and auto-scroll.
- **Command shortcuts.** Save a command from send history as a shortcut and replay the original bytes with one tap. Organize shortcuts into groups and import or export them as JSON.
- **Live plots.** Plot incoming values with CSV, key-value, or custom binary parsing rules. Pause plotting, inspect the latest value on each channel, or use sample data to preview a custom profile.
- **Session logs.** Record traffic locally, browse sessions by date, inspect individual frames, and export captures as TXT or CSV.

## Quick start

1. Connect a compatible USB serial adapter using a data-capable USB connection with OTG support.
2. Open diSerial, select the USB adapter, and allow the Android USB access request.
3. Match the serial settings to the connected equipment, then send a command or watch incoming data in the terminal.
4. Use the visualization view to plot incoming values or the log library to browse and export recordings.

## Build and verify

Install JDK 17 or a newer version supported by Gradle 9.3.1, Android SDK platforms 36 and 36.1, and Android SDK Build Tools 36.0.0. Set `JAVA_HOME` and `ANDROID_HOME`, or configure the SDK location with `sdk.dir` in a local `local.properties` file. The first build downloads Gradle and its dependencies.

On macOS or Linux:

```sh
./gradlew verifyPlatformBoundaries :app:assembleDebug
```

On Windows in PowerShell:

```powershell
.\gradlew.bat verifyPlatformBoundaries :app:assembleDebug
```

`verifyPlatformBoundaries` checks platform-specific imports in the shared production source sets. The build writes `app/build/outputs/apk/debug/app-debug.apk` and uses the local Android debug signing key.

## Code layout

| Path | Contents |
|---|---|
| [`app/`](app/) | Android app entry point, manifest, build configuration and resources |
| `shared/src/commonMain/` | Compose UI, serial protocol handling, plotting, and shared storage models |
| `shared/src/androidMain/` | Android USB transport, storage and platform integration |
| [`gradle/`](gradle/) | Gradle Wrapper configuration, dependency versions and platform-boundary verification |

## Support and license

- [Privacy policy](https://dsdtech-official.github.io/diSerial-android/privacy/)
- [DSD TECH support](mailto:dsd_tech@outlook.com)

Copyright 2026 DongGuan DESHIDE TECHNOLOGY CO.,LTD

Project source is licensed under the [Apache License, Version 2.0](LICENSE). See [NOTICE](NOTICE) for bundled third-party components and their licenses.
