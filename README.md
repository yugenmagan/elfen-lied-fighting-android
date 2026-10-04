# Elfen Lied Fighting — unofficial Android port

**v1.0.0-rc1 · build 14 · prerelease**

An unofficial Android port of the original fan-made PC game credited to **ねくぱっち**. This project implements an Android-compatible runtime for the supplied game data. It is not a new game authored by the port maintainer, and it is not an official Elfen Lied product. See [CREDITS.md](CREDITS.md) for attribution and rights.

[Русское описание](README_RU.md)

## Install and play

1. Download `elfen-fighting-v1.0.0-rc1.apk` from the release attachments.
2. Allow your browser or file manager to install apps when Android asks.
3. Install and launch **Elfen Lied Fighting**. Android 10 or newer is required.
4. Choose **English**, **日本語**, or **Русский**, then **New Game → Story / Versus** and choose a character.

The APK includes the game data. Single-player works offline, without Windows, root, or another application. English is selected by default on the first language screen; subsequent choices are remembered. The runtime is Java-based and supports ARM64 and x86_64 Android devices without a native-library ABI dependency.

## Controls and pause

The red arcade stick supports diagonals and motions. Red **A**, yellow **B**, and green **C** have small labels underneath. The white button opens Pause. Touch supports multiple fingers; button presses request Android haptic feedback, while the stick does not. Haptic strength and availability depend on the device and system settings.

Controls stay in the side margins, including their touch targets. Wide displays use a shallow row; narrower displays use a compact triangle. The arena retains its original 4:3 proportions. Size, opacity, position, mirror layout, stick dead zone, gamepad mappings, and hiding controls with a gamepad are available in Settings. Positions are constrained to keep the arena clear.

Home or loss of foreground pauses the session. On return, use **Continue** on the title screen. Continue only resumes a session still in memory. Closing the Activity or terminating the process ends the session; language and control preferences remain saved. Pause → Moves/Settings → Back → Continue returns to the same fight.

## What this candidate contains

Original title and character-selection artwork, combat HUD and portraits, local Story and Versus, EN/RU/JA text selection, an Android launcher icon based on the original Lucy portrait, deterministic combat snapshots/replay, and local rollback tests. Losing to the final boss leads to Continue rather than the ending.

This is **not a claim of complete Windows-runtime equivalence or final-release acceptance**. An experimental online client and room-server source are included, but no public server is deployed and a real internet match is not validated. See [KNOWN_ISSUES.md](KNOWN_ISSUES.md) and [TEST_RESULTS_RC1A.md](TEST_RESULTS_RC1A.md).

## Preview

The image below is a desktop render of the production control drawing code over the original game HUD; it is not an Android screenshot. Actual Android 10 emulator captures are in [docs/android-smoke](docs/android-smoke); the running fight is shown below.

![Controls outside the original 4:3 HUD](docs/controls-rc1a/controls-wide.png)

![Android 10 emulator: running fight with unobstructed HUD](docs/android-smoke/battle.png)

## Project

- [Build instructions](BUILDING.md)
- [Changes](CHANGELOG.md)
- [Porting notes](PORTING_NOTES.md) and [format notes](FORMAT_NOTES.md)
- [Credits and rights](CREDITS.md)

The original assets remain the property of their respective rights holders. No blanket license is granted for them by this repository.
