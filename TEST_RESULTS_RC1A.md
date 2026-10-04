# Test results — v1.0.0-rc1

Version: **1.0.0-rc1**  
Build: **14**  
Date: **2026-10-04**

This is a release candidate, not a claim of perfect equivalence with the original Windows version.

## Build

- Android project builds successfully with Gradle.
- Debug and release builds complete successfully.
- The published APK installs and launches correctly.
- Package, SDK, resources, alignment and signing checks pass.

Published APK:

`elfen-fighting-v1.0.0-rc1.apk`

SHA-256:

`5b770f932b1bec3dcda4cb7a0e68e1ce730430e91d946b63059a120ddb45c110`

## Automated tests

Automated tests currently cover:

- combat and damage;
- movement and special-move input;
- AI behaviour;
- round and match flow;
- KO, timeout and win/loss states;
- replay determinism;
- snapshot restore and rollback simulation;
- story progression and endings;
- localization;
- pause / resume flow;
- touch and multitouch input.

All current automated test suites pass.

These tests are intended to detect regressions in the Android port. They do not prove perfect frame-by-frame equivalence with the original Windows runtime.

## Android smoke test

The release APK was also tested in an Android 10 emulator.

Verified:

- installation and cold launch;
- language selection;
- title screen;
- character selection;
- Versus battle;
- AI attacks and special moves;
- Pause / Continue;
- returning from Home;
- restoring an in-memory fight;
- clean restart after force-stop.

No crash was observed during the final smoke test.

## Still needing broader real-device testing

The following areas still benefit from testing on different physical Android devices:

- touch comfort and latency;
- haptic feedback;
- audio behaviour;
- physical gamepads;
- unusual screen sizes and aspect ratios;

Please report reproducible problems through GitHub Issues.
