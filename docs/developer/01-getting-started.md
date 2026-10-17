# 1. Getting started

## Toolchain

| Tool | Version | Notes |
| --- | --- | --- |
| Android Studio | current stable | Its bundled JetBrains Runtime (JBR, JDK 25) is the JDK Gradle runs on |
| Gradle | 9.8.1 | Through the wrapper (`./gradlew`); never a system Gradle |
| Android SDK | API 37 platform | `compileSdk` and `targetSdk` are 37; `minSdk` is 34 (Android 14) |
| Emulator | API 37 image | Any phone profile; development checks run on an emulator |

Everything else (Kotlin, AGP, libraries) is pinned in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml) and downloaded by Gradle.

### Command-line builds

Android Studio sets the JDK for you. For a terminal, point `JAVA_HOME` at Android Studio's JBR first:

```bash
# Git Bash on Windows
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
```

## Everyday commands

| Command | What it does |
| --- | --- |
| `./gradlew assembleDebug` | Builds the debug APK |
| `./gradlew installDebug` | Builds the debug APK and installs it on the running emulator or device |
| `./gradlew check` | Runs everything CI would: unit tests (including Robolectric and Konsist), detekt, ktlint and Android Lint |
| `./gradlew buildHealth` | Dependency Analysis: fails on misdeclared dependencies (e.g. `implementation` that should be `api`) |
| `./gradlew assembleRelease` | R8-shrunk release APK (signed only if this machine has the release key; see below) |
| `./gradlew ktlintFormat` | Fixes most formatting findings automatically |
| `./gradlew :core:data:testDebugUnitTest` | Runs one Android module's unit tests (pure-Kotlin modules use `:core:domain:test`) |
| `./gradlew :core:data:testDebugUnitTest --tests '*StampedWriterTest'` | Runs one test class |

Before a merge to `master`, the full set (`check buildHealth assembleDebug assembleRelease`) must pass.

Test and lint reports are written to `<module>/build/reports/`. Raw test results (XML) are in `<module>/build/test-results/`.

## Two apps on one device

The build produces two separately installable apps:

| Variant | Application ID | Launcher label | Data |
| --- | --- | --- | --- |
| debug | `com.jrussell.fitness.debug` | GetFit (Debug) | Its own database and preferences |
| release | `com.jrussell.fitness` | GetFit | The real data |

The debug app can be wiped freely (`adb shell pm clear com.jrussell.fitness.debug`) without touching the release app's data. This is set up in [`AndroidApplicationConventionPlugin`](../../build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt).

## Release signing

Release builds are signed with a permanent key kept **outside the repository**. Four Gradle properties point at it, and they live in the developer's own `~/.gradle/gradle.properties`:

```properties
getfit.release.storeFile=<absolute path to the .jks>
getfit.release.storePassword=...
getfit.release.keyAlias=...
getfit.release.keyPassword=...
```

If any of the four is missing (a fresh clone, or CI), release builds are still produced, just unsigned, so R8 keeps being exercised. See [`ReleaseSigning.kt`](../../build-logic/convention/src/main/kotlin/com/jrussell/fitness/buildlogic/ReleaseSigning.kt).

**Never lose the key.** Android installs an update only over an app signed with the same key. Losing it means uninstalling the app, which deletes all of its data.

## Looking inside the running app

- **Database:** in Android Studio, use **App Inspection → Database Inspector** on the debug app to browse `user.db` live (the `UserProfile`, `Measurement` and `SyncSequence` tables, and so on). The emulator has no `sqlite3` binary, so this is the easiest way in.
- **Files:** `adb shell run-as com.jrussell.fitness.debug ls databases no_backup files`. This shows:
  - `databases/user.db`, the user database;
  - `no_backup/device_id`, the per-install ID;
  - `files/crash-logs/`, written if the app ever crashes.
- **Crashes:** every uncaught exception is written to `files/crash-logs/crash-<epoch ms>.txt` before the process dies ([`CrashLogHandler`](../../core/data/src/main/kotlin/com/jrussell/fitness/core/data/crash/CrashLogHandler.kt)). A viewer in Settings is planned.

## Windows notes

- In Git Bash, prefix `adb shell` commands that contain device paths with `MSYS_NO_PATHCONV=1`. Otherwise Git Bash rewrites `/data/...` into a Windows path.
- If a build fails with "The process cannot access the file because it is being used by another process", a leftover Gradle daemon is holding a jar. Run `./gradlew --stop` and build again.

## Robolectric

Android modules run their unit tests on the JVM with Robolectric, with no device needed. Two details are already handled, but it helps to know about them:

- Each module with Robolectric tests has `src/test/resources/robolectric.properties` containing `sdk=36`. Robolectric 4.17 emulates up to API 36, while the app compiles against 37.
- Emulating API 36 needs **Java 21 or newer**, so Gradle must run on JDK 21+ (Android Studio's JBR 25 locally, Temurin 25 in CI). On JDK 17 every Robolectric test fails with "Android SDK 36 requires Java 21".
- Every Android module's tests get the JVM flag `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`, which Robolectric needs on JDK 25. It is set in [`AndroidCommon.kt`](../../build-logic/convention/src/main/kotlin/com/jrussell/fitness/buildlogic/AndroidCommon.kt).

Next: [Architecture](02-architecture.md).
