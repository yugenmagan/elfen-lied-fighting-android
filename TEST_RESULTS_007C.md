# TEST 007c — проверка минорного обновления

Дата: 2026-10-02. База: финальный TEST 007b, ZIP SHA-256 `93b3b852aae8d365370e228746ff324fadd4dbdcff3abe1c83a787cc6d78a756`.

## Выполнено

| Проверка | Результат |
|---|---|
| Сравнение с исходниками 007b | Из игровых Java-файлов изменён только MainActivity: цвета, отсутствие букв, Start, haptic callbacks, обозначение версии |
| Геометрия и обработка ввода | layoutControls, buttonX, buttonY сохранены; 46 touch-событий дают те же Input samples и переходы, что обработчик 007b |
| Виброотклик | JVM harness исполняет настоящий onTouchEvent: нажатия A/B/C и Start вызывают VIRTUAL_KEY; стик, удержание, отпускание, CANCEL, пустая область и перенос блока атак — без новых запросов |
| Combat / network / assets / иконка | Все 34 runtime-класса, 4 network-класса, 2882 assets и 14 Android-ресурсов совпадают с 007b |
| SDK standalone build | Успешно; `builds/elfen-007c.apk` |
| Gradle assembleRelease assembleDebug | BUILD SUCCESSFUL; 74 задачи выполнены |
| Проверка трёх APK | ZIP CRC, alignment, подпись, manifest и игровые assets прошли проверку |
| Подпись и версия | Прежний сертификат TEST 003; org.elfen.fighting.nativeport; versionCode 10 / 0.0.7c-controls; minSdk 29 / targetSdk 35 |
| Разрешения | Прежнее INTERNET; новых разрешений нет |

Standalone APK: 68 680 031 байт; SHA-256 `763c634e74fc35d355a9952a3e1a6c280941c5c02a0bfc102a0fb5bed9eb6020`.

Gradle release/debug и standalone используют одни исходники, но разные упаковщики; их APK не обязаны совпадать побайтно. Gradle debug перекодирует часть PNG иконки, их декодированные пиксели проверены и совпадают. Игровые данные проверяются строго побайтно.

## Проверка checkpoint

`python3 tools/package_checkpoint.py` создаёт архив, проверяет CRC каждого файла, распаковывает его в отдельный каталог, собирает и проверяет APK из распакованной копии. Несовпадение SHA-256 с выдаваемым standalone APK завершает проверку ошибкой. Фактический результат и SHA-256 финального ZIP находятся в отдельно поставляемом `TEST007C_CHECKPOINT_VERIFICATION.json`.

При повторной распаковке выявлена гонка со служебными временными копиями среды. Сборочный скрипт и упаковщик теперь исключают служебные каталоги из обхода. Игровые исходники и содержимое выдаваемого APK от этого не изменяются; финальная проверка вновь требует побайтного совпадения APK.

## Доказательства и пределы

- `research/source_comparison_007c.json`, `research/source_changes_007c.diff`.
- `research/touch_feedback_007c.log`; воспроизведение: `python3 tools/check_touch_feedback_007c.py`.
- `research/build_007c.log`, `research/gradle_007c.log`.
- `research/apk_validation_007c.json`, `research/gradle_release_validation_007c.json`, `research/gradle_debug_validation_007c.json`.
- Android-приложение здесь не запускалось: adb, emulator и KVM отсутствуют. JVM harness проверяет вызовы haptic API и сохранность ввода, а не физическую вибрацию, её задержку или внешний вид на телефоне.
- Полные сюжетные/боевые тесты этой косметической итерацией не повторялись. Прежние результаты относятся к 007b; соответствующие исходники и assets в 007c совпадают побайтно.
- Сценарий устройства: `README_TEST_007C_RU.md`.
