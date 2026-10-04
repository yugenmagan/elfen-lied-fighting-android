# Форматы Elfen Lied Fighting

**Актуальный статус — TEST 008, 2026-10-03 (Москва).** Ниже сохранена история исследования: ограничения ранних разделов TEST002/003 не описывают целиком текущий runtime. Последние разделы фиксируют реализованные combat/match/story структуры; остающиеся ограничения — KNOWN_ISSUES.md.

Это описание установленной структуры, **не полная спецификация исполнения 2DFM2nd**. Числа little-endian, строки CP932 до первого NUL внутри фиксированного поля. Данные после NUL не следует трактовать как текст: встречаются остатки прежних значений.

Основной ориентир: исходники [Xem85/fm2ndparser](https://github.com/xem85/fm2ndparser/tree/7266d65b9ca486a619b6b10836ee0cfed734fefb), MIT. Описанные ниже границы разделов проверены собственным инспектором на 46 уникальных контейнерах приложенного архива. Семантика неподтверждённых полей остаётся неизвестной.

## Общая часть KGT / PLAYER / STAGE / DEMO

| Offset / размер | Содержимое |
|---|---|
| 0x0000 / 12 | сигнатура; у всех файлов начинается с `2DKGT2G`, затем нули |
| 0x000C / 4 | флаг: в данном архиве 1 для PLAYER, 0 для остальных; точный смысл не установлен |
| 0x0010 / 256 | внутреннее имя CP932 |
| 0x0110 / 4 | число навыков/скриптов |
| далее / N×39 | таблица навыков |
| далее / 4 | число 16-байтовых инструкций |
| далее / M×16 | массив инструкций |
| далее / 4 | число image slots, здесь 8192 во всех контейнерах |
| далее / переменный | записи изображений и их данные |
| далее / 8×1056 | восемь записей глобальных палитр |
| далее / 4 | число звуковых записей |
| далее / переменный | звуковые записи и встроенные данные |
| далее | расширение соответствующего типа файла |

Навык, 39 байт: имя[32], uint16 начала в массиве инструкций, byte, uint32 типа. Последняя запись используется найденным парсером как завершающая граница. Пустые записи и их индексы сохраняются.

Инструкция занимает 16 байт. Byte 0 — opcode. Остальные 15 байт зависят от opcode. Сохранены полностью в `blocks_hex`; неподтверждённые байты не интерпретируются как нули.

По найденному открытому парсеру группы инструкций относятся к движению, картинкам/анимации, звуку, объектам, ветвлению, столкновениям/атакам, переменным и вводу. В дальнейшем установлено: opcode с именем AI означает afterimage (след изображения), не искусственный интеллект противника. Это полезные подсказки для восстановления runtime, но они не подтверждают порядок исполнения, fixed-point единицы, hitstop и точные frame timings в оригинале.

В данных найдены opcode 0,1,2,3,4,5,9,10,12,14,16,20,21,22,23,24,25,26,30,31,35,36,37. Opcode 22 отсутствует среди обработчиков открытого парсера; его обработчик установлен в EXE (см. дополнение ниже). Три появления есть в `にゅう.player`, block indices 272, 296, 400. Raw bytes записаны отдельно в `research/unresolved_opcodes.json`.

## Изображения и сжатие

Запись изображения: uint32 pointer/reserved, uint32 width, uint32 height, uint32 palette_type, uint32 packed_size, затем payload. Сохранённые pointer-поля иногда отличаются между версиями при одинаковых пикселях: они не являются offsets файла.

Palette type 0 использует общую палитру, type 1 содержит 1024 байта индивидуальной палитры в начале распакованных данных. За ней идут width×height байт индексированных пикселей. У пустого slot нет payload. Если packed_size=0 и размеры ненулевые, данные хранятся без сжатия.

Сжатие — поток четырёх видов команд, не ZIP/zlib:

- верхние два бита управляющего байта задают режим: нулевые байты, literal, повтор одного байта, обратная ссылка;
- младшие шесть бит задают длину;
- при нулевой длине следующий ненулевой байт означает длину `byte + 63`;
- если и следующий байт нулевой, длина равна `uint24_le + 319`;
- обратная ссылка использует 8-битную дистанцию; нулевой байт переводит её в расширенную форму `(следующий_byte + 1) << 8`, после которой есть ещё один пропускаемый байт согласно обоим найденным декодерам;
- перекрывающиеся обратные ссылки копируются последовательно.

Собственный декодер требует полного потребления упакованных данных и точного выходного размера; оборванный поток, выход за границы и ссылка до начала — ошибки. Все непустые изображения архива прошли эти проверки. Это подтверждает декодирование данного набора, но не всех теоретически возможных файлов 2DFM2nd.

Первые 1024 байта записи общей палитры содержат 256 четырёхбайтовых цветов. Порядок каналов — B,G,R,служебный байт. Указание внешнего парсера на alpha оказалось недостаточным: оригинальный runtime определяет прозрачность по нулевому RGB, а четвёртый байт не является обычной PNG alpha. Индекс 0 сам по себе не прозрачный. Android PNG-кэш следует RGB-правилу; все восемь палитр сохранены. Семантика дополнительных 32 байт и полный compositing ещё требуют исследования.

## Звук

Заголовок: uint32 pointer/reserved, name[32], uint32 payload_size, byte flags, byte CD track, затем payload_size байт.

Младшие 2 бита flags — вид ресурса согласно открытому парсеру, bit 5 — loop. В данном архиве непустые type 1 начинаются с RIFF, type 2 — с MThd. Проверены границы и контрольные суммы данных. Воспроизведение, задержка, looping и pause/resume не проверялись.

## .kgt

После общей части: 4 байта; 50 имён персонажей по 256 байт; 200 записей hit junction по 36 байт; uint32; byte; три byte параметров stiff time; 50 имён stages по 256; 100 имён demos по 256; шесть byte выбора экранов. Пустые slots нельзя удалять: references индексируются по таблице.

Далее располагаются глобальные настройки, common images, ссылки на встроенные навыки меню/таймера/KO и параметры выбора персонажей. В нашем инспекторе эта часть KGT пока сохранена как неразобранный хвост; его ненулевое содержимое явно отмечается. Не утверждается, что KGT разобран полностью.

Offsets для предоставленного KGT:

| Раздел | Offset, десятичный |
|---|---:|
| skills count | 272 |
| blocks count | 4527 |
| images count | 9155 |
| palettes | 3340838 |
| sounds count | 3349286 |
| type-specific data | 4128634 |

12 непустых character references, 4 stage references и 27 demo references. Все имена найдены в архиве после восстановления кодировок. Screen selectors: title=7, p1_cpu=8, p1_p2=8, team=0, game_over=1, opening=25. Их фактическая работа не проверена запуском.

## .player

Общая часть содержит спрайты, звуки и скрипты движений/атак. За ней:

1. 4 байта и uint32 количества команд.
2. Записи команд по 82 байта: имя[32], uint16 времени, четыре uint16 ссылки на навыки, десять двухбайтовых input steps, десять uint16 параметров длительности.
3. Счётчик и пары uint16 hit-junction references.
4. Счётчик и 6-байтовые записи common images.
5. 10 байт, 100 CPU slots по 111 байт.
6. 24 uint16 встроенных навыков, 38 байт, 1785 байт настроек персонажа.
7. Сто story slots переменного суммарного размера, затем нулевой хвост.

Пустой story slot занимает один byte 0. Непустой занимает 206 байт: type byte и payload[205]. Type 1 — бой, 2 — demo, 3 — условный переход, 4 — конец. У demo первые два байта payload задают индекс сцены; у боя первый byte задаёт stage; все такие ссылки проверены по KGT. CPU-параметры противников внутри fight payload сохранены без потери, но не представлены как проверенная модель поведения.

Внутренние названия команд не обязательно равны названиям из руководства. Нельзя подменять ими публичную локализацию без сопоставления.

Варианты Марико содержат одинаковые навыки/пиксели/звук, но различные commands. Варианты Крафта различаются числом блоков и скриптами. Простая дедупликация по восстановленному имени теряет данные.

## .stage

Общая часть, затем 4 байта, uint16 индекса BGM и нулевой хвост. Скрипты слоёв находятся в общей таблице навыков/инструкций, а не в отдельном PNG фона. Все четыре уникальных контейнера прочитаны; ссылки BGM находятся в пределах таблиц.

## .demo

Общая часть, затем 4 байта, uint16 индекса BGM, uint16 skip-input, byte, uint32 времени и нулевой хвост. Единица измерения времени не подтверждена запуском.

Demo содержит навыки/инструкции, графические слои, реплики в изображениях, музыку и переходы. Это не готовый видеофайл. Нельзя заменить проигрыватель DEMO слайд-шоу и считать поведение сохранённым.

Визуально подтверждены японские растровые реплики в `マリコＯＰ.demo`. Для будущей русской локализации потребуется распознавание/перевод текстовых картинок, согласованная замена ресурсов и проверка timing. Исходные картинки сохранены; переводы не подставлялись.

## Что пока неизвестно

Точная семантика каждой инструкции, порядок обновления объектов, collision resolution, frame timings, hitstop/stun/knockback, скрытые особенности AI, mapping глобальных настроек KGT, условия всех branch-команд, оригинальный compositing и поведение lifecycle. Статический разбор не является доказательством эквивалентности собственного runtime.


## Дополнение 2026-09-21: исполнение оригинального x86

Источник адресов — предоставленный `source_original/entries/0186.exe`, image base 0x400000; дубликат EXE идентичен. Адреса относятся только к этому файлу. `tools/probe_x86.py` загружает PE-образ в Unicorn и исполняет отдельные обработчики с синтетической памятью. Windows API и игра целиком не запускаются. 29 воспроизводимых случаев прошли; результаты в `research/x86_probe_results.json`.

### Модель task и движение

| Поле task | Тип | Значение |
|---|---|---|
| +0x08, +0x0c | int32 | x, y в 16.16 |
| +0x18, +0x1c | int32 | vx, vy |
| +0x20, +0x24 | int32 | ax, ay |
| +0x28 | uint32 | flags, в том числе 0x20000000 parent follow |
| +0x3c | int32 | счётчик ожидания I |
| +0x40 | int32 | состояние freeze, точное общее расписание ещё не восстановлено |
| +0x5c bit 0 | byte | направление отражения |
| +0x12d, +0x12f | int16 | parent-relative pixel offsets |
| +0x17a | pointer | parent task |

Установка скорости 0x406450/0x406478–0x4064ae: `c = speed > 10 ? speed*10 : 50+5*speed`; timing=c, velocity scale=floor(65536/c), acceleration scale=floor(3932160/c²). Для исходного setting 10: 100, 655, 393. Проверены settings 0, 5, 10, 15, 20. Это не полная спецификация частоты общего игрового цикла.

Интеграция 0x40f96a–0x40f992: сначала vx+=ax, x+=vx, затем vy+=ay, y+=vy. Арифметика 32-битная с переполнением. Прежний порядок в нативной реализации был исправлен. После движения 0x40f992–0x40f9dc при flag 0x20000000 заменяет позицию на parent.x ± (offsetX<<16), parent.y + (offsetY<<16); знак зависит от направления родителя. Блок не меняет собственный facing дочернего объекта и не проверяет его Java-флаг ended. Проверены оба направления и переполнение координаты.

### Байткод, диспетчер и кадры

Инструкции остаются 16-байтовыми little-endian. Диспетчер 0x4125ec использует byte map 0x413c74 и таблицу обработчиков 0x413c08. Opcode 0 — метаданные. Следующий skill start определяет неявную границу навыка. Метаданные без показа изображения не должны создавать бесконечный цикл.

**M / opcode 1**, обработчик 0x41282d:

| Bytes | Поле |
|---|---|
| 1..2 | signed horizontal acceleration |
| 3..4 | signed horizontal velocity |
| 5..6 | signed vertical velocity |
| 7..8 | signed vertical acceleration |
| 9 | flags |

bit 0 — add. bits 1/2/3/4 — сохранить текущий vx/vy/ax/ay соответственно, не «остановить» компоненту. Без preserve: новое значение = operand × scale (и facing sign по X), плюс прежнее при add. Проверены 14 комбинаций flags/facing в оригинальном обработчике.

**I / opcode 12**, 0x4127e3: uint16 wait@1; uint16 image@3 (bits 0..12 index, bit14 X flip, bit15 Y flip); int16 x/y offset@5/@7; byte options@9, bit0 ignore facing. В начале interpreter pass счётчик уменьшается на 100; при результате >=0 исполнение откладывается. I с wait=0 задаёт -1 (бессрочно), иначе добавляет wait*timing. I всегда завершает текущий pass. Проверены wait 0/1/5/15/325. Точное взаимодействие global freeze, task lifetime и общего frame scheduler ещё не восстановлено.

### Создание объектов

**O / opcode 4**, обработчик 0x412a7b. flags@1; target skill u16@2, block@4; существующий slot branch u16@5, block@7; int16 x/y@8/@10; slot@12; explicit depth@13.

- flags bits0..1: 0 — depth родителя -1, минимум10; 1 — +1, максимум127; 2 — явная depth.
- bit2: без проверки общего слота; иначе используется один из десяти слотов контролирующего персонажа.
- Если слот жив и branch@5 ненулевой, ветвится сам вызывающий task; новый объект не создаётся. Если branch нулевой, прежний объект заменяется.
- bit5: follow parent; bit6: абсолютные экранные координаты. Иначе координаты относительны родителю и X отражается.
- bit3 связан с тенью; полная семантика ещё не реализована.

Ошибочное прежнее понимание bit0 как OUT-операции отменено. Slot array и character variables разделяются объектами одного персонажа.

### Условия, переменные и gauges

**Opcode 22**: flags@1 bit0 инвертирует результат, bit1 задаёт false до инверсии; target skill u16@2/block@4; condition@7. Условия: 1 ground, 2 ground и не down, 3 ground и down, 4 forward, 5 back, 6 up, 7 down, 8 neutral. Дополнительный player flag8 в back-ветке ещё требует привязки. В Ню некоторые targets 158/160 выходят за таблицу из 153 навыков при определённых ветках. Не исправлены наугад; они не посещаются 99 проверенными обычными путями. При фактическом переходе runtime выдаст адрес ошибки.

**Opcode 31**, 0x4137a4: target u16@1/block@3, var index@4, flags@5, source variable@6 или int16 literal@7; int16 comparison@9. indices0..15 — task, 64..79 — character,128..143 — system. flags low2: 1 присвоить, 2 прибавить с ограничением ±30000, 3 без изменения; bit7 выбирает variable source. flags bits2..3: 1 equal,2 strictly greater,3 strictly less. Сравнения не включают равенство для greater/less. Специальные источники192/193 — x/y,194/195 — камера,196/197 — parent x/y,198 — время,199 — раунды. Android host binding пока реализован только для192/193.

**GS / opcode16**, 0x413500: target u16@2/block@4; mode bit0@5; threshold@6; signed delta@7. mode1: при stocks<=threshold ветвление, иначе изменение stocks с clamp0..max. mode0: ветвление при stocks>=threshold. Target0 обращается к штатному built-in selector 0x410060; анимационный preview пока завершает действие, этот fallback не является точным боевым поведением.

**Opcode21** читает изменения life/special обоих участников; текущий host только хранит gauges. Life исходно читается из player settings offset1754 как int32 (например, Люси400). Формула урона по FA power не установлена: выводить damage напрямую из одного байта запрещено.

### Boxes и незавершённые обработчики

Opcode24 FA /25 FE: x/y int16@1/@3, width/height@5/@7, slot@9, до20slots. Нулевой размер снимает box. Остальные attack fields сохраняются. Opcode23 задаёт шесть hit-junction references u16@1/3/5/7/9/11. Collision/damage/hitstun/knockback/guard не реализованы; debug rectangles — только визуализация данных.

Opcode26 PS: freeze; взаимодействие с противником и global stop не завершено. Opcode30 cancel и36 command branch сохраняются, но боевые переходы по ним не реализованы. Opcode37 AI — **afterimage**; параметры сохраняются без вывода следа. Opcode14 EB (fade/shake) и20 RP (common response/spawn) ещё приводят к явной остановке теста. Это ограничения итерации, не пустые замены ресурсов.

### Палитра и colour instruction

RGB-zero прозрачность видна в 0x40d5b5/0x40c2af. Для opcode35: mode@1; signed R/G/B@2/3/4 прибавляются к 5-битным компонентам с clamp0..31. Если непрозрачный цвет стал полностью нулевым, сохраняется минимальный ненулевой blue. Mode0 normal,1 alpha50%,2 add,3 subtract,4 custom alpha. В custom signed byte5 задаёт destination weight A/32, source weight (32-A)/32. Нативный Canvas пока не воспроизводит весь оригинальный integer compositing; subtract останавливает тест. Записи offscreen effect и camera ordering требуют дальнейшей сверки.

### Переносимый EFP1 (формат этого проекта)

Это производный формат, не новая расшифровка исходной сигнатуры. Int32 и длины big-endian (Java DataInputStream), строки UTF-8 с длиной int32, blobs с длиной int32. Порядок:

1. magic EFP1; title/kind/sourceSHA256 strings.
2. Skills count; для каждого name string/start int32/type int32.
3. Исходный bytecode blob без изменений.
4. Images count; пары width/height int32, слоты сохраняются.
5. Sounds count; filename string/flags int32, пустые slots сохраняются.
6. Builtin indices array (count+int32); commands count; каждая command: name,time,skills array,20-byte steps blob,amounts array.
7. Hit junction count; пары int32; player settings blob.
8. BGM, demo time, skip input int32; полный исходный type-specific tail blob.

PNG — кэш palette0. `indexed.zlib` содержит все8×1056 байт палитр, затем для каждого из8192slots big-endian blob length и исходную распакованную image payload. Private palette остаётся префиксом1024байта внутри blob; размеры доступны в EFP. WAV/MIDI сохраняют исходные payload bytes. Служебные pointer-поля оригинального файла не превращаются в Android-адреса.

`catalog.json` связывает IDs и SHA-256; `filename-map.json` связывает все исходные raw names с Unicode и реальными pack paths. CP932 используется только при конвертации. Проигрывание DEMO и story events по сохранённым хвостам ещё предстоит реализовать.

## Verified combat fragments (2026-09-22)

`tools/probe_combat_x86.py` исполняет 0x40f010 целиком на синтетических task records и реальной таблице реакций Наны; Windows API не вызываются. JSON хранит каждый input/output. Уменьшение HP не подменено заглушкой. Матрица отдельно исполняет участок damage 0x40f786–0x40f7f9 и CRT rand 0x417a22.

FA/FD: x,y @1/3 signed16 — центр относительно персонажа; w,h @5/7 signed16 — половины размеров. Позиция 16.16 переводится в pixel делением с округлением к нулю. Строгое пересечение: касание границ не удар. FD flags @10: bit0 solid/push, bit1 damage, bit2 additional hit/throw eligibility; byte11 damage rate. FA flags @10: bit0 hit cancel eligibility, bit1 rearm (также при истечении I), bit2 chip, bit3 ignore guard, bit4 ignore grounded, bit5 ignore airborne, bit6 unblockable, bit7 ignore already hit. Power @12.

Damage = max(1, max(1,power-trunc(power*comboCount*attackerComboCorrection/100))*hurtRate/100). Power0 обрабатывается отдельно: нет damage/hitstop/combo increment, но реакционный переход возможен. Chip = max(1,power*attackerChipRatio/100). Последний минимум проверен для unsigned byte inputs. Signed32 overflow и division toward zero сохраняются. Life routine 0x40e7c0 дополнительно применяет correction при HP <= maxHP*threshold/100; у проверенных персонажей threshold0. Meter helper 0x40e6f0 переносит излишек в stocks и занимает stock при отрицательном meter.

R23 содержит 6 little-endian u16 junction IDs @1/3/5/7/9/11: hit stand/crouch/air, guard stand/crouch/air. PLAYER reaction берётся у жертвы, spark — у атакующего. KGT junction flag1 меняет guard outcome на hit. KGT tail: 4 reserved +50*256 names +200*(32-byte name +u32 flags) +u32 +u8 + hit/guard/clash delay bytes. В игре 7/9/6.

Task stride0x17e, pool0x4701e0. x/y @8/12; velocity @0x18/1c; accel @20/24; pending reaction @38; freeze40; ground58; facing5c; triggers64..78; FA pointers89..d5, FD d9..125, R129; controller index156; kind15a; flags15e. Flag16 prevents another hit until rearm; hit8, action4, guard12; stance low2 bits. Character stride0xe03f at0x4d1d80: main task def5, lastContact def9, lastAttacker defd, combo df01, HP df05, meter df1d, stocks df15. Settings1785: chip1749, lowHPthreshold1750, lowHPcorrection1751, combo1752, guard button1753, life1754, meterMax1758, stockMax1762, guardFlags1766, own meter1774, received meter1776, startStock1778.

CRT RNG state @0x41fb1c: state = state*214013+2531011 modulo2^32; return(state>>16)&32767. 4 seeds ×64 draws verified. Renderer must not consume battle RNG; native effect stream remains outside gameplay mutations.

Original scheduler 0x404d4c: depth-sorted scripts, then 0x40ffc0: clash40eb60, attack40f010, movement/solid40f910. Pending reactions are started before checking freeze and execute their first image immediately. Subsequent freeze ticks decrement without VM execution. Physics then checks current freeze. Image timeout scans FA rearm flag2. At END, character dispatch executes in the same interpreter pass; objects end.

CPU411270 reads 100*111 PLAYER records and synthesizes the original command-history ring, including historical charge inputs. This is an actual original AI privilege, not ordinary real-time button timing. Its faithful representation in unified InputFrame is under implementation; do not replace it with random attacks or direct skill selection.

## Command/CPU/contact refinements — TEST003

Command matcher0x410060:1024input cells, newest first; captured facing for auto-turn, match-time mirror in manual guard mode. Priority follows original record order. Shared c.time budget across steps, press edge/mash/charge/rotation modes; long recognized history (>29cells) clears ages20..1023. Original returns skill and stores one-based command atchar+df55. GS insufficient-stock fallback resumes from this one-based value as the zero-based scan start, skipping the previous command.1068x86 cases include actual3player commands and synthetic direction/mode combinations; matching every stored history cell is tested. Multi-touch events within one simulation tick retain order without increasing charge time.

CPU111bytes: probabilityu8@33, flags@32 selfAirbit0/enemyAirbit1, distanceu16@34/@36.100records checked in order, last eligible wins. Steps begin@41,stride7: held directionbyte1, activeflagbyte2&32, one-basedcommandu16@3,durationu16@5. Statepattern1-based/step/remaining/cooldown and CRT RNG serialized. Exact procedure reproduced from0x411270,12×120sequentialx86steps. CPU command output is the same1024-cell history format used by matcher.

Nana atdistance259ground/ground can select CPU records4(range100..401,呪い中),10(60..281,強→パパたすけて),22(0..500,ぜんだんはっしゃ),27(0..300,中→強→爆砕剣). All held directions0. At zero stocks and this spacing, seed19mirror remains active without further damage. This is an observed port scenario and data fact, not a verified full Windows match replay.

FA/FA routine0x40eb60: slot-order entities, descending19..0boxslots, same-plane/enemy checks; bothpower0 clear both FA sets. ON5/clash usespending reaction and clears both FA sets. KGTflagsu8tail58420=13, mask2=0: global clash freeze/spark disabled in these game data.64isolatedx86 cases compared.

Landing0x4118b4(character)/0x412463(object) switches skill+block, wait0, clears ON1 and FA/FD only; retains R, other triggers and VM context. Resetting R breaks Lucy117 after touchdown. Distinct forced pending reaction0x412327 clears more state.

Original0x40f635/0x40f657 handles missing mapped reaction / absent-or-zero R entry: debug `reaction error 1` / `reaction error 2`, then0x40f8bf (skip damage/freeze). Debug logger0x415190 returns immediately if debug disabled. Port reports the same branch, retains already-set contact flags; no invented fallback. Anna88blocks915+ explicitly zero three guard junctions. Fixture includes both absent R and zero selected guard entry.

## KGT match metadata и форматы TEST004

- `originalTail` KGT имеет базу Windows runtime0x435470. Системные animation IDs — little-endian u16 по адресам4451c2 intro,4451c4 outro,4451c6 ROUND1 (затем +2 до4451d8),4451da FIGHT,4451dc KO,4451de PERFECT,4451e0 YOUWIN,4451e2 YOULOSE,4451e4 P1WIN,4451e6 P2WIN,4451e8 DRAW,4451ea DOUBLEKO.
- Стартовый16-byte block skill opcode0: u16 offset1 — ожидание контроллера, независимо от суммы I waits. Проверено по4068e0 и используемым metadata.
- В данном KGT intro/outro200ticks, ROUND1–3 250, FIGHT50, KO/PERFECT200, P1/P2WIN300, DRAW/DOUBLEKO100. Значения считываются из данных, не зашиты в RoundController.
- `EFMS`0x45464d53 v1: UTF asset identity, четыре int rules, int global frame, восемь int RoundController, length-prefixed battle snapshot, serialized system animation VM pool. Big-endian, без Java object serialization, pointer indices вместо адресов. Конкретный порядок — MatchController/MatchOverlay write methods.
- `EFMR`0x45464d52 v1: firstFrame, initial EFMS blob, frame count, последовательные InputFrame samples и optional CPU histories; SHA256 всех предшествующих байтов. Не совместим с одиночным combat EFRP: различная magic намеренная.
- Hash state не включает музыку, Android lifecycle или wall-clock. Все значения, влияющие на дальнейшую симуляцию реализованного матча, находятся в EFMS. Ring buffer — производная history, а не часть snapshot.


## Оригинальные story / DEMO структуры — TEST005

### PLAYER story records

После первых4байт originalTail: u32 count команд +count×82; u32 count junction +count×4; u32 common images +count×6; затем10+11100CPU+48builtins+38+1785settings. Далее100 events. Каждый начинается type:u8; у type0 payload отсутствует, остальные имеют205байт. В памяти оригинала expanded record206bytes, base0x4d9a49. Все offsets ниже от type-byte.

| Offset | Размер | Назначение, подтверждённое для используемых событий |
|---|---|---|
| 0 | u8 | type1fight,2demo,3condition,4end |
| 1 | u8 | ссылка на stage или DEMO, one-based |
| 2 | u8 | побед для завершения боя |
| 3/4 | u8/u8 | режим восстановления HP / процент |
| 5 | u8 | bit0 запрос continue после поражения; у финального боя Люси0 |
| 6 | u16LE | время;0 означает бесконечное; runtime=time×100−1 |
| 8 | u16LE | позиция P1 x |
| 12 | u8 | presentation flags; все используемые7 |
| 18/19 | u8/u8 | получатель/очки при поражении P1 |
| 28 | u8 | первый противник, one-based; до7descriptors с шагом26 |
| 29 | u8 | CPU первого противника |
| 31 | u16LE | x первого противника |
| 38/39 | u8/u8 | получатель/очки при его поражении |

Dispatcher4069b0 увеличивает slot, пропускает нулевые ссылки, запускает task14/16. Type3: условие record1 (0always,1previousResult==1,2life<record2,3noLoss), signed record5 — относительный переход. Эти type3 отсутствуют в предоставленных10маршрутах; не приписывать им проверку полным игровым прохождением.

### KGT ссылки и EFSI1

Runtime-tail: reserved4,50имён персонажей по256,200records по36,8bytes,50имён сцен по256,100имёнDEMO по256,6selectors экранов. Original strings CP932, filenames должны совпадать с filename-map; пустые записи сохраняют индексы. Экранные selectors данных `[7,8,8,0,1,25]`, continue selector4→DEMO1→0086.43непустые ссылки:12character+4stage+27demo. `story-index.tsv` EFSI1 хранит KGT SHA256, kind,index,pack ID,source SHA256,декодированное имя. Не переименовывает исходники.

### DEMO tasks

Manager406c10; root loader406790. Именованные skills1..count−2 с opcode12 образуют независимые scripts. Type3 переключает depth12→100, typebit32 запрещает natural EOF loop. Явный E прекращает non-character task (412617). DEMO metadata time/skip уже сохранены в EFP1 и теперь используются: во всех27сценах time0,skip1. Guard10frames, затем release latch и attackmask0x3f0. Phase0 создаёт tasks,1active,2exit-requested,3finished. Визуальные tasks исполняются до depth127manager, включая последнийtickphase2.216native-managerfixtures проверяют управление;1344story-roundfixtures проверяют single-story ветку4086a0.

### Форматы сохранений порта

Все целые big-endian через DataInput/DataOutput; original payload сохраняет свой исходный порядок. Никаких Java serialized objects/адресов памяти.

- EFDS (`0x45464453`) v1: pack hash,frame,phase,guard,remaining,released,16globals,VM count,каждые16charVars+полныйScript state.
- EFSS (`0x45465353`) v1: identity(KGT/player hash/CPU),frame,slot,mode,seed,previousResult,losses,lastLife,fights,wins,continueChoice,lastMenuInput; optionalStoryRound+Battle snapshot+MatchOverlay; optionalDEMO packID+EFDS. Конкретные bounds проверяются в StoryController.restore.
- EFSR (`0x45465352`) v1: initial frame/snapshot,числоInputs,ordered InputFrame including CPU histories,заключительныйSHA256. Лимиты1millionframes/32MBpayload; initial snapshot<=20MB. Формат отдельный от EFMR.
- EFAS (`0x45464153`) v1: Android autosave wrapper,UTF selected packID,length+EFSS. AtomicFile сохраняет прежнюю копию при незавершённой записи. Audio playback position и Android lifecycle не входят в state/hash.

100используемых боевых events имеют одного противника. Другие team layouts запрещены явной ошибкой, не сводятся молча к одному бойцу.


### Ревизия combat2 и OO lifetime

Original40e4a0 для task kind1 очищает все10object pointers по character+0xdfbf, совпадающие с удаляемым task. Задача помечается type1;4069a0 позднее переводит её в0, allocator406570 использует свободный0. Android сохраняет прежнюю модель Script/slot, но сбрасывает numbered links при завершении и перед повторным выделением.18nativefixtures — research/object_cleanup_x86_005.json. Отложенные task1/0 scheduler transitions не объявляются полностью эквивалентными.

RULESET_REVISION=2 включён в battle/match/story identity. EFBS/EFMS/EFSS binary-layout versions не менялись; restore отвергает identity предыдущей семантики, чтобы не принимать старые записанные бои как совместимые после исправления stale OO references.


## TEST006: форматы исходной игры не изменены

Парсеры .kgt/.player/.stage/.demo, pack hashes и CP932 filename mapping сохранены из TEST005. Все29runtime Java sources, кроме добавленного RollbackSession, побайтно прежние. Исходные поля gameplay не заменены сетевыми данными. Battle/Match/Story snapshots и replay revision2 совместимы с TEST005.

Сетевой формат EFNET1 — новый формат порта, не обнаруженная структура Windows-игры. Полная схема frame-numbered INPUT/HASH/CONFIG, bounded strings и TLS framing описана в docs/NETWORK_PROTOCOL.md. Никакие images/audio/PLAYER/STAGE/DEMO или сериализованные battle snapshots не пересылаются relay. Каждый клиент исполняет локальные original packs, получая только configuration и InputFrame другого игрока.


## TEST007: character selection records

KGT tail is mapped at VA435470. Grid layout at4452b0..4452ba is six signed16-bit values:218,50,150,83,2,5. P1/P2 portrait positions at4452bc/be and4452c4/c6 are70,350 and560,350. Availability table4452cc has50 bytes, bit1 story and bit2 VS. Cursor script references445250/252/254/256 are unsigned16-bit84/85/86/87. Row-major cells resolve the first10 original character filename slots through the CP932-checked StoryCatalog, not a renamed asset list.

Story/VS screen selectors resolve DEMO#8, pack0064 キャラセレ.demo. The backdrop includes the original small portrait grid. Each PLAYER builtin[21] refers to its large selection composition, including OO children and original name art. Native input414770..414829 repeats after50 ticks then every5; grid helper406e70 wraps edges. See docs/CHARACTER_SELECTION_007.md for per-cell IDs, drawing anchors, native addresses, known timing/UI adaptations and unsupported alternative palettes. No container schema or converter modification was needed.


### TEST007a: уточнение Nana money can

Контейнеры не менялись. Pack0104: команды92→OO91,97→OO94,96→OO95. FA818/1038: centre2,-35; halfsize28,41; power5; flags0. R817/1037:4,5,6,10,11,12. ON2→93 — переход именно при guard. Skill94 M1041 задаёт vx50 (×655fixed scale), skill95 M1052 задаёт ay250 (×393), ON1 приземления→91. Skill93 M1025 задаёт vx100 и создаёт4skill90; FA794 power3/rearm2/R4,5,6,4,5,6. Native differential evidence and limitations: docs/NANA_CAN_007A.md / research/nana_can_x86_007a.json. Ни timing, ни hitbox, ни урон не были исправлены без оснований.

ArcadeStick находится в UI, передаёт прежние direction bits; форматы EFP/EFBS/EFMS/EFSS и combat revision2 не изменены.


## TEST 007b — отображение PLAYER commands и policy snapshot

EFP Command.steps содержит10 little-endian u16; amounts —10 ints; skills —4 индекса (air/near/far/crouch). Конец последовательности — последний шаг с0x2000. Низкие4бита — исходное направление0..13, bits4..9 —A..F. В этом архиве активные записи используют mode0 (flags>>14=0) и mode2 (hold); mode2 берёт длительность из amounts. 0x1000 сохраняется, не превращается UI в выдуманный дополнительный шаг. Неподдержанный будущий режим не отображается с догадкой, счётчик таких записей доступен UI; для всех14 PLAYER он0.

Направления:0=без ограничения;1=центр;2=вперёд;3=вниз-вперёд;4=вниз;5=вниз-назад;6=назад;7=вверх-назад;8=вверх;9=вверх-вперёд;10=любое назад;11=любое вверх;12=любое вперёд;13=любое вниз. UI зеркалится мысленно по положению бойца; строка явно указывает ориентацию. Запятая разделяет шаги, плюс объединяет биты кнопок в одном шаге.

Скан использования кнопок включает префикс до последнего активного шага, JS/op36 [pc+5..pc+14] и manual guard из settings1766/1753. A–F физически поддерживаются runtime, но это не значит, что все6 назначены конкретным PLAYER.

Match snapshot EFMS version2 имеет ту же разметку, что version1, но означает decisiveRounds=true (draw не начисляет побед). Идентификатор assets/combat остался тем же; version+rules проверяются при restore. Story EFSS version1 сохраняет разметку и отделяет policy суффиксом identity`:first-to-two-v1`. Чтение записи старой policy как новой без явной save migration запрещено. InputFrame, core battle snapshot, PLAYER/STAGE/KGT/DEMO и EFP не менялись.


## Дополнение TEST 007d: оригинальный боевой HUD

Полное описание таблиц, адресов EXE, изображений и якорей — `docs/ORIGINAL_HUD_007D.md`. KGT originalTail отображается в EXE с VA0x435470. HUD использует uint16 skill-селекторы 0x4451ec–0x445242; это адреса виртуальной памяти, не offsets файла .kgt. Смещение в сохранённом originalTail равно VA−0x435470.

- 0x4451ec: infinity; 0x4451ee: десять timer digits; 0x445202: десять stock digits.
- 0x445216/0x445218: win ON/OFF; 0x44521a: десять stage-layout навыков.
- 0x44522e: два health skills; 0x445232: два special-meter skills.
- 0x445236: семь marker skills, по порядку timer, portraits P1/P2, stocks P1/P2, win marks P1/P2.

Первый metadata block marker-навыка: opcode0, s16@1 x, s16@3 y. Byte@5 у timer — unsigned шаг цифр22; у win markers — signed dx (+30/−30), byte@6 — signed dy. Изображение skill: opcode12, u16@1 duration, u16@3 flags+image, s16@5 x, s16@7 y, byte@9 options. Используются уже установленные image flags/anchors. Opcode35 перед картинкой задаёт colour/tint.

PLAYER builtin[22] — навык портрета боя, отдельный от портрета выбора персонажа. У 11 из 14 файлов он содержит изображение; у 0142,0144,0154 — исходно пустой. Максимум meter — i32@1758 внутри 1785-байтовых settings.

Native gauge handler0x40dd95..0x40de7d: visible width=floor(width*current/max). Health image82 имеет206×15; meter image83 —118×14. P1 сохраняет правый край, P2 — левый после отражения. Cropping подтверждён на56fixturecases. Timer0x40a620 проверен на12значениях: 1–3 цифры, исходные позиции, infinity.

## TEST008: final event and localisation sidecars

StoryProgram records of the ten selectable protagonists all have their last type1 fight at slot18, with enemy reference11 and value(5)=0. The original flag permits the old progression after a loss. The port now layers an explicit final-victory-v1 policy over these unchanged records. It is not a newly inferred binary flag. Regular fights and original event traversal remain unchanged. Details and replay identity migration: docs/LOCALIZATION_008.md.

No .kgt/.player/.stage/.demo format changed. Translated text lives in UTF-8 sidecars, separate from EFP2 and filenames:

- ELF-UI-1: tab-separated key, Japanese, English, Russian.
- ELF-SCENES-1: pack ID, image index, mode, x, y, width, height, max font size, ARGB text colour, background/mask, alignment, Japanese, English, Russian.
- Backslash, tab and newline are escaped as \\, \t and \n. Source TSV is human-reviewed; the generator writes these escaped catalogues.
- Modes: replace (transparent text), overlay (original image plus caption panels), card (clean0064/image2 template). Backgrounds include none, clear, ARGB fill, and mask:ARGB:left:top:width:height for a protected artwork rectangle within a flat caption region.

Image dimensions and VM references/timing are preserved; Japanese mode loads original bitmaps. Locale selection is presentation state outside simulation and replay hashes. Source raw/decoded/Android-safe filename mappings remain unchanged.

## RC1 title cross-check

KGT originalTail offset `0xE42C` (58412) starts the six one-byte screen references read by `StoryCatalog.screen(n)`. Values resolve as: 0 → DEMO7 / `0074` (`デモ 2`, title), 1 and 2 → `0064` (character selector), 3 unassigned, 4 → `0086` (Continue), 5 → `0072` (opening). Running the unmodified DEMO7 VM at startup emits images 1 and 11, with image11 at (0,0) as a background sprite. `0074/0011.png` is the original 640×480 title artwork used by RC1. Its BGM index is zero. No original container or image was modified.
