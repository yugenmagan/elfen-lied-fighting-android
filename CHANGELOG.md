# Changelog

## v1.0.0-rc1 — build 14 — 2026-10-04

- Moved touch controls and their hit targets into side margins, keeping the 4:3 arena and HUD clear.
- Added small A/B/C labels below the matching red/yellow/green buttons.
- Added adaptive row/triangle placement for different screen shapes; kept mirror and size preferences with safe position bounds.
- Preserved button-only haptics, multi-pointer input, all combat/runtime data, AI, audio and lifecycle logic from private RC1.
- Removed embedded signing configuration from build recipes; release signing now uses private environment variables.
- Added layout/render/touch regression checks and public-release documentation.

Inherited private RC1 behavior: original title screen after language selection, English initially selected, no TEST/debug overlay or saved-story popup, New Game/Continue, pause on background, in-memory continuation only, and corrected pause-menu navigation.

Inherited earlier checkpoints: original selection/HUD, EN/RU/JA text, final-boss victory requirement, deterministic combat/replays, and experimental local rollback/room-client work. This candidate does not imply completion of all original-port or online acceptance criteria.
