# Changelog

## v1.0.0-rc1 — 2026-10-04

First public release candidate of the Android port of **Elfen Lied Fighting**.

### Added

- Standalone Android version with no Windows, Wine or Winlator requirement.
- Story and Versus modes.
- Touch and multitouch controls.
- English, Japanese and Russian text options.
- Original character selection screen, combat HUD, portraits, stages, music and story content.
- Pause menu and in-memory Continue.
- Configurable touch-control size, opacity, position and mirrored layout.
- Configurable gamepad mappings.
- Android lifecycle handling for pause, Home and application focus changes.

### Changed

- Adapted the original 4:3 game presentation for modern Android screens.
- Moved touch controls into the side margins where screen space allows, keeping the combat arena and HUD unobstructed.
- Added small A / B / C labels to the attack controls.
- Added adaptive control placement for different screen shapes and aspect ratios.
- Story progression now requires defeating the final boss before proceeding to the ending.

### Fixed

- Corrected pause-menu navigation and resume behaviour.
- Fixed touch handling so simultaneous directions and attack buttons remain independent.
- Fixed several combat, round-flow and Story progression issues found during port testing.
- Removed embedded release-signing configuration from the public project; signing credentials are now supplied externally.

### Testing

- Lucy's Story mode has been completed manually from beginning to end on Android.
- Other Story routes have automated progression coverage but have not all been completed manually.
- Automated tests cover combat, match flow, input, replay, rollback simulation, localization and Story progression.
- The release APK has passed Android emulator smoke testing.

### Known limitations

- This is a release candidate and does not claim perfect frame-by-frame equivalence with the original Windows runtime.
- Online play is not publicly available yet.
- Physical gamepads and device-specific audio, haptics and latency still benefit from broader real-device testing.

See [KNOWN_ISSUES.md](KNOWN_ISSUES.md) for the current list of known limitations.
