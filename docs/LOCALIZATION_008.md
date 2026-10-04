# TEST008: final victory and presentation localisation

Base: Elfen_Lied_Android_007d_project.zip, SHA256 68fde066f6d8f9f7b638699b2ba9abc13082c2ded6dda4f1e2fadf0b64afdc06. The checkpoint was materialized and CRC-verified before extraction. No combat code was reconstructed or replaced.

## Final battle

All ten selectable story programs end their fight sequence at slot18 (enemy reference11, Kraft). The original record's `value(5)` is zero, so the existing StoryRound permits advance after a defeat. TEST008 intentionally requires a won final match, as requested by the user. This is an explicit progression policy, not a change to damage, HP, AI or round timing.

`StoryProgram.finalFightSlot()` locates the last nonempty fight record in the audited linear programs. `StoryController(..., classicRounds, requireFinalWin)` enforces victory only at that event. A completed final match with `wins1 <= wins2` offers the original Continue scene and retains the event slot. Retry reconstructs a fresh match at the same slot. A final win follows the original ending/credits events. Regular fights retain their existing flags.

The new identity suffix is `:final-victory-v1`; legacy constructors retain old replay behaviour. `loadOfflineSave` validates the old snapshot before explicitly migrating its identity. A legacy ending after a final defeat moves back to the boss Continue; an earned ending and a current battle's state/RNG are retained. Old replay files are not silently relabelled. Snapshot header/version and combat ruleset revision2 remain unchanged.

## Text sources

`localization/scenes_source.tsv`: manual Japanese transcription and direct English/Russian translations of 90 source images. Includes story dialogue, scene titles, endings, credits, opening/title images, Continue, KGT battle announcements and ten character-select name images. There are 95 rendering regions because a few captions share a source image. All 27 DEMO packs were inspected; 0064/0226 do not contain readable dialogue. The source hash/dimensions/mode for each translated image are in `localization/source_manifest.json`.

`localization/ui.json`: 354 keys in JP/EN/RU, including menu/settings/status text, names of all46 packs and all156 distinct nonempty command names. The original command bytecode is not changed. Some author-specific move names are transliterated instead of guessing a new technique. Unusual original strength-label typos are rendered as the corresponding light/medium/heavy label; no command bytes or move strength changes result from this.

Japanese proper names are read from the supplied data. Russian uses Люси, Ню, Нана, Марико, Маю. Guest character handles and author credits are transliterated; original text and URLs are retained in the source catalogue. The unusual opening phrase on 0072/image12 literally reads 手紙 (letter); it was not rewritten as an assumed “promise”. Tiny decorative pseudo-text in title artwork, stage shop signs, voice recordings and original branding are not translated dialogue.

`tools/build_localization.py` compiles the reviewed source to `assets/localization/ui.tsv` and `scenes.tsv`. Generated files are checked in, so Android Studio does not require Python. The generator is needed when changing translations; tests verify it reproduces the packaged data exactly.

## Presentation

`LocaleCatalog` is outside the battle engine. The saved preference is `controls/language`; it is not part of battle state, RNG, replay or network identity. LanguagePicker uses native text and drawn flags (Japan/US/Russia), independent of flag-emoji availability. It appears before character selection and is accessible from control settings. Existing PauseGate blocks simulation while this dialog is open.

Japanese mode returns original bitmaps. EN/RU bitmaps retain exact original dimensions, sprite references, anchors, VM instructions and timing. `TextFit` shrinks/wraps text inside audited regions, never silently dropping a line. Long words and URLs shrink as a unit. A few original dialogue sprites extend past the frame; their translation regions stop inside the frame.

Replace mode draws text on transparency. Overlay mode keeps the original portrait and covers only the original caption area; flat panels are used when lettering overlaps artwork. A data-driven mask preserves the yellow surprise mark beside the caption in 0060/image2. Title cards use the existing clean template 0064/image2. No generated art, external portraits or modified original PNGs are included.

The Continue background retains its headline. MainActivity masks the old baked choices below it before drawing the active translated menu, avoiding duplicated choices. Japanese source PNGs remain unchanged. Bitmap/tint caches include the selected language and are cleared when it changes. Resource/translation errors are explicit exceptions, not empty placeholders.

## Verification limits

The production LocalizedSprites method was executed with a narrow Java2D adapter for Android graphics calls. 180 EN/RU source-image previews and 186 full-scene previews were generated from the original DEMO VM. These are desktop previews, not Android screenshots; Java2D font metrics differ from Android. All190 translated regions were fitted with the normal metrics and an extra12% width stress margin. 27 DEMO twins ran56700 ticks with language changes and identical hashes.

581 forced-outcome checks cover final loss/retry/repeated loss/quit/win/ending/credits/snapshot/replay/save migration in all10 routes. A separate real-combat test starts at the boss event and lets idle Lucy lose0:2 in17242 ticks, producing Continue without HP/AI/damage/round overrides. A second input-only pilot naturally wins with Young Lucy0156, then reaches ending/credits/COMPLETE. Three replay/restore runs match. Ordinary CPU pilots failed the bounded attempts recorded in final_boss_cpu_attempts_008.log; that did not prove an invincible boss. Both passing natural fixtures start at the final event and are not complete route playthroughs. No new full-route playthrough is claimed for TEST008.

The APK is built and packaging/signature/assets checked. This workspace has no adb, emulator or KVM. Physical installation, native-dialog rendering, text metrics and language changes through Android lifecycle remain device checks.
