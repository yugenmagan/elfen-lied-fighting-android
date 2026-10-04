# Проверки TEST 007b

Дата: 2026-10-02. База — целый архив 007a, SHA256 e6663c1eb62440e766b9f6faea2c71d5dace74febe61e8e7935c8f1df6f66c19. Все проверки ниже выполнены в Linux/JVM, кроме явно помеченных как не выполненные.

| Проверка | Результат |
|---|---|
| SHA256/CRC исходного checkpoint | Совпали; 3301 entries распакованы |
| CommandGuide: исходные таблицы 14 PLAYER | 1668 комбинаций команда/stance/сторона распознаны; неизвестных режимов 0 |
| Buttons | ABC у основных бойцов; ABCD у Крафта; AB у0154; opcode36 и guard также просканированы |
| Реальные последовательные pointer-down A/B/C | Все 6 порядков в одном tick распознают исходную ABC-команду после FrameInput; до исправления выбиралась обычная C |
| Пауза | Побайтный snapshot и frame не меняются за 2000 попыток tick; вложенные окна, foreground и точный следующий frame после Resume |
| VS: победы/раунды | 2:0,0:2,2:1,1:2 с обеими последовательностями; double KO/timeout draw при1:1; результат после второго выигрыша |
| Reset раунда | HP, позиции, timer, отсутствие старых атакующих объектов; stock/special переносятся по прежним правилам |
| Сюжет | После первого выигрыша остаётся тот же противник; после второго — следующая сцена либо Continue; draw не даёт score |
| Старое story-save | Явная миграция policy сохраняет frame/slot и battle snapshot побайтно; новый save восстанавливается без миграции |
| Новые проверки суммарно | 26442 assertions, research/offline_tests_007b.log |
| Полная ветка Маю до двух побед | CPU80, seed19,169804 ticks,10 боёв,9 побед, ending и титры |
| Полные повторы Маю | 3 запуска ×193 контрольных хеша, плюс периодический snapshot/restore; одинаковый финальный hash |
| Classic rollback | Две simulation,3000 кадров; задержки50/100/150/200ms; подтвердившиеся кадры и финальные хеши совпали, включая переход раунда |
| Legacy rollback | Прежний итоговый hash сохранился для тех же3000 кадров и4 задержек |
| Аркадный стик | 7827 assertions:7 соотношений экрана,3 размера,3 dead zones, зеркальность, мультиввод, настоящие команды Люси/Наны/Марико через новый FrameInput |
| Банка Наны | 15156 assertions; исходные contact/motion fixtures,6 команд с обеих сторон, damage, guard, restore/replay; combat не изменён |
| Replay combat | 3×2400 покадровых хеша,60 ticks resimulation, повреждённый replay отклонён |
| Выбор персонажей | 20321 assertions;1764 native EXE input frames,160 grid cases,10 story intro→fight,10 VS mirrors,7 пропорций |
| Ресурсы/runtime | 2854 assertions;46 пакетов,99 из99 анимационных путей |
| Standalone APK | javac/D8/aapt2/zipalign/apksigner — PASS |
| Gradle clean release+debug | BUILD SUCCESSFUL;75 tasks,73 executed,2 up-to-date |
| Упаковка трёх APK | Все2882 assets присутствуют;2881 исходный hash совпал, story-index.tsv сохранён; CRC, подпись, SDK, launcher/иконка проверены |
| Архив нового проекта | PASS: CRC всех файлов, отдельная распаковка и сборка; получен побайтно тот же APK. Финальный результат — builds/TEST007B_CHECKPOINT_VERIFICATION.json |
| Android установка/запуск/новые диалоги | Не выполнены: adb/emulator/KVM в этой среде отсутствуют |
| Физические multitouch/audio/lifecycle | Требуется сценарий PHONE_TEST_007B_RU.md |

## Хеши детерминизма

- Маю, полный новый маршрут: `b8409b34c4e60c75957c5683963416c3e8e7454e9f1f61f90c6eb9c3d35f5a31`.
- Classic rollback: `8f2913ac4f83c08131fcd980c9ce1da346405198c07cc88221a26d693be15e24`.
- Legacy rollback: `80c4f41871bd4fc59bd7b54f4ba0cf9102834d465f86d2ede1fcced94b0281e7`.
- Legacy combat replay: `d003c48f139f8adde9abe9504f34539d750babd98f0cb9c2d48257817ac93c2f`.

## Незавершённые bounded-проверки

Люси, first-to-two, seed19, CPU80 и CPU100: оба прогона остановлены лимитом900000 ticks на slot11, после5 побед. AI многократно проигрывал шестого противника. Эти прогоны не засчитаны как прохождение; logs story_first2_007b.log и story_first2_cpu100_007b.log сохранены. HP/урон/победитель не менялись ради результата. Проходимость всеми героями с новой политикой пока не подтверждена.

Полная ветка Маю выполнена через обычные InputFrame и исходный CPU, без принудительного KO/подмены damage. Отдельные **unit**-проверки round result намеренно задают HP/points, чтобы покрыть редкие исходы; они не выдаются за естественное прохождение. Маю в финале проигрывает по исходному разрешённому пути к ending; тест не утверждает победу над финальным противником.

## Воспроизведение

```sh
python3 tools/test_offline_007b.py --story
python3 tools/test_hotfix_007a.py
./gradlew --no-daemon assembleRelease assembleDebug
python3 tools/build_android.py
python3 tools/check_apk.py
python3 tools/package_checkpoint.py
```

Gradle8.9 / AGP8.7.3 / JDK17 / build-tools35.0.0 / platform35. Альтернативная standalone-сборка использует те же исходники и manifest; она воспроизводит выдаваемый APK побайтно из распакованного проекта. Gradle меняет упаковку resources, поэтому его APK имеет другую checksum при том же коде проекта.
