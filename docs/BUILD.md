# RC1 reproducible build

JDK17; Gradle8.9; AGP8.7.3; Android platform35 / build-tools35.0.0. Android Studio opens this project root.

```sh
./gradlew assembleRelease
```

Equivalent standalone route (downloads pinned official SDK components if needed):

```sh
python3 tools/build_android.py
python3 tools/check_apk.py
```

Output: `builds/elfen-rc1.apk`; Gradle output: `app/build/outputs/apk/release/app-release.apk`. The retained private-test signing certificate permits updating the accepted TEST008 installation. Source/compiled resources live in the project; no Windows directory or manual conversion is needed. The main APK contains all offline game data.

`python3 tools/package_checkpoint.py` creates and CRC-checks the full source ZIP, extracts into a separate folder, builds from those extracted files and compares the APK hash. `.deps`, Gradle caches and local.properties are not included. Do not run `tools/apply_rc1_patch.py` on RC1; it records the one-time changes from TEST008.
