# Known issues — v1.0.0-rc1

This is a release candidate. The game is playable, but some parts still need broader testing.

- Gamepad mappings are included, but physical gamepad support has not been extensively tested across different controllers.
- Exact frame-by-frame behaviour may differ slightly from the original Windows version in some edge cases, especially wall behaviour and a few visual effects.
- Some visual effects from the original runtime are not fully reproduced yet.
- In rare mirror matches, particularly Nana vs. Nana, the AI may stop outside attack range.
- A few characters whose original data do not contain a combat portrait still have no portrait in the port.
- On narrow screens, the game arena may be scaled down slightly to leave room for touch controls.
- Continue only restores the current session while the app remains in memory. If Android terminates the process, the current fight cannot be resumed.

If you find a crash, softlock, broken story route, incorrect attack, missing asset, or behaviour that clearly differs from the original PC game, please open a GitHub issue.

When reporting a bug, please include:
- Android device and version
- character / opponent
- Story or Versus mode
- what happened
- what you expected to happen
- screenshots or video if possible
