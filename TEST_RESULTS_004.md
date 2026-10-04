# Проверки TEST 004 — 24 сентября 2026

## Восстановленная база

Контрольный `Elfen_Lied_Android_003_RECOVERED_project.zip` извлечён заново после очистки среды. SHA-256 архива: `fa9f314fd4a36c7f4f0781ca0e2a70c9babbfd55d905c719ae00e2305eebbc1c`. CRC всех 3066 файлов корректны. Независимая повторная сборка дала исходный APK `6709395d45df9876d476a3795aa67758d807ce5b80f404d04bf976ab2a1ac7dd`; 2900/2900 записей совпали. TEST004 разработан в отдельной копии.

## TEST004: headless JVM

- 432 изолированных случая оригинального EXE, функция0x4086a0: состояния/ожидания, таймер, HP, счёт, выбор системных анимаций совпадают. Presentation/task creation в probe перехвачены; остальная логика исполнялась как исходные x86-инструкции. Fixtures включены.
- Полный матч CPU Люси–Нана: 3 раунда, 21 614 кадров, счёт1:2. Три независимых replay дают одинаковый SHA-256 на каждом кадре (64 842 сравнения). Общий тест: 68 663 проверки, пять точек snapshot/restore, в том числе перед результатами раунда.
- Конечный хеш этого матча: `e7fa7b19d8518f7df7a81fcddba181065d156fe6e5ae1b071af78182ae41974d`.
- 12 матчей с разными персонажными наборами завершились до экрана результата, четыре через естественный KO, остальные по времени.
- Отдельно: P1/P2 perfect, doubleKO, ничья с очком обоим, бесконечный таймер, перенос meter/stocks, сброс HP, восстановление через границу раунда, отклонение повреждённого replay/snapshot без частичной мутации.
- Две независимые simulation: 3000 одинаковых покадровых хешей.
- Искусственная задержка P2: 3/6/9/12 ticks (50/100/150/200мс). 11 970 сравнений подтверждённых состояний; 101 700 пересчитанных кадров плюс финальное получение задержанного ввода. Ни одного desync. Конечный хеш всех четырёх сценариев: `98ac1dead1c8d3dc7b62a1edd12a4f2a594165144f5a949d8dc24a370d99d3ec`.
- History хранит последние120 ticks; отдельно восстановлены и пересчитаны80 ticks.
- 46 packs, 2854 assertions, 99/99 animation paths; четыре арены по1800 ticks; пять физических командных InputFrame-сценариев, включая quarter-circle, half-circle и charge, с600 последующими ticks.
- Desktop renderer показал исходные ROUND / FIGHT / Win из KGT. PNG в `docs/match004-render/` — визуальная проверка симуляции, **не скриншоты Android**.

## Регрессия TEST003

Старые tests повторно пройдены после match hooks: 518 исходных collision/damage/RNG cases; CPU1 481 760 сравнений; command matcher1 094 700; clash/reaction394; combat/restore32 582; CPU twins/restore27 587; три replay по2400 кадров; 12 исходных KO-сценариев. Боевые формулы и AI не переписывались.

## Android и упаковка

Standalone SDK build и `./gradlew assembleRelease` выполнены. Подпись TEST003 сохранена. Все2881 asset-файлов и launcher resources сверяются с исходным APK. CRC, apksigner и zipalign проверяются инструментом `tools/check_apk.py`.

Итоговый архив после создания отдельно распаковывается и собирается из этой копии без переноса class/dex/build cache. Точные hashes и результат находятся рядом в `TEST004_CHECKPOINT_VERIFICATION.json`.

Установка, запуск, multitouch, audio latency и lifecycle TEST004 на Android **не проверены в этой среде**. Здесь нет Android Emulator/adb/устройства/KVM, AF_UNIX EPERM. Для проверки на телефоне выдан `PHONE_TEST_004_RU.md`. Нельзя считать пройденными acceptance-критерии полного порта или интернет-боя.

Логи: `research/match_*_004.log`, `research/combat_regression_004.log`, `research/android_build_004.log`, `research/gradle_build_004.log`.
