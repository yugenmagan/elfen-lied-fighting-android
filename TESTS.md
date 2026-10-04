# Проверки TEST 003 — 2026-09-22

Полное acceptance не пройдено. TEST 002: пользователь подтвердил анимации, направления, одновременный touch. TEST 003: APK собран, физическая установка/боевое управление ещё не проверены.

| Проверка | Результат | Доказательство |
|---|---|---|
| Parser/архив/кодировки и ссылки | 15 tests PASS | research/static_tests_003.txt |
| 46 packs, 99 animation paths | 2854 assertions PASS | research/engine_test_003.txt |
| Пять движений команд, четыре арены | PASS | research/motion_scenarios_003.txt |
| Damage/RNG/полный обработчик столкновения EXE | 518 assertions PASS | research/combat_tests.txt |
| Оригинальный command matcher | 1068 сценариев, 1094700 сверок результата/истории PASS | research/command_tests.txt |
| Оригинальный CPU | 12 сценариев ×120 последовательных ticks; RNG, внутренние поля, 1024 input cells совпадают | research/cpu_tests.txt |
| FA/FA clash и original reaction-error branches | 64+2 сценария, 394 assertions PASS | research/contact_tests.txt |
| Люси/Нана/Марико против манекена до KO | twins и restore PASS, 32582 assertions | research/combat_tests.txt |
| Три боя CPU80–CPU80 до KO | все кадры twins совпали; 27587 assertions, периодический snapshot restore | research/battle_cpu_tests.txt |
| 12 вариантов персонажей, выбранные противники до KO | PASS | research/all_cpu_characters.txt |
| Replay | 3×2400 hashes совпали; history resimulation60ticks; повреждённая запись отвергнута | research/replay_tests.txt |
| Desktop рендер реальной симуляции | 9 кадров, просмотрены примеры | research/render_checks_003/ |
| APK Java/D8/AAPT2, подпись v3, zipalign | PASS | research/android_build_003.txt |
| Все ресурсы APK побайтно | 2881/2881; 46packs,244имени,88containerentries | research/apk_validation_003.json |
| Fighting.zip | SHA-256 не изменён | research/apk_validation_003.json |
| Android launcher resources | стандартные5densities, round, adaptive foreground/background, manifest wired | android/app/src/main/res/ |

Не считать числом разных игровых сценариев количество проверенных input cells или сравнений hash. x86 probes выполняют реальные изолированные функции EXE через Unicorn, без запуска всей Windows-игры. Desktop AWT — не Android-скриншоты. Headless sound events проверяют ссылки, не фактический звук.

Зеркальный Nana CPU seed19 исследован отдельно: длительный обмен ударами вне досягаемости остаётся открытым сценарием для таймера матча; он не включён в таблицу успешных KO. Исходные AI records на259px перечислены в FORMAT_NOTES.md.

Не проверены/не готовы: новая физическая установка003, реальная Android-отрисовка003, special inputs на телефоне003, звук, lifecycle, физические геймпады; оригинальное меню, раунды, таймер, DEMO, полное прохождение; delayed rollback50/100/150/200ms; сетевой transport/интернет-бой; русская локализация сцен. Итогового готового APK пока нет.

Сценарий телефона: PHONE_TEST_003_RU.md. Исторические результаты002 сохранены в research/history/ и research/*_002*.
