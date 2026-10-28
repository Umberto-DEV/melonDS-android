# Decisioni e limiti del controllo

Le decisioni seguenti conservano perimetro e costo possibile; nessuna equivale a una correzione verificata fuori dal proprio test. Nessun rilievo minore rinviato nella revisione finale.

- Ruling: continuous execution and main-only are explicitly authorized by user; no repeated plan approval or worktree. Cost if wrong: user can review isolated commits before publication, which remains gated.

- Ruling: fix PCM overflow without changing gain/UI; this preserves existing sensitivity. Cost if wrong: loud input still saturates, so #1679 remains partial until hardware/gain design.

- Ruling: Added Task8 after real SQLite evidence of data loss on malformed XML. Use a bounded parser event channel consumed within one Room transaction; keep synchronous parser work outside the transaction executor to avoid callback deadlocks. No schema changes or unbounded game accumulation.

- Final: Ruling: physical microphone sensitivity/routing/concurrency set aside — callback algorithms are verified with explicit boundaries; hardware acceptance remains pending — cost if wrong: device-specific input/routing races can remain.

- Final: Ruling: resampler fidelity/short callbacks/queued mute samples set aside — unchanged interpolation and same-session queue policy — cost if wrong: distortion or stale queued input may remain outside these fixes.

- Final: Ruling: complete ROM/NAND format/TMD authenticity/JNI allocation exceptions set aside — identity/length checks do not certify these independent properties — cost if wrong: malformed inputs or exceptional allocation paths can still fail.

- Final: Ruling: FAT short writes/durability/logical quota/HOME Menu set aside — unchanged core/model boundary and no real NAND fixture — cost if wrong: underlying write failures may not be reported and real import can still corrupt NAND.

- Final: Ruling: live NUS behavior/blocked I/O cancellation set aside — exact-read contract proven with controlled streams — cost if wrong: remote endpoint or provider reads can still fail/stall.

- Final: Ruling: physical display/Thor/fold/touch latency set aside — provider and valid MotionEvent contracts proven, hardware timing not inferred — cost if wrong: OEM routing/offset/latency bugs remain.

- Final: Ruling: arbitrary cheat database resource exhaustion and gameplay codes set aside — bounded event queue does not bound existing full address book or32-bit byte counter — cost if wrong: huge/malformed imports and game-specific codes remain problematic.

- Final: Ruling: Mac frontend/unrelated categories/publication readiness set aside — Android mandate and explicit publication gate retained; other categories documented by reproduction prerequisites — cost if wrong: broader issues remain open, no unsupported closure claim.
