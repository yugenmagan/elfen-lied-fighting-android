# RC1 build 14 — verification record

Date: 2026-10-04. Version: `1.0.0-rc1`, versionCode `14`. This is a prerelease; it does not close all original-port acceptance gates.

## Build and artifact identity

- Standalone SDK build: compiled, aligned and signed successfully.
- Gradle 8.9 / AGP 8.7.3: `assembleRelease assembleDebug` passed; 74 tasks executed.
- All three APKs passed package, SDK, resource, CRC, alignment and signing checks. The delivered APK is the standalone build; Gradle ZIP packaging produces a different hash.
- Delivered APK: `elfen-fighting-v1.0.0-rc1.apk`, 68,712,947 bytes.
- SHA-256: `5b770f932b1bec3dcda4cb7a0e68e1ce730430e91d946b63059a120ddb45c110`.
- Retained signing certificate SHA-256: `12f2989f0ab609532bdf881cfcb54093343a5707874af00a0ee8f47ce460d086`. The key and passwords are excluded from the public source package.
- `SOURCE_INPUTS.json` records 2,959 application/source/asset/build inputs. `tools/check_release_inputs.py` passed for the delivered APK.
- Separate ZIP extraction/rebuild evidence is supplied in `RC1A_CHECKPOINT_VERIFICATION.json` alongside the checkpoint.

## UI and input regression

- 240 geometry variants: eight screen sizes, three scales, mirrored/unmirrored, and five preference combinations. 9,169 assertions passed.
- The full stick and all six 1.12-radius attack touch targets remain outside the arena. Pairwise attack hit targets do not overlap. Captions and all controls remain on screen. The arena remains 4:3.
- Four previews execute the actual control drawing methods through a Java2D adapter. Every arena pixel remains untouched by control drawing; A/B/C centre colours and corresponding captions are checked. These previews are explicitly not Android screenshots.
- 276 real touch-handler events across six configurations passed. Checks include direction held with simultaneous A+B, release/cancel, mirror, and button-only haptic requests. The callbacks and Input samples match the prior handler.
- Android hardware vibration strength, physical touch latency and comfort are not established by these JVM tests.

## Preserved logic and deterministic tests

- Byte comparison against the retained private RC1 checkpoint: all 37 runtime source files, four network source files, 2,884 assets and 14 resource files unchanged. The actual `onTouchEvent` method is identical. Changes to MainActivity are limited to five layout/drawing methods and a geometry field. No audio/lifecycle/combat/AI code changes.
- Lifecycle/navigation: 749 assertions, 40 pause/moves/settings cycles, eight Home/pending-dismiss orderings. Actual production Activity methods are exercised with queued-dialog fakes and real game snapshots. Not an Android lifecycle substitute.
- Combat: 32,582 deterministic damage/collision/restore assertions; original CPU fixtures 1,481,760 assertions; 1,068 original command cases with 1,094,700 assertions; contact/reaction-error fixtures passed. All CPU-character fixtures load and run.
- Replay: three runs of 2,400 frames with identical every-frame hashes; history restore/resimulation and corruption rejection passed.
- Match: 68,663 assertions; a 21,614-frame match replay ends with the same hash and 2:1 score. KO, round/timer branches, movement and special input sequences passed.
- Rollback: independent 3,000-frame twins and artificial delays of 50/100/150/200 ms converge to identical state hashes. This is local simulation, not an internet match.
- Localization/demo: 66,343 checks, 367 UI keys in three languages, 46 packs, 759 command-name lookups, 27 DEMO twins across 56,700 ticks, and translated-region checks passed.
- Progression: 581 checks across ten routes for defeat/retry/repeated defeat/quit/victory/ending/credits. Natural input-only boss-event fixtures: Lucy loses 0:2 and reaches Continue; Young Lucy wins 2:0 and reaches ending/credits. The 22,734-frame winning recording repeats/restores with hash `ad0ae7d3c66961dcdf56ad9487cbd1a956b8caa6d3cb0fcb2a477a418761e1dc`.
- Those boss tests enter at a boss-event fixture. They are not a fresh full-route Android playthrough. Historical route completions cannot be relabelled as new full playthroughs under the stricter boss policy.

## Android smoke

Official Android 10 / API 29 x86_64 image, software CPU emulation, 1280×720. No KVM, audio disabled. Installation returned Success. First startup displayed the language screen with English checked, the title and New Game/Versus, original Lucy/Nana selection, a running fight and Pause. During the first emulator graphics configuration two framebuffer captures were corrupted; a repeat selection and paused fight rendered correctly, and the emulator logged a framebuffer-handle error. A graphics-backend recheck is recorded separately below; this observation is not hidden or treated as a game fix.

Final smoke continuation, same signed APK: cold launch returned Status: ok; Versus selected Lucy/Nana and loaded a fight; the AI executed a special and spent meter. Native Pause appeared, Continue closed it and returned to the running fight. Home returned to the title with Continue enabled, and Continue restored the in-memory fight. Screenshots in docs/android-smoke are actual emulator captures. Attack buttons, their A/B/C captions and the stick remain outside the HUD in the captured fight. After force-stop, restart returned Status: ok (COLD) and Continue was disabled. The Android crash buffer was empty. Details are recorded in research/android_smoke_rc1a.json.

One initial capture using swiftshader_indirect with Vulkan disabled was tiled incorrectly; subsequent title, fight, Pause and Home captures were correct. This is a capture/graphics-environment limitation under investigation, not a change to game code or proof of universal device compatibility. Startup and observed menu/battle/lifecycle smoke checks passed; real-device audio, haptics, timing and comfort remain unverified in this iteration. Physical-phone audio, haptics, latency, gamepad compatibility and a complete current-version story run remain outside this environment's verification.

## Release status

Candidate only. See KNOWN_ISSUES.md for inherited runtime gaps, missing public online server and unverified real internet play. The GitHub package omits private signing material. A local tag does not imply that a GitHub Release has been created.
