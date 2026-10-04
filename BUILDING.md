# Building

This document describes how to build **Elfen Lied Fighting for Android** from the current source tree.

## Requirements

- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0

The project currently uses:

- Gradle 8.9
- Android Gradle Plugin 8.7.3
- minSdk 29
- targetSdk 35
- compileSdk 35
- Java 8 source/target compatibility

No Android NDK is required.

The runtime and networking code are shared Java sources compiled directly into the Android application.

## Build with Gradle

Gradle is the recommended build method for normal development.

From the repository root:

```sh
./gradlew assembleDebug
```

The debug APK will be created under:

```text
app/build/outputs/apk/debug/
```

To build the release variant:

```sh
./gradlew assembleRelease
```

Release output will be created under:

```text
app/build/outputs/apk/release/
```

Without the maintainer's signing credentials, the release APK is unsigned.

## Android Studio

The repository can also be opened directly in Android Studio.

Use JDK 17 and make sure the following Android SDK components are installed:

- Android SDK Platform 35
- Android Build Tools 35.0.0

Android Studio can then use the included Gradle wrapper to build the project normally.

## Release signing

Official release signing credentials are not stored in the repository or Git history.

Local release signing can be configured with the following environment variables:

```sh
export ELFEN_KEYSTORE=/path/to/keystore
export ELFEN_KEY_ALIAS=your_alias
export ELFEN_STORE_PASSWORD=your_store_password
export ELFEN_KEY_PASSWORD=your_key_password

./gradlew assembleRelease
```

Do not commit keystores, passwords or signing configuration containing private credentials.

A build signed with a different key cannot be installed as an update over the official release without first uninstalling the existing application.

## Standalone build tool

A separate release-oriented build script is also included:

```sh
python3 tools/build_android.py
```

This build path uses Python 3 and JDK 17 and can bootstrap the required official Android SDK components automatically.

Set `ANDROID_SDK_ROOT` if you want the script to reuse an existing Android SDK installation.

With release signing configured, the output is:

```text
builds/elfen-fighting-v1.0.0-rc1.apk
```

Without signing credentials, the output is explicitly marked as unsigned:

```text
builds/elfen-fighting-v1.0.0-rc1-unsigned.apk
```

The standalone build path is mainly used for release and reproducibility checks. Gradle remains the recommended method for ordinary development.

## Game data

Converted game data required by the Android runtime is already included in the repository.

The original Windows executable is not required to build or run the Android application.

The conversion pipeline can be invoked separately when working with the original source data, but normal Android builds do not require reconversion.

## Testing

The repository includes regression tests for areas such as:

- combat and damage;
- movement and command input;
- AI behaviour;
- round and match flow;
- replay and deterministic state;
- rollback simulation;
- Story progression;
- localization;
- touch controls;
- lifecycle and release flow.

Individual test and verification scripts are located under:

```text
tools/
```

Current public verification results are documented in:

[TEST_RESULTS_RC1A.md](TEST_RESULTS_RC1A.md)

Some research and reverse-engineering tools require additional dependencies or original Windows files. They are not required to build or run the Android application.

## APK verification

The repository includes tools for checking APK metadata, resources, alignment and signing.

Note that checks tied specifically to the official release certificate are expected to fail for independently signed builds.

This does not mean that an independently signed build is otherwise invalid.

## Android smoke testing

The repository also includes:

```text
tools/android_smoke_host.py
```

This can provision an Android 10 / API 29 x86_64 emulator environment for automated smoke testing.

Emulator testing is useful for installation, startup, navigation and basic gameplay checks, but it is not a substitute for testing latency, audio, haptics, controller behaviour and performance on physical Android devices.

## Continuous integration

GitHub Actions automatically builds the project on pushes and pull requests to `main`.

The workflow builds:

- the debug APK;
- the unsigned release APK.

The CI configuration is located at:

```text
.github/workflows/build.yml
```

The private release signing key is not available to GitHub Actions.

## Release builds

The current public release candidate is:

**v1.0.0-rc1 · build 14**

Official APKs are published through GitHub Releases rather than committed directly to the repository.

See the project README for the current download link.
