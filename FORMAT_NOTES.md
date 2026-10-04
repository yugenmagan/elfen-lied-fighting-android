# Game data format notes

This document gives a short overview of the original **Elfen Lied Fighting**
data formats used by the Android port.

It is not intended to be a complete specification of the 2D Fighter Maker 2nd
runtime.

Detailed reverse-engineering notes, opcode research, x86 handler analysis and
historical test findings are preserved under `research/`.

## Original file types

The original game primarily uses four container types:

| Extension | Purpose |
| --- | --- |
| `.kgt` | Global game configuration |
| `.player` | Character data, commands, AI and story route data |
| `.stage` | Stage data |
| `.demo` | Scripted scenes / demonstrations |

The containers share the signature:

`2DKGT2G`

Strings in the original data use **CP932 / Shift-JIS** encoding.

## Shared container structure

The four formats share a common section containing:

- internal name;
- script / skill table;
- 16-byte bytecode instructions;
- image slots;
- palettes;
- sound resources.

Type-specific data follows this shared section.

Empty slots are significant and must be preserved because the original game
references resources by numeric index.

## Scripts and bytecode

Game behaviour is stored as fixed-size 16-byte instructions.

The first byte is the opcode; the remaining bytes depend on the instruction
type.

The Android runtime implements the subset required by the supplied Elfen Lied
Fighting data, including character movement, animation, attacks, branching,
variables, input commands, collisions and match logic.

The original bytecode is preserved during conversion rather than rewritten into
a new scripting language.

Detailed opcode research is documented in the files under `research/`.

## Images

Image resources use indexed colour palettes.

The supplied game data contains:

- global palettes;
- optional per-image palettes;
- compressed and uncompressed image payloads.

The original image compression is a custom command stream rather than
ZIP/zlib.

During conversion, images may be cached as PNG for Android rendering, while the
original indexed data and palette information are retained where required.

Transparency behaviour follows the original game's RGB rules rather than
treating the fourth palette byte as ordinary PNG alpha.

## Audio

Embedded audio resources contain original WAV and MIDI payloads.

The port preserves the original resource data and maps it to Android playback
at runtime.

## `.player`

Player files contain, among other things:

- character scripts and animations;
- command inputs;
- hit / reaction references;
- CPU behaviour data;
- character settings;
- story route entries.

Story data includes battles, demo scenes, conditional branches and endings.

## `.stage`

Stage files contain stage-specific configuration and references to music and
scripted visual data.

Background behaviour is not represented simply as a single PNG; the original
stage scripts and resource references are retained.

## `.demo`

Demo files are scripted scenes, not video files.

They may contain:

- images;
- animation scripts;
- dialogue artwork;
- music;
- timing;
- transitions.

The Android port executes the converted demo data rather than replacing these
scenes with prerecorded video.

## Internal Android format

The converter produces an internal format called **EFP1**.

EFP1 is a derived Android-friendly representation of the original game data.
It is not claimed to be an original 2D Fighter Maker 2nd format.

It preserves the information needed by the Android runtime while avoiding
dependencies on Windows memory layouts and pointer values.

The conversion pipeline also records file names, Unicode mappings and hashes so
the generated Android assets can be traced back to the original source data.

## Compatibility scope

The goal of the project is compatibility with the supplied **Elfen Lied
Fighting** game data.

It should not be assumed that the parser or runtime supports every game ever
created with 2D Fighter Maker 2nd.

Behaviour that has not been verified against the original Windows runtime is
documented conservatively rather than guessed.

## Further research

Detailed reverse-engineering material is preserved under:

- `research/`
- `PORTING_NOTES.md`

This includes opcode layouts, x86 handler analysis, scheduler behaviour,
collision and damage research, RNG behaviour and historical test results.
