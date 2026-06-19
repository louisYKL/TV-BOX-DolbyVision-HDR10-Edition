# TV BOX Dolby Vision / HDR10 Support Edition

> A TV-first TVBox branch focused on native hardware playback, HDR activation, Dolby Vision fallback routing, and living-room friendly interaction.

`Version 0.1` ・ [简体中文](README.md)

## What this project is

This is not a “just make it play somehow” TVBox fork.

The branch is built around real TV use cases: native playback when the device can do it well, a compatibility path when vendor firmware cannot, proper HDR routing, subtitle handling, audio passthrough behavior, and remote-control friendly fullscreen interaction.

The core principle is simple:

- If the device system player can handle the stream reliably, keep it on the native path.
- If the native chain is unstable for the container or stream type, route it to the built-in compatibility player.
- Detect HDR10, HDR10+, and Dolby Vision from the actual video stream, not from titles or filenames.

## What this branch focuses on

- Preserve the native hardware decode path for standard HDR10 / HDR10+ playback.
- Add a more reliable compatibility chain for MKV / WebM / difficult Dolby Vision cases.
- Prefer HDR10 base-layer playback on devices without native Dolby Vision decoding.
- Keep subtitles, audio passthrough, fullscreen controls, and remote focus behavior consistent for TV use.
- Split the project into clearer deliverables for long-term maintenance.

## 0.1 Variants

| Variant | ABI | Target devices | Notes |
| --- | --- | --- | --- |
| `java32` | `armeabi-v7a` | Mainstream 32-bit Android TVs / smart screens | Primary TV build |
| `java64` | `arm64-v8a` | 64-bit Android phones / tablets / boxes | Dedicated 64-bit build |
| `hisense` | `armeabi-v7a` | Hisense 32-bit TVs | Dedicated Hisense build |

## Highlights

### 1. Native system-player first

- Standard MP4 / TS / HDR10 / HDR10+ playback prefers the native system player.
- This keeps hardware decoding, native HDR switching, and vendor image processing in the device path whenever possible.

### 2. Dolby Vision routing

- The app probes the stream before playback and selects the playback path up front.
- On devices without native DV decoding:
- If an HDR10 base layer is available, the app prefers that path.
- If not, it falls back to the built-in compatibility player with HDR or SDR fallback depending on device capability.

### 3. MKV / WebM compatibility

- Some TV firmware is unreliable with HTTP HEVC MKV on the native extractor / decoder chain.
- The built-in MPV compatibility path is used to avoid forcing those streams through a path that simply fails.

### 4. Subtitles and audio

- Supports internal subtitles, source subtitles, external subtitles, and local subtitles.
- Automatically prefers Simplified Chinese / Traditional Chinese when available.
- Audio passthrough follows the app setting while the app keeps its own volume at full scale.

### 5. TV-first interaction

- The UI and control flow are tuned for remote navigation, fullscreen control layers, seek behavior, and predictable back handling.
- The visual direction keeps a dark base with liquid-glass style components and a tvOS-inspired UI foundation.

## Playback architecture

```text
Stream probe
  -> identify HDR10 / HDR10+ / Dolby Vision
  -> inspect container and device capability
  -> choose native system player or compatibility player
  -> apply HDR request, subtitle policy, audio passthrough, and fullscreen controls
```

## Repository layout

```text
app/        Main Android application
player/     Player abstraction and native playback logic
quickjs/    JS engine module
pyramid/    Python extension module
```

## Local build

The repository is arranged so build dependencies and caches can stay inside the project runtime directory.

- Android SDK: `E:\apk\tvbox\TVBoxOS-main\_runtime\android-sdk`
- JDK: `E:\apk\tvbox\TVBoxOS-main\_runtime\jdk\temurin11\jdk-11.0.31+11`
- Gradle Home: `E:\apk\tvbox\TVBoxOS-main\_runtime\gradle-home`

PowerShell:

```powershell
$env:JAVA_HOME='E:\apk\tvbox\TVBoxOS-main\_runtime\jdk\temurin11\jdk-11.0.31+11'
$env:GRADLE_USER_HOME='E:\apk\tvbox\TVBoxOS-main\_runtime\gradle-home'

.\gradlew.bat :app:assembleNormalDebug
.\gradlew.bat :app:assembleJava64Debug
.\gradlew.bat :app:assembleHisenseDebug
```

## GitHub Actions

The repository includes a basic Android build workflow for:

- `TVBox_debug-java32.apk`
- `TVBox_debug-java64.apk`
- `TVBox_debug-hisense.apk`

## Notes

- This project does not bundle any media catalog, live playlist, or subscription source.
- Only use content sources, subtitles, and subscriptions that you are legally allowed to use.
- `tvOS`, `iOS`, `Apple TV`, `Dolby Vision`, `HDR10`, and `HDR10+` are trademarks of their respective owners.

## Roadmap

- `0.1`: unify the 32-bit TV build, 64-bit Android build, and Hisense 32-bit build.
- Next: portable Windows build, more device-specific branches, and a proper GitHub Release workflow.
