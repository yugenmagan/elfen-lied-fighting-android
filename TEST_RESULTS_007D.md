# TEST 007d — проверка оригинального HUD

Дата: 2026-10-02. Основа: финальный TEST007c, ZIP SHA-256 `5c8d7b74e54a7bb9c99413016c833406bd2a8aef3a83806766ce0e5343d1bae1`.

## Выполнено

| Проверка | Результат |
|---|---|
| Сохранение кода и данных 007c | 2938 файлов runtime/network/java/assets/res идентичны; изменены MainActivity и read-only BattleView, добавлен BattleHud |
| Управление | Код от layoutControls до конца MainActivity идентичен: геометрия, цвет, multi-pointer input, кнопочные haptics сохранены |
| Обрезка полос | 56 случаев совпали с исполнением оригинальных x86 инструкций: позиция, ширина и смещение источника |
| Таймер | 12 вариантов infinity/1–3 цифр совпали с изолированной исходной процедурой по количеству, позиции и skill |
| Все PLAYER | 14 файлов; 11 исходных портретов, 3 явно пустых навыка; все HUD-ссылки на PNG разрешаются |
| Общая HUD-проверка | 2314 проверок, включая повторные вызовы, метки выигрыша, phase meter и snapshot restore |
| Изоляция симуляции | Две независимые simulation с одинаковыми AI/input/seed, 2400 ticks с HUD и без него; hashes сравнивались каждые 31 ticks и совпали |
| Восстановление состояния | Snapshot кадра701 вернул идентичный список отрисовки HUD |
| Отрисовка | 9 PNG через Java desktop renderer; проверены полные/потраченные полосы, портреты, цифры и рамки |
| Standalone APK | Собран; CRC, zipalign, aapt metadata и apksigner прошли |
| Gradle | assembleRelease и assembleDebug прошли, 74 tasks executed; оба APK дополнительно проверены |
| Игровые assets | 2881 исходных assets совпадают с reference APK; story-index.tsv сохранён; всего2882 |
| Иконка | Исходные пиксели сохранены; debug AAPT2 меняет кодирование некоторых PNG, не пиксели |
| Update identity | Прежние package ID и certificate, versionCode11 / versionName0.0.7d-hud, minSdk29/targetSdk35 |

CRC нового ZIP, отдельная распаковка, сборка из извлечённых исходников и сравнение APK фиксируются упаковщиком в поставляемом рядом `TEST007D_CHECKPOINT_VERIFICATION.json`. Это отдельный отчёт о финальном архиве; он не включается сам в проверяемый ZIP.

## Артефакты и воспроизведение

- `research/preservation_007d.json` и `research/source_changes_007d.diff`: сравнение с 007c.
- `research/hud/clip-x86.tsv`, `timer-x86.tsv`, `timer-x86.json`: исходные x86 fixtures.
- `research/hud_native_007d.log`, `hud_tests_007d.log`, `hud_render_007d.log`: результаты тестов.
- `research/build_007d.log`, `gradle_007d.log`: журналы сборки.
- `research/apk_validation_007d.json`, `gradle_release_validation_007d.json`, `gradle_debug_validation_007d.json`: проверки APK.
- `docs/hud007d-render/`: 9 настольных превью, **не скриншоты Android**.

```sh
python3 tools/test_hud_007d.py
python3 tools/build_android.py
python3 tools/check_apk.py
./gradlew assembleRelease assembleDebug
python3 tools/package_checkpoint.py
```

Для нового исполнения x86 probes: установить `tools/requirements-research.txt` в `.deps/python`, затем `python3 tools/test_hud_007d.py --probe`. Сохранённые fixtures не требуют Unicorn для обычной Java-проверки. Инструкция сборки — `docs/BUILD.md`.

Выдаваемый `elfen-007d.apk`: 68 684 127 байт, SHA-256 `b0c84c7992c0c19cc708edaab2334368a2229fc8a4e1687527b4d975b8954dbe`.

## Границы проверки

В этой среде отсутствуют adb, Android Emulator и KVM. Установка, новый HUD на Android Canvas, расположение поверх touch-кнопок и производительность 007d на телефоне здесь не проверены. Desktop previews и x86 fixtures этого не заменяют.

Полный Windows runtime не запускался; probes исполняют только указанные исходные процедуры, подменяя выделение timer tasks для фиксации результата. Положение/цвет графики восстановлены из оригинальных skills. Совпадение глобальной фазы анимации с Windows task scheduler полностью не установлено.

Combat/story/network не переписывались. Полные прежние сюжетные и сетевые прогоны не повторялись ради этой правки. Состояние этих компонентов и незавершённые проверки сохранены в `KNOWN_ISSUES.md` и исторических результатах. Эта итерация не объявляет весь порт завершённым.
