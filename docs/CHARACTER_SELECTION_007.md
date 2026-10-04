# Original character selection — TEST007

Source: final TEST006 ZIP SHA256 `22effcd41686ec393d1966a2687d5b24ba265dbb0b4170b13404a381b38a3ef0`, original KGT0116, CP932-validated `story-index.tsv`, and original `source_original/entries/0186.exe`. No original asset was edited.

KGT tail is mapped at original VA435470. StoryCatalog.screen(1) (story) and screen(2) (single VS) both resolve DEMO#8, pack0064, `キャラセレ.demo`. The original background already contains ten small portraits and their frames.

| Original VA | Type | Value / meaning |
|---|---|---|
| 445250,445252 | u16 each | idle cursor skills84/85 |
| 445254,445256 | u16 each | confirm cursor skills86/87 |
| 4452b0,4452b2 | s16 each | grid origin218,50 |
| 4452b4,4452b6 | s16 each | spacing150,83 |
| 4452b8,4452ba | s16 each | 2 columns,5 rows |
| 4452bc,4452be | s16 each | P1 anchor70,350 |
| 4452c4,4452c6 | s16 each | P2 anchor560,350 |
| 4452cc | 50 bytes | availability bit1 story,bit2 VS |

| Row | Left pack / original name | Right pack / original name |
|---|---|---|
| 1 | 0122 にゅう | 0128 マリコ |
| 2 | 0114 アンナ | 0110 マユ |
| 3 | 0156 るーしー | 0108 ダイアナ |
| 4 | 0170 ルーシー | 0168 ミサキ |
| 5 | 0136 ロストナンバー | 0104 ナナ裏 |

PLAYER builtin[21] is the portrait script. Original CO/I/OO also create animated child objects and name art, executed by the existing Script VM. P2 inherits facingLeft. Root depth80, child depth from OO, cursor depth101. Original world Y+480 cancels cameraY480; output is absolute at the resulting coordinates. Cursor image84 is58×58. Non-character E ends confirmation rather than repeating sound4.

Original manager406fc0, VS branch407782, story407ad6, wrap406e70, menu input414770..414829. EXE constants41e3fc/41e400 are50 and5 ticks. `probe_select_x86.py` executes isolated original input and wrap routines through Unicorn:1764 input frames and160 grid cases. No Wine or OS emulation in APK. Old confirm counter>100 gives101 further ticks after both VS choices,102 after story choice. These delays come from disassembly, not an end-to-end execution of the entire Windows task.

## Android adaptation

CharacterSelect owns presentation only. SelectionControls routes one phone/gamepad to P1 then P2 with a release guard. Independent InputFrames for both players are also supported by the controller. SelectionLayout shares aspect-preserving drawing/touch geometry. The existing60Hz accumulator drives selection. No combat, RNG, snapshots or replay rules changed.

Original background/portraits remain640×480; touch toolbars are outside the viewport. Touch focuses a cell, a separate button confirms. Back from P2 reopens P1; another Back restores the previous menu. The paused battle/story stays intact until commitment. The old story is saved with its old character identity before replacing it.

Two off-grid fighters retain TEST006 diagnostic VS access via «Дополнительно», without invented original portrait cells. Initial cursor positions use the last chosen characters rather than Windows process globals. Focus swaps preloaded portraits directly without emulating the original file unload/reload gap. Alternative palettes selected through A–F in Windows remain unsupported. Team mode is not introduced. Toolbars/menus are Android adaptations, not a claim of pixel-identical Windows UI.

Images in `docs/selection007-render/` are desktop VM renders, not Android screenshots. Installation, touch responsiveness, audio focus and Home require the phone check.
