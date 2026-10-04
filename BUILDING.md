# Building v1.0.0-rc1

Use JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. The wrapper pins Gradle 8.9 (with distribution SHA-256); the build pins Android Gradle Plugin 8.7.3. Minimum Android API is 29; target and compile API are 35. No NDK is required. Runtime and network sources are shared Java modules compiled into the application.

Open this directory in Android Studio, let it resolve the pinned components, or run:

```sh
./gradlew assembleDebug
./gradlew assembleRelease
```

Debug produces an installable APK with your local Android debug key. Release is unsigned unless all four environment variables below are provided. The private release key is deliberately absent from the repository, ZIP and Git history.

```sh
export ELFEN_KEYSTORE=/private/path/release.p12
export ELFEN_KEY_ALIAS=your_alias
# Supply ELFEN_STORE_PASSWORD and ELFEN_KEY_PASSWORD privately in your
# local environment or CI secret store; do not commit their values.
./gradlew assembleRelease
```

Gradle outputs are under `app/build/outputs/apk/`. The public candidate uses the existing private checkpoint signing identity so it can update previous test builds. A build made with your own key cannot update those APKs without removing the old application.

## Standalone SDK build

The same source tree can also be built on Linux with Python 3 and JDK 17:

```sh
python3 tools/build_android.py
```

This uses the pinned official SDK packages from `tools/bootstrap_android.py`, downloads missing packages, compiles the application and shared runtime, and packages all game data. Set `ANDROID_SDK_ROOT` to reuse an installed SDK. With the signing environment configured, output is `builds/elfen-fighting-v1.0.0-rc1.apk`; otherwise output is explicitly marked `-unsigned.apk`.

The distributed APK is made with this standalone recipe. Rebuilding an unpacked checkpoint with the same SDK, JDK and private signing identity is checked for byte-for-byte equality. Gradle packages the same source/assets but need not produce an identical ZIP hash.

## Verification

```sh
python3 tools/test_controls_rc1a.py
python3 tools/check_touch_feedback_007c.py
python3 tools/test_release_flow.py
python3 tools/test_combat.py
python3 tools/test_match.py
python3 tools/test_localization_008.py
python3 tools/check_apk.py
```

The desktop-render tests use Java2D and Python Pillow; they are not Android execution. `check_apk.py` verifies the official candidate's package, version, assets, alignment and retained signing certificate; an independently signed build will intentionally fail the certificate assertion. Historical reverse-engineering tools may additionally require Capstone/Unicorn and the user's original private Windows files. Those tools are not needed to build or run the Android application.

`tools/android_smoke_host.py` can provision official Android 10 x86_64 emulator components for headless smoke checks. It uses software emulation when KVM is unavailable and does not measure real-device latency or sound. Real-device haptics, controller compatibility, audio focus, and performance still require hardware checks.
