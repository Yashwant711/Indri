# Indri

[![Android](https://img.shields.io/badge/Android-API%2027%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09.01-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase Realtime Database](https://img.shields.io/badge/Firebase-Realtime%20Database-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com/docs/database)
[![Stream Video](https://img.shields.io/badge/Stream-Video%20SDK-005FFF)](https://getstream.io/video/)

Indri is an Android application for monitoring sensor readings from a paired Bluetooth device while broadcasting live video. It reads MQ2 gas-sensor and distance values over Bluetooth RFCOMM, displays the latest reading during a Stream Video broadcast, writes readings to a PDF report, and publishes the latest formatted reading to Firebase Realtime Database.

> **Project status:** This repository was an early-stage prototype for my project "Indri : Remote Controlled Vehicle for Hazardous Gas Detection". It serves the computer on the remote device and connects to a workstation device using the internet.

## Features

- Pair-device workflow through the Android Bluetooth settings.
- Discovery of bonded Bluetooth devices from the app.
- Bluetooth Classic RFCOMM connection using the standard Serial Port Profile UUID.
- Live MQ2 and distance readings while a broadcast is active.
- Stream Video host call with start/stop broadcast controls.
- PDF report export to `Documents/Indri` through Android `MediaStore`.
- Latest report line written to the Firebase Realtime Database path `reading`.

## Tech Stack

- Kotlin 1.9.0 and Gradle Kotlin DSL
- Android Gradle Plugin 8.6.0
- Android SDK 34; minimum SDK 27
- Jetpack Compose with Material 3
- Stream Video Android Compose SDK 1.0.13
- Firebase Realtime Database using Firebase BoM 33.4.0
- JUnit and AndroidX instrumented tests

## Architecture

The app is a single Android application module with a small activity-based architecture:

```text
MainActivity
  -> requests/enables Bluetooth
  -> lists bonded devices
  -> opens an RFCOMM socket and stores it in SocketManager
  -> enables the streaming action after a successful connection

ActivityTwo
  -> reads the Bluetooth input stream through MyBluetoothManager
  -> parses newline-delimited "<mq2> <distance>" readings
  -> updates the Compose UI and writes PDF/Firebase output
  -> joins and hosts a Stream Video call

LiveStreamApplication
  -> initializes the Stream Video client
```

Sensor input is expected to be newline-delimited text with two space-separated fields, for example:

```text
145 32
```

The first value is treated as the MQ2 reading and the second as distance in centimeters. MQ2 values below `200` are rendered as normal readings; malformed input is recorded as `ERROR`.

## Requirements

- Android Studio with Android SDK 34 installed.
- JDK 17 or a compatible Android Studio-managed JDK for Android Gradle Plugin 8.6.0. The app itself targets Java 8 bytecode.
- A physical Android device or emulator running API 27 or newer.
- A device with Bluetooth and camera hardware. The manifest declares both as required.
- A Bluetooth Classic peripheral that exposes an RFCOMM/SPP service and emits the sensor format described above.
- A Firebase project configured for the Android application ID `com.example.indri`.
- A Stream Video project and a valid user token for the configured Stream user.

The app requests camera, microphone, and Bluetooth permissions at runtime where required by Android. A physical device is recommended because emulator Bluetooth and hardware sensor behavior vary.

## Installation

1. Open the repository in Android Studio.
2. Install Android SDK 34 and select a JDK supported by Android Studio.
3. Add the Firebase configuration file supplied by your Firebase project at `app/google-services.json`. Do not commit a production configuration file containing secrets unless your repository policy permits it.
4. Configure Stream credentials as described in [Environment Variables](#environment-variables).
5. Sync the Gradle project.
6. Build and install the debug variant:

   ```bash
   ./gradlew :app:assembleDebug
   ./gradlew :app:installDebug
   ```

   On Windows, use `gradlew.bat` instead of `./gradlew`.

## Usage

1. Launch **Indri** and allow the requested Bluetooth permissions.
2. Pair the sensor device in Android Bluetooth settings.
3. Tap **Show Devices**, select the paired device, and wait for the connection confirmation.
4. Tap **Start Streaming**. This action becomes available after the RFCOMM connection succeeds.
5. Grant camera and microphone permissions when prompted.
6. Tap **Start Broadcast** to go live. Sensor values appear above the video renderer while readings are received.
7. Tap **Stop Broadcast** to end the call and save the PDF report under `Documents/Indri`.

The current implementation uses the Stream call type `livestream` and a fixed call identifier. Change this to a deployment-specific identifier before supporting multiple sessions or users.

## Environment Variables

The current source does not read environment variables or Gradle properties for runtime credentials. Stream credentials are presently hard-coded in `LiveStreamApplication.kt`; this is unsuitable for production and any exposed token should be revoked and replaced.

Before a production build, move at least the following values into a secrets provider or a locally ignored Gradle properties file and load them through `BuildConfig` or another secure configuration layer:

| Variable | Purpose |
| --- | --- |
| `INDRI_STREAM_API_KEY` | Stream Video application API key |
| `INDRI_STREAM_USER_ID` | Stream user identifier |
| `INDRI_STREAM_USER_NAME` | Stream display name |
| `INDRI_STREAM_USER_TOKEN` | Short-lived Stream user token |
| `INDRI_STREAM_CALL_ID` | Stream livestream identifier |
| `INDRI_FIREBASE_DATABASE_URL` | Firebase Realtime Database endpoint, if selecting it at runtime |

Firebase Android initialization currently uses `app/google-services.json`. The file must match the application ID and Firebase project used for the build. Never place service-account credentials in the Android app; Android clients cannot keep embedded secrets private.

## Build and Test

```bash
# Compile the debug app
./gradlew :app:assembleDebug

# Run local unit tests
./gradlew :app:testDebugUnitTest

# Run instrumented tests on a connected device or emulator
./gradlew :app:connectedDebugAndroidTest
```

The repository currently contains baseline unit and instrumented tests. Hardware Bluetooth, Firebase, PDF export, and Stream Video flows require additional device and integration coverage before release.

## Security and Production Checklist

- Revoke the Stream token currently present in source control and issue short-lived user tokens from a trusted backend.
- Remove API keys and user identifiers from source code where they are not intentionally public.
- Restrict Firebase Realtime Database rules to authenticated, least-privilege access. The app currently writes the latest reading to `reading`.
- Validate and frame sensor messages robustly; the current parser expects exactly the first two space-separated fields.
- Handle Bluetooth disconnects, lifecycle cancellation, and duplicate report finalization.
- Add release signing, minification/obfuscation review, crash reporting, and CI checks.
- Test runtime permissions on Android 12 and newer, where Bluetooth permissions differ from legacy Android permissions.
