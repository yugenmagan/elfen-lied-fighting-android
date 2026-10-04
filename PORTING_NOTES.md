# Технический журнал переноса

Обновлено: 2026-10-03 (Москва). **Текущая итерация TEST 008: финальный исход и JP/EN/RU.** Исторические разделы относятся к прежним итерациям; актуальны TEST_RESULTS_008.md, docs/LOCALIZATION_008.md и KNOWN_ISSUES.md.

## Архитектура и фактический результат

Собственный Java runtime исполняет оригинальный bytecode и таблицы из EFP1/PNG/WAV/MIDI, подготовленных Python-конвертером. Android Canvas, SoundPool/MediaPlayer и MotionEvent обеспечивают вывод, звук и ввод. Fixed simulation tick 60Hz отделён от renderer, аудио и lifecycle. Унифицированный InputFrame, полные snapshots, RNG, state hashes, replay и rollback history сохранены. Windows EXE, Wine и Winlator в APK отсутствуют.

TEST 005 использует существующую combat logic TEST 003/004, а не новую реализацию по памяти. Добавлены StoryProgram, StoryCatalog, DemoSession, StoryRound, StoryController, StoryReplay и StoryHistory. Данные 10 сюжетных маршрутов прочитаны; полный маршрут Люси автоматически достиг ending и титров, с 9 победами и исходным переходом после поражения в финальном бою. Новый APK не объявляется проверенным на Android до телефонной проверки. Онлайн и русификация оригинальных сцен ещё не реализованы.

Android 10+, minSdk29, target/compileSdk35; pure Java/DEX без ABI-ограничения, включая arm64-v8a/x86_64. Это не подтверждение работы на каждом ABI. Исходная иконка Люси и настройки touch/gamepad сохранены.

## Подтверждённая идентификация

Архив `Fighting.zip`, SHA-256 `347016a345bf56377bfbf311534d2f48855ada091ee3677e68085196d91f9d68`, не изменён. 244 ZIP entries, 118 содержательных файлов, 75 уникальных содержимых. Оба EXE идентичны, PE32 x86, machine 0x014c. VERSIONINFO: FileVersion 1.0.2.0, ProductVersion 1.0.0.1; ProductName/Description `２Ｄ格闘ツクール2nd.`; ENTERBRAIN/OUTBACK, 2001. Номер конкретного редакторского патча не установлен.

Найдены `2DKGT2G`, `2DKGT2K`, `KGT2KGAME`, `KGT2nd_EDITOR.exe`; импорты DirectDraw, DirectSound, WINMM, USER32/GDI32, DPLAYX/WSOCK32. Значения окна в game.ini — 640×480. A–F: Z/X/C/A/S/D; Pause: Escape; joystick A–F: 0–5. Японские подписи левого/правого направлений в ini противоположны VK-кодам 37/39; исходный файл не исправлялся, направление Android задаётся явно. Динамическое соответствие ini оригинальному меню ещё не проверено.

## Имена и неизменность ресурсов

Это не обычный CP932 ZIP: в сырых именах сохранён UTF-8 от mojibake CP1251/CP1252 с Unicode NFD. Восстановление: raw ZIP bytes → UTF-8 → NFC → обратное CP1251/CP1252 → CP932 → Unicode. Неоднозначности запрещены, одинаковые имена с разным содержимым не перезаписываются.

`research/filename_manifest.json` сохраняет все raw hex, Unicode-имена, цепочки декодирования, SHA-256 и entry paths. Его `android_safe_path` — старое резервирование исходных записей, не путь упакованного EFP. Актуальная полная таблица raw → Japanese → реальный Android pack находится в `research/android_filename_map.json` и в APK `assets/game/filename-map.json`. Все 88 исходных container entries разрешаются в 46 уникальных пакетов по SHA-256. Прочие исходные файлы остаются в неизменном ZIP; у них нет исполняемого pack path.

Варианты Марико 0082/0128 различаются командами; варианты Крафта 0142/0144 — байткодом. Все варианты сохранены. Тестовое меню выбирает обновлённую Марико 0128 (согласуется с оригинальной 説明.txt и charge 80 вместо 40) и базового Крафта 0142; усиленный 0144 не подставляется молча. Alias メストＥＤ с внутренним ロストＥＤ сохранён и сводится к идентичным байтам по hash.

Не заменяли исходную графику, реплики, музыку или параметры боя. Изображения переводятся из индексов в PNG-кэш, исходные индексы и все восемь палитр дополнительно сохранены. Публичной публикации ресурсов или APK не было.

## Что прочитано

46 уникальных контейнеров: 1 KGT, 14 PLAYER, 4 STAGE, 27 DEMO. Все 376 832 slots проверены; 2 567 непустых изображений строго декодированы. 220 аудио: 208 RIFF/WAV и 12 MIDI. 12 имён персонажей, 4 arenas, 27 scenes из KGT разрешаются. Таблицы команд, CPU slots, story events и неизвестные хвосты сохранены. Эти результаты не подтверждают работу AI или сюжетного проигрывателя.

`FORMAT_NOTES.md` описывает структуру, установленные x86-адреса и неизвестные части. Часть подсказок внешнего парсера оказалась неточной: opcode 37 `AI` означает afterimage, а не искусственный интеллект; M flags сохраняют компоненты скорости, а не останавливают их.

## Историческая реализация TEST 002: анимационный runtime

Фиксированный шаг 60 Гц выбран для текущего тестового сеанса; полная синхронизация с планировщиком оригинала ещё не восстановлена. Числа движения 16.16, множители из кода EXE. При game speed 10: timing=100, velocity=655, acceleration=393. Оригинальный порядок интеграции: скорость += ускорение; позиция += новая скорость; затем привязка объекта к родителю. Переполнение int сохраняет поведение 32-битного ADD.

Восстановлены загрузка skills, I/M, ожидания, ветвления, переменные, создание объектов, звук, часть gauges и условий. Hit/hurt rectangles и hit-junction данные читаются, но collision scheduling/реакции ещё не исполняются. Неподдержанные инструкции останавливают тест с pack/skill/pc; критические файлы не заменяются пустыми данными. В START/landing исправлено сохранение block offset до сброса триггеров.

PreviewSession пока содержит тестовый выбор idle/walk/crouch/jump/attack и возвращение в idle. Это временный исполнитель для проверки оригинальных анимаций, не доказательство эквивалентности боевого state machine. Граница сцены, камера, потеря высоты, переключение навыков и поведение объектов при freeze требуют дальнейшего восстановления.

## Графика

Вывод 640×480 с единым масштабом и чёрными полями; арена 1280×960 показывается через камеру, не ужимается целиком. Для персонажей используется нижняя центральная точка и original image offsets. Фоновые слои имеют другой anchor. Отражения, глубина и RGB tint читаются из инструкций.

