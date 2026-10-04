# TEST 005 — результаты проверки, 2026-09-24

Выдаваемый APK: `elfen-fighting-005-story.apk`, 68655455 байт.
SHA-256: `3b16ae87daaa1ee45d32d158823fdd287e6c089ac56e102bd2879691ceb5bc07`.
Пакет `org.elfen.fighting.nativeport`, versionCode5 / 0.0.5-story, minSdk29 / targetSdk35. Та же подпись, что TEST003/004. Native .so отсутствуют; ABI-ограничения нет.

## Проверки сборки и данных

| Проверка | Результат |
|---|---|
| Standalone SDK35 build | PASS: Java compile, D8, AAPT2, alignment, подпись |
| Gradle8.9 / AGP8.7.3 | PASS: `./gradlew clean assembleRelease` |
| Проверка Gradle APK | PASS: полный CRC, подпись, manifest, assets; SHA256 `b355b6b9370feedb301581ad7415088c9906d772db30f590ecccef00d0383e7f` |
| Исходные assets | Все2881 сохранены побайтно; добавлен только story-index.tsv |
| Launcher | Все изображения и compiled icon XML совпадают; AGP сокращает их внутренние пути, standalone сохраняет исходные |
| KGT ссылки | 43ссылки разрешены по CP932-именам и исходным SHA256 |
| PLAYER progression | 10маршрутов,100боевых событий,110сценовых событий |
| DEMO | Все27файлов,56754ticks двух независимых VM, snapshots и защита от удерживаемой атаки |
| Изолированное исполнение оригинального EXE | 216DEMO manager и1344story-round fixtures сравнены с Java |
| Сюжетные бои | Все100: загрузка и750кадров на независимой восстановленной копии с покадровыми hashes |
| Ветки | Loss→retry,loss→quit,отклонение повреждённого snapshot без изменения живого состояния |
| Rollback сюжета | Пересчёт40кадров через границу scene→fight с исправленным поздним вводом |
| Визуальный контроль |24PNG из исходного DEMO bytecode; это desktop renderer, не Android screenshots |

StoryDataTest:70687assertions. Код/логи: `tools/test_story.py`, `research/story_*_005.log`. Процедуры EXE вызываются изолированно с перехватом внешних функций, а не как полностью работающая Windows-игра.

## Полный сюжетный маршрут

Люси, seed19. P1 управляется исходным CPU (уровень80), противники — CPU из каждого оригинального события. Ввод переходов/continue проходит через InputFrame. HP, победы и параметры атак не подменяются.

Результат: **217600кадров,10боёв,9побед,ending0202,credits0222,COMPLETE**. При поражениях использованы обычные повторы боя. Финальный противник победил Люси; исходный record5=0 переводит к ending и после поражения. Этот результат не называется победой в финальном бою.

На каждом кадре сохранён snapshot в StoryHistory120 — тот же путь, что в Android. Затем выполнены три полных replay с271контрольной точкой каждый, включая переходы сцен/боёв, и дополнительными restore каждые5003кадра. Все хеши совпали.

Итоговый state hash: `99d01a21bdee45b171cf5df51e652ea1f4cd639e472a5f3a551d8333d6936ce0`.
Запись включена: `research/story-lucy.efs` (4790042байта).

## Исправление, найденное проверками

Более частые snapshots обнаружили stale numbered OO reference в Маю0110skill96. Старая ссылка после повторного использования pool slot удаляла новый объект и оставляла недействительный parent у skill99. Исправлена очистка object links по40e4a0;18original-x86 fixtures и отдельный тест вложенных объектов прошли. Ошибка не скрыта обработчиком исключений.

Из-за исправления результат некоторых AI-прогонов изменяется относительно TEST004. Native damage, commands, CPU records и исходные данные не менялись. Combat revision2 включён в snapshot/replay identity; несовместимая старая запись отклоняется. Подробности — PORTING_NOTES.md.

## Регрессия боя и VS

Повторно выполнены `tools/test_combat.py` и `tools/test_match.py` на коде TEST005:

- 518original collision/damage/RNG fixtures;32582combat/restore assertions.
- Original CPU12×120ticks /1481760exact assertions.
- Command matcher1068cases /1094700assertions;clash/reaction-error394assertions.
- Active CPU twin/restore30012checks;3×2400frame replay,7200покадровых hashes.
- Original VS dispatcher432cases;полный матч21614frames,три replay,68663checks.
- Две независимые VS-копии:3000покадровых совпадений.
- Задержка50/100/150/200мс:все4локальных rollback сценария сошлись,101700resimulated frames суммарно. Сеть не использовалась.
- 12персонажей в match branches,46packs,99анимационных путей,4арены и физические quarter-circle/half-circle/charge inputs прошли.

Логи: `research/combat_regression005.log`, `research/regression004_from005.log`.

## Чего эта проверка не подтверждает

TEST004 пользователь подтвердил на телефоне. TEST005 **ещё не устанавливался и не запускался на Android в этой среде**: нет emulator/adb/KVM; AF_UNIX socket запрещён. Поэтому установка нового APK, новый сюжетный UI, реальные звук/latency/gamepad/lifecycle/autosave требуют PHONE_TEST_005_RU.md. Наличие APK, DEX и desktop PNG это не заменяет.

Полностью автоматизирован один маршрут; остальные9полностью не пройдены. Не заявлены полная эквивалентность Windows-runtime, online versus или русификация оригинальных сцен. Ограничения находятся в KNOWN_ISSUES.md.

Одно промежуточное чтение incremental Gradle APK обнаружило неполный ZIP после сообщения BUILD SUCCESSFUL. Чистая повторная сборка и полные CRC/signature/asset проверки прошли. Выдаваемый standalone APK отдельно проверен; сообщение Gradle само по себе не используется как критерий целостности.

## Контроль архива проекта

`tools/package_checkpoint.py` создаёт `Elfen_Lied_Android_005_project.zip`, проверяет CRC каждой записи, распаковывает проект в отдельный каталог и собирает его заново. Требуется побайтное совпадение собранного APK с выдаваемым. Конкретные размер/SHA256/число файлов и факт повторной сборки фиксируются рядом в `TEST005_CHECKPOINT_VERIFICATION.json`.
