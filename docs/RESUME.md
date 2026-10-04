# Current continuation point — 2026-10-04

Use **RC1 build 14**, never the stale TEST008 scratch tree. Current production layout is CombatLayout.java plus five MainActivity drawing/layout methods. The APK is elfen-fighting-v1.0.0-rc1.apk, versionCode 14. SOURCE_INPUTS.json records its source and asset inputs. Combat/runtime/network/audio/lifecycle are unchanged from private RC1; 37 runtime files, 4 network files and all 2884 assets matched the private baseline.

Public source/APK publication was explicitly authorized by the user. No room-server deployment is implied. Do not label this 1.0.0: original-runtime fidelity and real internet-play gates remain open.

No signing material is included in this source package. The old private RC1 checkpoint retains the upgrade signing identity; obtain it only from the user's private checkpoint when authorized. Build recipes take external ELFEN_* environment values. Do not put those values, keys, or machine-local configuration into GitHub.

Current results are in TEST_RESULTS_RC1A.md, KNOWN_ISSUES.md, STATUS.json and release documentation. Android screenshots are separately labelled from desktop Java2D previews. A successful JVM test is not a physical-phone test. Use the prepared GitHub package, local commit and tag if publication is blocked; do not recreate history unnecessarily.

The current GitHub connector cannot create a repository or a Release or upload Release attachments; owner-list retrieval returned no accessible repository. Finish and preserve the ready Git bundle/attachments before asking for browser-fallback approval. The user already authorized public source/APK publication; this is an environment capability block, not a new publication-permission request.
