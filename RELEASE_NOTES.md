# Elfen Lied Fighting v1.0.0-rc1

**Prerelease · versionCode 14 · Android 10+**

Unofficial Android port of the original fan-made PC game credited to **ねくぱっち**. Elfen Lied and the original assets belong to their respective rights holders. This is not a new game authored by the port maintainer.

## Changes

- Touch buttons and stick stay outside the arena and original HUD.
- Red A, yellow B and green C have small labels underneath; narrow screens use a compact triangle.
- Retained multitouch and button-only haptics, original HUD/title/selection, language choice, pause and memory-only Continue.
- No combat, AI, damage, asset or audio changes in this UI update.

Install `elfen-fighting-v1.0.0-rc1.apk`. All game data are included; offline play needs no Windows or companion app. The APK keeps the earlier signing identity. `SHA256SUMS.txt` identifies the exact release file.

## Verification and limits

See `TEST_RESULTS_RC1A.md` for the actual build, Android smoke and automated results. The source checkpoint is rebuilt separately and compared with the attached APK. The new touch layout still needs a comfort/haptic/latency check on a physical phone.

This remains RC1: a public room server and real internet battle are unverified, complete frame-by-frame Windows fidelity is not claimed, and some inherited runtime limitations remain. See `KNOWN_ISSUES.md`. No server is deployed as part of this release preparation.

## Что изменилось

Стик и кнопки вынесены за пределы арены и HUD. Под красной, жёлтой и зелёной кнопками добавлены A, B и C. На узком экране кнопки располагаются треугольником. Боевая логика, AI, урон, ресурсы, звук и пауза этой правкой не менялись. Это предварительная сборка RC1; подробности — `README_RU.md` и отчёт проверок.