В коде EXE установлено: прозрачность определяется RGB==0, не индексом 0 и не четвёртым байтом палитры. Opaque black в ресурсах часто представлен минимальным ненулевым цветом. Каналы исходно квантованы до пяти бит. PNG-кэш исправлен соответственно. Реализованы normal, alpha, add и часть custom alpha; subtract ещё вызывает видимую остановку. Afterimage-параметры читаются, но следы пока не рисуются. Альтернативные палитры сохранены, переключение ещё не реализовано.

В `research/render_checks` есть шесть локальных рендеров Java/AWT. Они проверяют геометрию обычных кадров, не Android Canvas, полный compositing или работу на телефоне. Это не скриншоты Android.

## Ввод

Каждый touch pointer и источник геймпада имеет отдельную маску; итог — OR. Отпускание пальца не сбрасывает другие. Обрабатываются исторические MotionEvent samples и rolling D-pad; диагональ — два направления. Короткое нажатие кнопки сохраняется до simulation tick. История направления хранит время кадра, чтобы дополнительные события не накапливали charge быстрее.

Commands читает оригинальный порядок, направления, кнопки, time и charge fields. Пять заданных движений распознаются из оригинальных таблиц; точность окна допуска и приоритетов против оригинала ещё не подтверждена. Сброс входа выполняется при pause, Cancel, потере фокуса и открытии меню.

В APK есть шесть кнопок, масштаб/прозрачность/dead zone, зеркалирование, перемещение двух групп, сохранение настроек, скрытие overlay при физическом геймпаде. KeyEvent/joystick axes/HAT поддерживаются, шесть основных кнопок переназначаются. Analog trigger E/F пока фиксирован. Реальные направления и одновременные касания TEST 002 подтверждены пользователем. Конкретные Xbox/DS4/DualSense/HID устройства не проверялись.

## Звук и Android lifecycle

