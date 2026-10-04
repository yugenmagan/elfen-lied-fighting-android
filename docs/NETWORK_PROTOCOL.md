# EFNET1 — TEST006

Новый протокол порта, не формат оригинальной Windows-игры. Реализация: `network/src/org/elfen/net/Protocol.java`, `GameConfig.java`, `RoomClient.java`, `OnlineMatch.java`; relay — `server/src/org/elfen/server/RoomServer.java`.

## Кодирование

TLS stream с normal trust-chain и endpoint verification, без HTTP. Каждый пакет: int32 BE длина, uint8 type, payload. Длина включает type, допустимо1..4096. Числа big-endian. Строки — Java DataOutput.writeUTF / modified UTF-8 с uint16 длиной в байтах, не произвольная UTF-8. Весь payload должен быть использован; хвост отклоняется.

| Направление / type | Поля |
|---|---|
| C CREATE1 | protocol int32=1, build hash string64, player ID string4, stage ID string4, wins int32, time int32 |
| C JOIN2 | protocol, build hash, player ID, room code string8 |
| S CREATED11 | room code |
| S CONFIG12 | assigned slot uint8, build hash, P1 ID, P2 ID, stage ID, seed int32, wins int32, time int32 |
| C READY3 | initial state hash string64 |
| S START13 | empty; only after both READY match |
| C INPUT4 / S REMOTE_INPUT14 | frame int32, sample count uint16, samples uint16[] |
| C HASH5 / S REMOTE_HASH15 | state frame int32, SHA-256 lowercase hex string64 |
| C LEAVE6 / PING7 | empty |
| S CLOSED16 / ERROR17 | message string, max1024 chars |
| S PONG18 | empty |

Player/stage ID — four decimal digits. Room code — eight signs from ABCDEFGHJKLMNPQRSTUVWXYZ23456789. Valid sample count1..256, each bitmask≤1023; ordered intermediate transitions preserved. Frame0..216000. INPUT N transforms stateN into stateN+1. HASH N names the state before inputN.

Wins1..9, time0..999; zero means original infinite timer. CPU disabled for both online players. Build identity SHA-256 includes sorted original pack IDs/hashes and semantic marker `ELF-NET1:match1:combat2`. It is a compatibility discriminator, not anti-cheat or a signed client attestation. Each runtime change affecting deterministic results must change this identity.

## Rollback

Simulation60Hz, integer combat, serialized RNG. History120frames; prediction18frames. Missing remote samples use the last known held mask. Exact late sample arrays replace predictions. Restore earliest changed state and resimulate with retained local input; rendering/audio never participate in state calculation. A corrected earlier finish stops resimulation there.

Contiguous remote inputs determine the confirmed prefix. Replay accepts only confirmed inputs. Hashes are sent every30 confirmed frames and for the exact final frame. Hash mismatch produces explicit DESYNC, not snapshot replacement. End-of-match UI waits for confirmed final state and the other player's matching final hash. An opponent leaving after this point cannot erase the confirmed result.

Effects are retained during prediction/resimulation, replaced on rollback, and emitted exactly once when their frame becomes confirmed. This avoids cancelled/duplicate SFX but adds network delay to sound. Music is presentation state, not simulation state.

Wall-clock time exists only in transport timeouts/room expiry and Android's scheduler. SecureRandom selects the room code and initial seed outside combat; the transmitted seed feeds the original deterministic RNG. No wall-clock is read by combat.

## Threading and failure

RoomClient reader/writer threads exchange immutable messages through bounded queues. OnlineMatch.pump/advance and RollbackSession run on the simulation thread. A lost connection stops online play explicitly. Neither client nor relay invents remote AI or awards a win. Android onPause closes an unfinished network match; resuming requires a new room. Offline story autosave remains separate.

Production endpoint deliberately unset until hosting is chosen. Tests use a fresh isolated localhost certificate and do not change Android's trust store. Certificate keys are excluded from checkpoint/APK.
