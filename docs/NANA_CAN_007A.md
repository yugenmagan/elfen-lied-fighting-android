# Nana money can audit, TEST 007a (2026-10-01)

Source: unchanged EFP pack0104 (`ナナ裏`), derived from the original PLAYER. Identified visually using image0141.png: the can containing burning notes. Instructions remain byte-for-byte unchanged.

## Original instruction map

| Command | Owner skill / OO PC | Object | Position relative to Nana | Motion |
|---|---|---|---|---|
| のろい弱, QCB+A | 92 /1018 |91 `技名9` |+70,0 |No M: stationary |
| 呪い中, QCB+B |97 /1071 |94 `官中` |+60,0 |PC1041 M: vx50×655=32750 (16.16), about0.4997px/tick at speed10 |
| 呪い大, QCB+C |96 /1063 |95 `缶大` |+260,-500 |PC1052 M: ay250×393=98250; ON1→91 on landing |

Offsets mirror with facing. Strong skill95 has no FA; only grounded91 enables direct contact. Bank FA at818/1038: centre(+2,-35), half extents(28,41), flags0, base power5. R at817/1037: hit4/5/6, guard10/11/12. No chip flag. ON2 guard at815/1035→93, not an on-hit transition. This is why the directly hitting can can continue moving after its first contact, with bit16 preventing another contact.

Skill93 `のろいくん` spawns four skill90 children at relative X -70,-40,+70,+40; PC1025 sets vx100×655=65500. Children skill90 use original images134..138, FA PC794 with base power3, flag2 rearm and R4/5/6 for both hit and guard. Their damage remains separate from the bank's direct blocked contact.

## Native evidence

`tools/probe_nana_can_x86.py` executes supplied0186.exe in Unicorn, without Windows APIs or patched function bodies:

- M handler0x41282d→0x4125ae and integration0x40f96a→0x40f9dc:960 frames from four actual M instructions and both facings.
- Full FA/FD procedure0x40f010:60 cases using original Nana bank/flame FA and R, task kind1, two facings, guard/no guard and five distances. Synthetic FD isolates contact rather than claiming a complete Windows replay.
- Each contact is repeated without rearm. Ordinary bank contact:400→395; bank guard:400→400, attacker pending93; flame contact:400→397. Non-overlap:400. No second damage from the repeated call.

Shipped evidence: `research/nana_can_x86_007a.json`; Java differential fixtures and command scenarios: `tests/NanaCanTest.java`. `probe_combat_x86.py` now reads the identical junctions from shipped data.efp instead of depending on an absent generated research JSON. This changes the research tool's data source, not the Android game.

## Port verification and decision

Real numbered inputs trigger all three commands against Lucy from both sides. First direct hit has5HP damage; strong can falls and then becomes91. Guard test waits for contact before holding back; holding back from frame0 simply walks Lucy out of range during startup and is not a guard test. The resulting ON2→93→90 chain is verified.

Six scenarios each restore a snapshot with the can alive and replay739 frames twice, comparing every hash. Combat sources and original assets match final TEST007 byte-for-byte. No speculative friction, added damage, new hitboxes or alternative projectile behavior was introduced. Remaining whole-runtime fidelity limits still apply.