SoundPool загружает исходные WAV, MediaPlayer — исходные MIDI; внешнего банка или Интернет-загрузки нет. MIDI type 0/1 предусмотрен платформой согласно [Android Supported media formats](https://developer.android.com/media/platform/supported-formats). Это не доказательство воспроизведения наших файлов на устройстве; тембр системного MIDI может отличаться от Windows.

Написаны audio focus, остановка/возврат при Menu/Home/pause, очистка касаний и аккумулятора кадров. Исправлено воспроизведение при невыданном audio focus: play и асинхронный prepared callback проверяют фактический focused. Звук не проверен прослушиванием на Android; looping, задержка и потеря фокуса требуют телефона. После уничтожения процесса запускается новый тест, не сохранённый матч.

## Сборка и проверка

Проверенный путь: `python3 tools/build_android.py`, JDK 17, официальные SDK Build Tools 35.0.0 и Platform 35 r02 с проверкой pinned SHA-1. Python-конвертер нужен только при повторном извлечении; его результат включён в проект. AAPT2/D8/zipalign/apksigner проходят. Подпись v3; тестовый ключ сохранён для последующих обновлений.

Android Studio project: AGP 8.7.3, Gradle 8.9, Java 8 source/target. Namespace задан в Gradle согласно [требованию AGP 8](https://developer.android.com/build/releases/agp-8-0-0-release-notes). Комбинация Gradle/JDK сверена с [AGP 8.7](https://developer.android.com/build/releases/agp-8-7-0-release-notes). UI Android Studio не проверен. Попытка Gradle build остановилась на Java HTTPS-загрузке дистрибутива с Network is unreachable; проверен эквивалентный standalone builder. Повторный standalone build дал побайтно идентичный APK в той же среде. Точные команды и ограничения — `docs/BUILD.md`.

Результаты: 15 parser/archive tests, 2 854 Java assertions, 99/99 animation paths, 5 command scenarios по 600 последующих ticks, 4 stages по 1 800 ticks, 29 isolated x86 cases. Статическая упаковка проверяет каждый из 2 881 asset-файла побайтно, отсутствие EXE/JNI, сохранность ZIP и соответствие имён. См. TESTS.md и research/*_002*.

## Ограничение среды и дальнейшая работа

В доступной среде AF_UNIX запрещён (EPERM); прежний Wine завершался до игры. adb/Android emulator/доступного устройства/KVM нет. Ограничение не обходилось. Вместо требования Windows от пользователя выполнены статический анализ и запуск изолированных чистых x86-фрагментов через Unicorn. Это не запуск Windows-игры целиком и не Android-тест.

Пользователь уже разрешил проверку промежуточных APK на своём телефоне. Повторного выбора архитектуры или Windows-PC не требуется. Следующий внешний шаг — `PHONE_TEST_002_RU.md`: установка, 3 касания, команды, звук и lifecycle. `device-report.txt` формируется локально и отправляется только по явному действию пользователя.

Следующие внутренние задачи: восстановить collision/hit junction/damage/stun/block/hitstop, штатный state machine и AI; довести бой до KO; затем исходное меню, story graph, DEMO и ending. Не подставлять предполагаемые damage/AI или слайд-шоу вместо оригинальной семантики. Локализация ждёт проходимой версии.

## Зависимости и лицензии

Ориентир формата: [fm2ndparser](https://github.com/xem85/fm2ndparser/tree/7266d65b9ca486a619b6b10836ee0cfed734fefb), MIT; исходники и лицензия в research/vendor. Его CLI и runtime не включены в APK. Pillow 12.3.0 — экспорт; Unicorn 2.1.4 и pefile 2024.8.26 — исследование; всё это используется на стороне разработки. Android SDK не входит в архив исходников. Gradle wrapper имеет собственную сохранённую лицензию. Wine/Winlator/Box64 не включены и не являются зависимостями. Подробности — THIRD_PARTY_NOTICES.md.

## Новые требования после TEST 002

Пользователь подтвердил анимации и multitouch TEST 002. Звук, lifecycle и геймпады этим сообщением не подтверждены. APK 002 не пересобирается только ради архитектуры.

Финальный результат теперь включает online versus Create Room / Join Room. До транспорта: оригинальная combat logic → детерминированная симуляция и локальный бой → полный solo → replay/restore, две независимые копии с hashes, локальный rollback при 50/100/150/200 ms. Сервера и matchmaking сейчас не разрабатываются. Выбор размещения relay/signalling требуется только когда локальная часть готова.

Фиксированный tick, frame-numbered InputFrame, целочисленная математика, canonical snapshots всех игровых полей и VM/command history, RNG state и hash обязательны. Android, рендер и аудио не меняют simulation state; эффекты выдаются событиями кадра.

Для финальной сборки нужна собственная launcher icon из оригинального портрета Люси; adaptive и mipmap ресурсы плюс экспорт и точное имя исходного ассета. Приоритет ниже корректного runtime и APK.

## Боевой этап: 2026-09-22, промежуточный исходный checkpoint

Написан headless BattleSimulation, не связанный с Android/Canvas/SoundPool. InputFrame содержит номер simulation frame, обе маски и упорядоченные touch samples. BattleView — отдельные неизменяемые копии данных для вывода. Звук/эффекты выдаются событиями кадра, аудио не является callback симуляции. Frame rate — фиксированные 60 ticks/s; wall-clock внутри боя отсутствует.

Canonical binary snapshots сохраняют VM PC, ожидания, call/loop stacks, все script fields, 16+16+16 переменных, ссылки parent/controller, объекты и slots, FA/FD/R, stun/hitstop, input history, RNG, KO, combo, red life, camera. Restore сначала валидирует отдельное состояние; hash — SHA-256. Идентичность четырёх исходных pack hashes обязательна. Снимок текущего подмножества ещё не доказывает полноту будущих AI/story полей: они должны добавляться в codec при реализации.

518 assertions сверили математику с original x86: 10 collision scenarios × 7 outputs, 192 damage cases, 256 RNG draws. Первые три replay-сценария Люси/Нана/Марико достигли KO против неподвижной Наны; две независимые копии совпали на каждом кадре, восстановление frame 351 и повторный просчёт совпали. 36 598 assertions в первом прогоне; точный лог research/combat_tests.txt. Это НЕ оригинальный AI, НЕ полные match rules, НЕ Android-проверка combat. Дальнейшие правки могут изменить golden hashes; хеши версий не смешиваются.

Новый боевой код пока не подключён к APK002. Восстановлены FD geometry, FA flags, R junction selection, damage/combo correction/chip, meter, hitstop, реакционные skills/KO, PS, RP и RNG32. Collision scheduler и базовые states написаны по EXE, но полная дифференциальная проверка всего боя ещё не выполнена. Command tolerance, CPU, projectile clashes, camera/boundary details и match/story scheduling остаются рабочими задачами. Временные правила тестовой сессии не объявляются балансом оригинала.

## TEST 003 — подключённый combat, 2026-09-22

Разделы выше сохраняют историю исследования; актуальное состояние описано здесь и в STATUS.json. Android теперь использует BattleSimulation и неизменяемый BattleView, а не PreviewSession. Fixed tick60, InputFrame с номером и ordered samples; CPU читает исходные111-byte records и подаёт унифицированную историю ввода. AI state, RNG, command history, lastCommand, freeze-input входят в snapshot version3. UI/audio не могут изменить BattleState. ReplayTape сохраняет начальный снимок+полный input stream с SHA256; BattleHistory поддерживает bounded snapshots/inputs и пересчёт от frameN. Сетевой transport не начат.

Восстановлены command matcher0x410060 и CPU0x411270; x86 сравнения:1068командных случаев,12CPUсценариев×120ticks. Дополнены GS fallback, opcode36 short-commands, hitcancel, projectile clashes и ON4wall. Исправлено зависание Nana blowback52: clamp должен вызывать ON4. После наземного END выполняется разворот к противнику; повторное распознавание того же skill не перезапускает его. ON1 landing сохраняет R и прочие ON, сбрасывая FA/FD: это необходимо для вертолёта Люси117.

Anna88 имеет нулевые guard-R entries в одной фазе. Оригинал на0x40f657 выводит `reaction error 2` через0x415190 и пропускает остаток обработки контакта. Порт повторяет это поведение и пишет диагностическое событие, а не придумывает hit reaction. Два подобных случая сверены с x86. Строгие ошибки отсутствующих ресурсов/неизвестных opcode остаются.

Три активных CPU боя и12вариантов персонажей достигли KO в заданных сценариях; детерминизм и replay проверены локально. Список и границы доказательств — TESTS.md. Nana mirror seed19 застревает на259px: исходные AI records4,10,22,27 выбирают атаки без движения. Не вводится искусственное сближение. Нужны original timed match rules. Этот случай не засчитывается как успешный KO.

Android получил HP/meter HUD, настройку CPU/противника, победу/поражение и перезапуск тестового боя. Subtract blending реализован в software640×480 framebuffer. Opcode14 визуальное событие и afterimages пока не отрисовываются. Отчёт ZIP содержит device-report.txt и last-replay.efr; передача только по явному пользовательскому действию.

APK003 подписан тем же тестовым ключом. Анимации/ввод002 подтверждены пользователем; установка003/latency/audio/lifecycle пока ждут телефона. Сюжет, раунды, полный route и online не объявляются готовыми. Следующий этап — original match rules, KGT story links и DEMO execution; затем полная одиночная ветка, local delayed rollback и transport.

## Иконка Люси

Источник: Fighting/g2サヴァイヴ/ルーシー.player, entry0170, image135, builtin21→skill24 `キャラセレフェイス`. Копия исходного изображения: docs/icon/lucy-original-0170-image0135.png. Из него image-generation edit создал очищенный foreground с тем же лицом/волосами/рожками; новый сторонний арт не использовался. Игровые assets не заменялись. Foreground сохранён отдельно; ExportIcons.java воспроизводит5mipmap densities, round и adaptive layer и1024pxэкспорт. Источник и параметры — docs/icon/ICON_DESIGN.md.

## TEST004 — 24 сентября 2026: match runtime

После очистки временного workspace восстановлен сохранённый recoveredTEST003 ZIP. Его SHA256/CRC проверены; сборка снова побайтно совпала с эталоном. Несохранённый черновик TEST004 отсутствовал и восстановлен в отдельном `elfen_test004` поверх этой базы. Combat не заменялся новой реализацией.

Добавлены MatchRules, RoundController, MatchState, MatchController, MatchOverlay, MatchReplay, MatchHistory. В BattleSimulation добавлены presentation-tick hooks без collision/AI ввода, начальная character intro17, установка результата раунда. Обычный combat step сохранил прежние регрессионные hashes. Android рисует read-only snapshots, получает одноразовые audio events и не меняет simulation из renderer/audio. Fixed tick60Hz отделён от Choreographer; wall-clock используется только UI scheduler и диагностикой.

RoundController повторяет single-VS ветку0x4086a0, числовые фазы110/111/112/200/300/510–541/900–902. KGT runtime-tail отображается от0x435470; u16 table0x4451c2–0x4451ea содержит intro/outro/ROUND/FIGHT/KO/PERFECT/P1WIN/P2WIN/DRAW/DOUBLEKO. Длительность берётся из opcode0 metadata u16+1, как4068e0. Фазовый wait использует post-decrement; 432 isolated original-x86 cases покрывают переходы и fallthrough.

Оригинальный clock = timeSetting*100−1, уменьшается на1 в phase200; значение−1 означает без таймера. При underflow clock0 и phase300. Победитель определяется сравнением1000*HP/maxHP в integer, оба нуля — doubleKO. Draw/doubleKO дают очко обоим. Perfect проверяет HP==maxHP. Initial meter задаётся лишь перед round1 (411f2d); новый раунд сбрасывает HP, задачи и команды, переносит stocks/special и RNG. В конечной фазе оригинал создаёт character-select task10; TEST004 показывает собственный итоговый экран и меню.

Полный EFMS snapshot содержит asset identity, rules, глобальный input frame, фазу/ожидание/таймер/счёт, весь battle snapshot v3 с RNG/CPU/commands/entities, а также все KGT script states/ссылки/переменные. Decode выполняется во временных объектах до commit. Hash SHA256 по явной big-endian сериализации. MatchReplay EFMR хранит initial snapshot и последовательные InputFrame; AI остаётся исходной детерминированной CPU-программой, её состояние/RNG входят в snapshot. MatchHistory — ограниченное кольцо120ticks, restore N + resimulate до текущего кадра. Внешние эффекты при resimulation не исполняются. Два core и задержки3/6/9/12ticks проверены локально; transport отсутствует.

### Пустая result-анимация

Нана builtin20→skill21 (blocks127..128), Люси builtin20→skill22 (99..100) содержат только metadata. Прежний порт по завершении character script повторно запускал такую анимацию в том же interpreter pass и превышал300instruction budget. Для пустого result-script TEST004 сохраняет последнюю позу, ставит wait−1 и ждёт следующего раунда. Данные не изменены, проверка неизвестных инструкций/ресурсов не отключена. Это явное исправление порта. Статически проверены оригинальные finish/result routines411a80/411b86 и boundary412625; полная визуальная эквивалентность этого особого случая с оригиналом не доказана и не заявляется.

KGT presentation играет через тот же VM, но в отдельном Host без доступа к battle damage/RNG. OO number0 в системных анимациях не использует player object slots; это сохраняет две intro-полосы одновременно. Используемые system states сериализуются. Android KGT sprites рисуются в абсолютных координатах исходного окна640×480; stage/characters сохраняют прежнюю камеру.

Старые ограничения camera/collision/visual instructions сохраняются. Исходная частота Windows scheduler и все зависимости произвольных специальных script variables от матча ещё не подтверждены. Точная проверка отдельных функций не означает полную эквивалентность всей Windows-игре.

### Android TEST004

VersionCode4, versionName0.0.4-match, та же подпись/applicationId. Menu: правила, новый матч, повтор, сохранить/восстановить кадр. Touch/gamepad код сохранён. SFX KGT предварительно загружаются. Replay UI проверяет конечный hash; автоматический тест проверяет каждый кадр. Audio playback/голоса не являются состоянием боя; restore перезапускает BGM и не воспроизводит уже отменённые эффекты.

Gradle8.9/AGP8.7.3 и standalone SDK35 build прошли. JVM в данной среде получает штатные proxy host/port через параметры процесса, без изменения TLS/checksum; эти параметры не встроены в проект. Новый Android-запуск здесь невозможен (нет adb/emulator/KVM, AF_UNIX EPERM). Требуется телефонный тест004; его нельзя подменять desktop PNG.


## TEST 005 — 24 сентября 2026: оригинальная progression и DEMO

База — сохранённый и проверенный checkpoint TEST 004. Его CRC и SHA-256 проверены перед распаковкой. Все 2881 существующих asset-файлов и launcher resources сохранены побайтно; добавлен только производный `game/story-index.tsv`. Он связывает 43 непустые ссылки KGT с pack ID, SHA-256 и точным CP932-именем. Варианты Марико0128/Крафта0142 соответствуют уже задокументированной политике TEST003; альтернативные данные не удалены. Исходный архив не менялся.

### Сюжетные события и правила

PLAYER runtime story base 0x4d9a49, stride206; на диске type0 занимает один байт, остальные типы — ещё205байт. StoryProgram читает100 records после сохранённого originalTail, не создаёт порядок боёв вручную. Dispatcher4069b0: type1 — бой,2 — DEMO,4 — завершение. Type3 conditional переход восстановлен статически; в предоставленных маршрутах его нет. Поля побед, времени, позиции, уровня CPU и continue считываются из каждого события. Все100 боёв этих данных имеют одного противника; неподдержанный состав не подменяется.

StoryRound реализует сюжетную ветку4086a0. В отличие от VS она использует YOUWIN/YOULOSE и очки за поражение участника. KO award40e863 берёт значения record19/39, recipient18/38=0 означает фактического победившего участника. Учтены post-decrement waits, fallthrough, относительный HP и исходная проверка continue40983f. У финального боя Люси record5=0: ending наступает и после поражения. Автоматический тест именно проиграл этот бой; победа или HP не были подделаны.

### Исполнение DEMO

406c10 создаёт корневые script tasks через406790. Корни с пустым именем/без I пропускаются; depth12, после разделителя type3 —100; managerdepth127. Сцена исполняет параллельные оригинальные VM с исходными I/M/CO/звук/ветвлениями. На первом tick менеджер создаёт задачи, выполнение начинается на следующем. Явный E (412617) завершает non-character task, даже если автоматический EOF должен был бы зациклить его. Для этого Script.Host получил default explicitEnd=false; боевые VM сохранили прежнее поведение, DemoSession возвращает true.

Все27 DEMO используют только проверенные opcodes0,1,3,5,10,12,35. Их time=0, skipInput=1: сцена не является автоматически перелистываемым слайдом. Guard10ticks и обязательное отпускание атаки исключают случайный skip от предыдущего боя. Continue использует исходную0086DEMO и собственную двухпунктовую UI-надпись; исходный Windows-menu cursor не воспроизведён.

### Determinism и Android

EFSS включает глобальный frame, route slot/mode, seed, исход предыдущего боя, losses/life/counters, continue input, весь BattleSimulation, StoryRound, KGT overlay и DemoSession. EFDS хранит все concurrent VM, переменные, skip latch/timer. Restore декодирует во временные объекты; повреждённый snapshot не изменяет живое состояние. EFSR сохраняет начальный snapshot и последовательные InputFrame; SHA-256 защищает запись. StoryHistory120 хранит frames/inputs и умеет пересчитывать через границу scene/fight. Effects и музыка не меняют simulation.

Android сохраняет сюжет через AtomicFile при onPause, переходе в VS и перед replay. EFAS1 оборачивает selected pack ID и полный EFSS. При запуске предлагается продолжение. Запись replay после перезапуска начинается с восстановленного snapshot; старая запись не притворяется сохранённой. Ручной snapshot остаётся in-memory. BGM перезапускается при restore; голоса audio не входят в hash.

При смене сцены SoundPool выгружает ненужные packs; callbacks уже выгруженных sounds игнорируются как отменённая загрузка, критическая ошибка актуального sound останавливает тест. Аудио-события старой сцены обрабатываются до смены набора sounds. Отображение DEMO сохраняет4:3 с отдельным местом для верхней полосы управления. Игровые рисунки и текст не перерисованы.

### Реальные проверки

216 изолированных native DEMO-manager cases и1344 original story-round cases получены из неизменённого EXE с Unicorn и сравнены с Java. Это сравнение конкретных процедур с перехваченными внешними вызовами, а не исполнение всей Windows-игры. 27DEMO twin runs —56754ticks, 70687assertions суммарно в StoryDataTest. Все100 боёв проверены по750frames на независимой восстановленной копии; отдельно проверены loss/retry/quit и40tick rollback через границу сцены.

Полный Lucy route:217600frames,10боёв,9побед,ending0202,credits0222. P1 управляется тем же исходным AI (уровень80), противники — исходными уровнями события. Три полных replay и периодические restore дали один hash `99d01a21bdee45b171cf5df51e652ea1f4cd639e472a5f3a551d8333d6936ce0`. Остальные9веток не объявляются полностью пройденными. Регрессия TEST004 match/replay/delayed rollback прошла. 24desktopPNG дают визуальную проверку исходных сцен; это не Android screenshots.

Сборка TEST005 и её извлечённая копия проверяются `tools/package_checkpoint.py`; машинный итог рядом с APK. Установка и новый запуск на Android здесь недоступны: adb/emulator/KVM отсутствуют, AF_UNIX socket возвращает EPERM. Следующий физический этап — PHONE_TEST_005_RU.md, затем устранение обнаруженных различий, остальные сюжетные маршруты и предусмотренный online transport. Хостинг сервера пока не выбирался.


### Исправление OO-ссылок перед выдачей TEST005

Расширенный тест snapshot обнаружил `Unregistered entity reference` в Mayu0110 skill96 pc1059: ссылка numbered objectSlots указывала на уже завершённый объект и переживала повторное использование того же pool index. Ненумерованный объект skill96 ошибочно принимался за предыдущий нумерованный OO, удалял себя при создании skill81, а следующий OOskill99 оставался с недействительным parent. Редкие final-only hashes эту ошибку не обнаруживали.

Оригинальная процедура40e4a0 очищает10numbered links при завершении object-kind1. Это проверено18isolated x86cases. В порте очистка теперь выполняется при завершении/выходе объекта и перед повторным использованием slot. Ссылки parent/controller продолжают следовать существующей модели pool indices. Полное совпадение всех отложенных task1/0 переходов Windows scheduler этим не доказывается.

Добавлен отдельный regression Mayu96 с реальным bytecode, parent/child snapshot/restore. Полный маршрут теперь выполняет StoryHistory.advance и сохраняет каждый из217600кадров; три replay сравнивают271промежуточный hash, включая переходы. Новая корректная ссылка может менять исход конкретного AI-боя относительно TEST004: например, regression match21614frames теперь2:1, раньше1:2. Damage/AI/settings не редактировались; это исправление портированного управления объектами, не изменение баланса.

Combat ruleset revision повышен до2 и включён в identity battle/match/story. Старые snapshots/replays, зависящие от ошибочной версии, явно отклоняются. Бинарная структура EFBSv3 остаётся прежней; ревизия семантики хранится в identity. Итоговый Story hash — `99d01a21bdee45b171cf5df51e652ea1f4cd639e472a5f3a551d8333d6936ce0`.

Окончательная сборка `./gradlew assembleRelease` через Gradle8.9/AGP8.7.3 прошла. Официальный Gradle ZIP отдельно проверен по закреплённому SHA256; медленная первоначальная загрузка не потребовала замены библиотек или отключения TLS. SDK standalone build также прошёл; все2881старыхassets и иконки побайтно сверены.


## TEST 006 — начало 29 сентября 2026

Пользователь сообщил «Всё работает» после выдачи TEST005 и восстановления скачивания. Это подтверждение работы сборки на его телефоне; отдельное прохождение всех десяти маршрутов или каждый lifecycle-тест этим не заявляется. Рабочая копия создана из неизменённого checkpoint005.


### TEST006 — 30 сентября: маршруты и rollback transport

Все29исходных Java-файлов runtime из TEST005 сохранены побайтно; боевой расчёт, AI, команды, damage, gameplay RNG и revision2 не менялись. Добавлен самостоятельный RollbackSession. Исторические разделы выше описывают состояние на дату их записи; актуальные ограничения — KNOWN_ISSUES.md, результаты — TEST_RESULTS_006.md.

Восемь новых полных CPU80маршрутов плюс ранее проверенная Люси: суммарно1454441базовых кадров, ending/credits/COMPLETE, идентичные recorded-input replay с периодическими restore. Марико не объявляется пройденной: CPU80/100 и input-пилоты упирались в лимит500000кадров; лучший пилот достиг6побед/slot13. Ресурсы не подменялись, баланс не менялся. Начальный неполный лог Наны с exit0 не засчитан; повтор прошёл. Harness требует явный PASS полного replay.

RollbackSession хранит120кадров states/inputs, предсказывает максимум18. Каждый input имеет simulation frame и массив внутритактовых переходов. Late input вызывает восстановление earliest dirty frame и resimulation; corrected final KO может закончить матч раньше предсказанного. Подтверждённый префикс попадает в MatchReplay. SHA-256 отправляется каждые30подтверждённых кадров и отдельно для точного финала. DESYNC завершает матч с диагностикой, не исправляет его за счёт импортированного состояния.

Эффекты сохраняются по кадрам и заменяются при пересчёте. Audio/UI получают их ровно один раз после подтверждения; их потребление не меняет battle state. Это добавляет задержку SFX относительно speculative renderer. BGM остаётся presentation-only. Полностью совпали54088проверок двух копий с50/100/150/200мс,jitter,reorder,duplicates; конечный матч и все confirmed effects проверены.

Новый network модуль: bounded binary protocol1, normal TLS1.2/1.3 с hostname verification, отдельные I/O threads и очереди512. OnlineMatch.pump/advance выполняются на simulation thread. Сервер пересылает только input/hash, без игровых данных и Windows. GameConfig содержит общие pack IDs, seed, правила; build identity содержит original hashes и combat revision. SecureRandom используется только для начального seed/кода комнаты вне симуляции. Wall-clock transport timeout не входит в core.

RoomServer — собственный JDK-компонент без сторонних зависимостей. До32комнат/64соединений, bounded packets/queues, READY/initialhash gate, sequential inputs, проверка подтверждаемости hashes и timeout. TLS loopback с теми же Android client classes прошёл:Create/Join,1800кадров и полный904-кадровый матч, canonical replay, disconnect, false content/cert/hostname rejection. Результат уже подтверждённого матча сохраняется при отключении соперника; EOF больше не меняет COMPLETE на FAILED. Конкретный лог/число assertions в TEST_RESULTS_006.md.

Android получил Online Create/Join, рольP1/P2, выбор персонажа/арены/правил, room code и diagnostics. Правила комнаты возвращают в online menu. Проверенная ранее touch/gamepad архитектура сохранена. Пока у подключения нет default endpoint: выбор размещения relay должен сделать пользователь согласно его явному требованию. Никакой сервер и игровые assets не опубликованы. onPause закрывает незавершённый online match; offline autosave из005 остаётся. Финальный online HUD ждёт совпадения hash обоих игроков. Открытая Menu временно останавливает local simulation; общей паузы/переподключения пока нет.

VersionCode6 /0.0.6-rooms; тот же package ID и signing key. Новое разрешение INTERNET, cleartext запрещён. Все2881исходныхassets и story-index из005 сохранены, Lucy icon не изменена. Gradle clean release и SDK standalone прошли, CRC/подпись/assets проверены. Android006 здесь не запускался (нетadb/emulator/KVM), зато настоящий loopbackTCP/TLS доступен. Полная проверка двух телефонов через Интернет ожидает сервер. Сборка и проверка извлечённого checkpoint выполняются package_checkpoint.py; отдельный JSON — итог, не предположение об успешности ZIP.


## TEST007 — original character selection (2026-10-01)

User confirmed TEST006 and deferred online hosting. Restored the final persisted TEST006 ZIP (SHA256 22effcd41686ec393d1966a2687d5b24ba265dbb0b4170b13404a381b38a3ef0); did not use the older transient source copy. A workspace reset during continuation erased initial TEST007 files. Reapplied the in-conversation changes, saved a separate WORKING_CHANGES archive, then reran the tests from restored sources.

Added CharacterSelect, SelectionControls and SelectionLayout. They play original pack0064 background, PLAYER builtin[21] portrait/OO scripts and KGT cursor skills84..87. Full mapping and verified offsets: docs/CHARACTER_SELECTION_007.md. The Android activity now opens this screen for story and local VS, preserves suspended gameplay until confirmation, supports touch focus/drag, gamepad navigation, confirmation release guard and Back. Original10-cell roster; off-grid diagnostic fighters remain accessible separately.

All30 pre-existing runtime classes, network sources, game assets and launcher image sources match final TEST006 (2930 checked files). No combat balance, frame timing, AI, RNG, replay or state schema changed. Selection runs on the existing60Hz accumulator and cannot mutate combat state. Image output remains640x480, toolbars outside; no original asset or Japanese filename changed.

AAPT2 debug packaging recompresses launcher PNGs. Byte mismatch was investigated: source files still match final TEST006; reference encoded PNGs match the original APK inventory, and decoded RGBA pixels are identical. APK checker now accepts re-encoding only for resource PNGs after both checks; game assets still require byte identity. Updated versionCode7/versionName0.0.7-select consistently in Gradle, manifest and validator.

Selection tests cover20321 assertions including1764 original-x86 input frames,160 original wrap cases,10 story intro-to-fight transitions,10 VS mirror starts,7 viewport ratios, release/back, portrait object cleanup and paused-battle isolation. Full Lucy route rerun:217600 frames,10 fights,9 wins,ending/titles;3 replay passes at271 checkpoints retain hash99d01a21bdee45b171cf5df51e652ea1f4cd639e472a5f3a551d8333d6936ce0. Remaining limitations and Android testing boundaries are explicit in TEST_RESULTS_007.md and KNOWN_ISSUES.md.


## 2026-10-01 — TEST007a: аркадный стик и проверка банки Наны

База — финальный TEST007 ZIP SHA2566d51f87eab4e142b6c6da180a4a9af107acadf27f68a8e8619f4e3c1bbb5edb0. Все runtime/network классы, игровые assets, иконки и original EXE сохранены. Это малая итерация TEST007, не TEST008.

Нана: 60native contact cases и960M/integration frames подтверждают прямой урон5, отсутствие chip у банки, ON2→93 при блоке и исходное движение среднего варианта. Шесть реальных командных сценариев в обе стороны + восстановление живого объекта/повторы проверены; отдельная проверка guard→93→90. См. docs/NANA_CAN_007A.md. Изменять баланс или движение оснований нет; никакая combat-правка не вносилась.

Новый UI adapter app/src/main/java/org/elfen/controls/ArcadeStick.java: одно captured pointer ID, круглая dead zone,8направлений, ход рукоятки0.58радиуса базы, cap0.36, зона захвата1.45. Движение продолжается вне круга; другой палец не перехватывает стик. Шесть attack-кнопок по-прежнему независимы. Исторические MotionEvent samples и последняя координата ACTION_UP обрабатываются до отпускания, затем через Input.drain/sample поступают в numbered InputFrame. UI float geometry не входит в simulation. Прежние сохранённые положения/размер/прозрачность/mirror/gamepad-hide используют те же preference keys. Dead zone теперь доля полного хода рукоятки, а не радиуса большого круга.

clearInputs сбрасывает одновременно Input, pointer groups и положение рукоятки. Сброс вызывается при Menu, onPause, потере window focus, CANCEL, смене размера View и переходах режимов; второй палец кнопки не снимает held direction. Gamepad API и simulation60Hz не изменены.

Проверки: ArcadeStickTest7827 assertions; NanaCanTest15156 assertions; replay3×2400покадровых хешей. Полный список — TEST_RESULTS_007A.md. Установка/Android Activity/физические касания в этой среде не проверены.

Пакет org.elfen.fighting.nativeport и подпись прежние. versionCode8 служит только установке обновления; публичное имя TEST007a /0.0.7a-stick. RULESET_REVISION2, сериализация, replay и совместимость TEST005–007 сохранены.


## TEST 007b — офлайн-обновление

База: проверенный CRC и SHA-256 `Elfen_Lied_Android_007a_project(1).zip`, `e6663c1eb62440e766b9f6faea2c71d5dace74febe61e8e7935c8f1df6f66c19`. Новый порт с нуля не создавался. Assets, Script VM, BattleSimulation, CommandRecognizer, CPU, Audio, ArcadeStick и network-классы сохранены. Сравнение — research/source_comparison_007b.json.

### Аудит кнопок и команд

Проверены 14 PLAYER, таблицы команд, JS/opcode36 и guard flags1766/button1753. У десяти основных бойцов маска0x70 (ABC); у Крафта0142/0144 —0xf0 (ABCD), D запускает キャンセラー, skill75; у0154 —0x30 (AB). UI показывает только соответствующие кнопки, так же фильтрует список переназначения геймпада. Низкоуровневые коды A–F сохранены для формата InputFrame/старых записей.

CommandGuide строит только read-only представление исходных Pack.Command. Это не новая таблица боевых действий. Оригинальные названия, порядок шагов и доступные stance сохранены. Стоимость/класс super не выдуманы; условия персонажного скрипта продолжают проверяться runtime. 1668 комбинаций command/stance/facing проходят исходный распознаватель. Сырой аудит — research/command_audit_007b.json.

### InputFrame и пауза

Найден дефект UI адаптера 007a: последовательные Android pointer-down для A/B/C в одном simulation frame создавали samples[16,48,112,112]; исходная проверка свежего нажатия не распознавала ABC, выбиралась обычная C. FrameInput теперь сохраняет промежуточные направления и отпускания, а новые кнопки объединяет в последнем sample одного tick. Шесть порядков трёх pointer-down дают одну исходную ABC-команду. Сохранены rolling motion и короткое нажатие между ticks. Это адаптация событий Android, не увеличение окна команды и не изменение CommandRecognizer. Записанный InputFrame уже содержит окончательный поток; replay/network не зависят от pointer IDs. ArcadeStick геометрия не менялась.

PauseGate принадлежит UI: manual pause, число модальных окон, foreground. В blocked состоянии doFrame не шагает simulation. Открытие/закрытие вложенных окон не снимает ручную паузу. Очистка touch/gamepad pending input и сброс render accumulator не меняют BattleState. Resume начинает следующий simulation frame, без догоняющих ticks. Audio.pause/resume не меняет simulation. Фактические Android callbacks/latency ждут телефона.

### Правила раундов — явно запрошенное изменение

Новые офлайн VS и сюжет используют first-to-two. Ничья не увеличивает счёт; при 1:1 после ничьей повторяется решающий раунд. Число раундов поэтому не ограничено тремя. HP/позиции/таймер и объекты сбрасываются существующим newRound; stock/special переносятся как раньше. Урон, команды, AI, скорости и исходный сюжетный timer не менялись.

Legacy конструкторы MatchRules/StoryController оставлены для оригинальных тестов и старых replay. Новые офлайн MatchRules.decisiveRounds=true. Match snapshot версия2 кодирует эту политику; версия1 остаётся исходной. Mismatch правил явно отклоняется. Story identity содержит `:first-to-two-v1`; вся round/battle/VM/RNG state по-прежнему входит в snapshot/hash.

loadOfflineSave — явный перенос только пользовательского сохранения: сначала валидируется legacy identity и весь snapshot, затем меняется policy identity и состояние проверяется повторно. Текущий кадр, сцены, битовое состояние боя и уже заработанный счёт сохранены. Уже завершённые бои не переигрываются. Если старая версия уже начислила очки за ничью, эти очки сохраняются как часть старого checkpoint; новые ничьи очков не дают. Старые replay не мигрируют в новый ruleset молча.

### Полное прохождение и пределы

Маю, P1 original CPU80, seed19:169804 ticks,10 боёв,9 побед, ending+титры. Это тестовый источник ввода; в приложении P1 остаётся человеком. Три полных replay совпали по193 checkpoints каждый, итоговый hash b8409b34c4e60c75957c5683963416c3e8e7454e9f1f61f90c6eb9c3d35f5a31. Исходный финальный переход после поражения сохранён.

Два bounded-прогона Люси (CPU80/100, seed19,900000 ticks каждый) не завершили маршрут: после пяти побед AI многократно проигрывал бой slot11. Это сохранённый результат, не замаскированный принудительной победой. Он не доказывает непроходимость для человека. Новые правила делают некоторые бои труднее для данного AI. Ранее пройденные девять маршрутов относятся к legacy-правилам; все они заново до двух побед не проверены.


## TEST 007c — цвета управления и виброотклик (2026-10-02)

База — финальный TEST 007b, SHA-256 ZIP `93b3b852aae8d365370e228746ff324fadd4dbdcff3abe1c83a787cc6d78a756`. Из игровых исходников изменён только MainActivity. Все runtime/network-классы, игровые assets, Android-ресурсы, иконка, Audio, ArcadeStick, FrameInput и PauseGate совпадают с базой. Геометрия touch-элементов и зона Start сохранены. Версия 10 / 0.0.7c-controls нужна для установки обновления; прежний package ID и ключ подписи сохранены.

Цвета взяты из предоставленного фото `01-1000026326.webp` как медианы RGB небольших участков: стик #C71B20, A #C43438, B #CAB61B, C #4BDE60, Start #DAD9D5. На удерживаемой кнопке цвет слегка светлеет; прежняя настройка opacity действует на стик и атаки. Буквы удалены из drawControls; белый Start сохраняет только геометрический значок паузы. Дополнительная D остаётся прежнего нейтрального цвета, без буквы: пользователь запросил окраску A/B/C. Фото не добавлялось в игровые assets.

`View.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)` вызывается при ACTION_DOWN / ACTION_POINTER_DOWN на активной атаке или Start. Для атак во время перемещения элементов запрос подавлен. Stick/MOVE/UP/CANCEL/пустая область не запрашивают отклик. Нет периодического вызова при удержании, нет вызовов в simulation/renderer/replay. Haptic API учитывает системную настройку и не требует разрешения VIBRATE: https://developer.android.com/develop/ui/views/haptics/haptic-feedback . Тайминги, длительность и сила эффекта определяются Android/устройством.

Проверка реального обработчика onTouchEvent с JVM-заменами Android UI: 46 событий, совпадение Input samples с извлечённым обработчиком 007b и только ожидаемые haptic callbacks. Это не проверка физического вибромотора. Standalone APK и Gradle release/debug собраны, их подписи/assets/manifest проверены. Итог проверки упаковки и независимой пересборки ZIP — отдельно поставляемый TEST007C_CHECKPOINT_VERIFICATION.json. Новых Android или полных сюжетных прогонов в 007c не заявлено; дальнейших игровых правок эта версия не содержит.


## TEST 007d — исходный HUD здоровья, портретов, таймера и спецприёмов

База восстановлена из неизменённого checkpoint 007c. Введён BattleHud: он читает таблицы KGT 0116 и PLAYER builtin[22], затем строит read-only список отрисовки по BattleView. Рамки, цифры, полосы и портреты взяты из существующих игровых PNG, без перерисовки. KGT-адреса и x86 evidence — docs/ORIGINAL_HUD_007D.md.

Полосы обрезаются без растяжения; native x86 подтвердил целочисленную ширину и противоположные края у P1/P2. Восстановлены расположение цифр таймера, знак бесконечности, количество зарядов, двухфазное мигание meter по исходным инструкциям и отметки выигранных раундов. Пустые оригинальные портретные навыки дополнительных бойцов остаются пустыми.

BattleView.Fighter получил только read-only packId для корректного портрета текущего противника в сюжете/online. Combat, RNG, snapshots и сетевой протокол не меняются; BattleHud не исполняет боевые инструкции. Повторные вызовы, восстановление snapshot и 2400 ticks двух simulation с HUD/без HUD сохранили одинаковые hashes.

В MainActivity прежние временные шкалы заменены оригинальными. Игровой viewport VS теперь оставляет сверху те же 10% под служебную панель, что и сюжет: иначе панель закрывала исходные рамки у y=0. Соотношение 640×480 сохранено. Отрисовка controls, размеры, InputFrame и haptics сохранены побайтно.

Пройдены 2314 HUD-проверок, 56 native x86 clipping cases, 12 timer layouts, загрузка HUD всех 14 PLAYER. Сохранены 9 desktop-preview, это не Android screenshots. Собраны standalone, Gradle release/debug, проверены все игровые assets, CRC, подпись и metadata. Итог упаковки и отдельной пересборки ZIP — TEST007D_CHECKPOINT_VERIFICATION.json, поставляемый рядом с checkpoint.

## TEST 008 — победа финальному боссу и три языка

Восстановлен checkpoint007d; исходный ZIP неизменён. У всех десяти сюжетных программ последний бой — slot18, enemy11 (Крафт), value(5)=0 разрешает старый переход после поражения. По запросу пользователя введена отдельная policy final-victory-v1: проигрыш даёт Continue/retry на том же slot, победа — исходную концовку/титры. Это намеренное отличие progression от прежней интерпретации данных, не изменение боя. Старые constructors оставлены для replay; loadOfflineSave явно мигрирует проверенное сохранение.

Добавлены354 UI keys JP/EN/RU и переводы90 текстовых изображений (95 областей). Японские исходники, PNG/EFP/audio неизменны. LocaleCatalog/TextFit и Android Canvas localisation не меняют simulation/snapshot/hash/RNG. Язык хранится отдельно в preferences. Список, masks, source hashes, ограничения и тесты — docs/LOCALIZATION_008.md. Наложенный текст покрывается панелями только в EN/RU; clean title-card берётся из0064/image2. Продублированные baked варианты Continue скрываются под активным меню.

581 progression checks проходят для10 маршрутов; естественный проигрыш idle Люси0:2 за17242 ticks также даёт Continue.66304 localization checks,27 DEMO twins/56700 ticks,180 sprite previews и186 desktop scene frames. APK собран, но здесь не установлен на Android. Голоса японские; сценические вывески и декоративный псевдотекст не локализованы. Combat, AI, physics, touch/haptics, network, audio и launcher icons сохранены.

Дополнительно проверена естественная победа маленькой Люси0156 над боссом через обычные InputFrame, затем ending/credits/COMPLETE и три совпадающих replay/restore. HP, AI и боевые правила не подменялись. Это тест с входом сразу на slot18, не новый полный маршрут. Результат — research/final_boss_natural_win_008.log. Пробные CPU100 не победили; их отрицательный результат сохранён отдельно, а не засчитан как PASS.

## RC1: title, pause and in-memory continuation (2026-10-03)

- Base is the verified TEST008 archive, not the stale scratch directory. Engine, AI, collision, HUD assets, scene translations, audio backend and battle touch branch are unchanged; see `research/preservation_rc1.json`.
- Original title positively identified: KGT type-specific `originalTail` offset 58412 (`0xE42C`) contains the first screen selector, value 7. `StoryCatalog.screen(0)` resolves DEMO 7 to pack `0074`, original `デモ 2.demo`. Its initial VM view emits background image1 and title image11. Image11 is 640x480; BGM=0. The title uses the original logo unchanged; interactive localized New Game/Continue replaces the baked STORY/VS labels. The original file is not edited. Settings/Language are buttons below the original frame. Artwork retains its ratio.
- `SessionFlow` holds only in-memory navigation/session availability. Cold onCreate does not instantiate a match. English is selected once on migration from TEST008 or first RC1 launch; subsequent explicit language selections persist.
- Removed startup tutorial/save dialog, TEST header, character/command/input hex overlay, debug snapshot/replay/report menu entries and hitbox UI. Internal snapshots, deterministic history and recording remain intact. Runtime errors still halt play and write a diagnostic; normal player UI shows a localized failure message, not internal data.
- Fixed pause navigation: `DialogRoutes` associates one explicit after-dismiss action with each dialog. The old globally posted onDismiss → pauseMenu fallback is gone. All release-menu transitions run after the current modal pause token is released. Back from a submenu returns to pause; Resume and Back from pause resume exactly once.
- Lifecycle invalidates pending dialog actions before dismissing windows. onPause requests pause, clears all pointers/pending input, stops frame callbacks and audio, resets clocks, and returns UI to title. onResume does not clear pause or start battle audio. Continue explicitly resumes the same controllers. Focus loss pauses; returning focus requests pause menu without resuming implicitly.
- Disk `story-save.bin` writing/reading is removed from the Activity; the old AtomicFile and its backup are deleted on cold launch. Destroy clears references; process death naturally loses the session. This intentional policy change implements the user's requested distinction between backgrounding and unloading. Language/control preferences remain persisted. Engine-level snapshot/legacy-load APIs remain available for regression tests and future netcode.
- No balance, timing, resource-renaming, AI or story policy changes. Final-boss victory gate remains TEST008's `final-victory-v1`. Existing room transport is retained; no server was deployed.
- `test_release_flow.py` executes 22 production Activity methods with queued-dialog/lifecycle fakes and actual battle/story controllers. This checks races and state preservation; it is not an Android framework/device test. Physical acceptance is documented in README_RC1_RU.md.


## 2026-10-04 — RC1 build 14: control margins and release preparation

CombatLayout defines one geometry for drawing and touch hit testing. It preserves the old viewport on 2048×945 and reserves side gutters on narrower displays. The arena stays 4:3. Full 1.12-radius button hit targets, captions and the stick stay outside the arena; six-button space is reserved so A/B/C do not jump when a character changes. Mirror, size and custom positions are clamped into the available gutters. A/B/C captions are below their coloured buttons.

Production differences from private RC1 are limited to five Activity layout/draw methods, a layout field and the new presentation-only CombatLayout class. The actual touch handler, runtime, AI, damage, audio, lifecycle, network and game assets are unchanged. This patch does not change simulation timing or input bit meanings.

VersionCode is 14; versionName remains 1.0.0-rc1. Public source build recipes now obtain signing configuration solely from ELFEN_* environment variables. No private keystore is included in the public tree or source checkpoint. The pre-existing private key is used only to sign the delivered upgrade APK.

Public GitHub publication of source/APK was explicitly requested in this iteration. No room-server deployment is authorized by that request. The release remains a prerelease because full original-runtime fidelity and real internet play are not established. Current verification is recorded in TEST_RESULTS_RC1A.md; historical tests are not silently promoted to new hardware acceptance.
