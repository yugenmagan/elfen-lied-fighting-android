# Porting notes

This document describes the current architecture and main technical decisions
of the Android port of **Elfen Lied Fighting**.

For the full chronological reverse-engineering and development log, including
the early TEST builds, see:

`research/historical/PORTING_LOG_FULL_RU.md`

Detailed binary-format research is documented in `FORMAT_NOTES.md` and
`research/`.

## Overview

The Android version does not run the original Windows executable through Wine,
Winlator or another compatibility layer.

Instead, the project implements its own Java runtime for the original game
data.

The original Fighter Maker data is converted into an Android-friendly internal
representation while preserving the source assets, bytecode, resource indices
and gameplay data required by the runtime.

The APK contains no original Windows executable.

## Runtime architecture

The port separates:

- deterministic game simulation;
- rendering;
- audio;
- Android input;
- lifecycle handling;
- UI;
- replay and rollback state.

Gameplay runs at a fixed simulation rate of 60 ticks per second.

Rendering and audio consume simulation output but do not directly mutate
gameplay state.

## Original game data

The supplied game contains:

- 1 global KGT container;
- 14 PLAYER containers;
- 4 STAGE containers;
- 27 DEMO containers.

The converter preserves resource indices and empty slots because the original
scripts reference data numerically.

Original graphics, music, sound effects, character data and story structures
are retained.

See `FORMAT_NOTES.md` for a high-level description of the source formats.

## Conversion

A Python conversion pipeline reads the original Fighter Maker containers and
produces the internal EFP1 representation used by the Android runtime.

The conversion process preserves:

- script and skill tables;
- original bytecode;
- indexed image data and palettes;
- WAV and MIDI resources;
- command tables;
- CPU data;
- character settings;
- story events;
- DEMO data;
- source filenames and hashes.

CP932 is used when decoding original Japanese strings.

Generated Android-safe filenames and source mappings are recorded so converted
assets can be traced back to their original files.

## Combat simulation

Combat is implemented as deterministic simulation state rather than being tied
to Android rendering.

The runtime includes support for:

- movement;
- command input;
- attacks and collision;
- damage;
- hit and guard reactions;
- meter and stocks;
- hitstop and freeze;
- projectiles and spawned objects;
- original CPU behaviour;
- KO and round flow;
- timer and match results.

Several parts of the original Windows runtime were verified by executing
isolated x86 handlers and comparing their behaviour with the port.

Detailed findings are kept under `research/`.

## Match and story runtime

The Android port implements the original game flow required for:

- character selection;
- Story mode;
- Versus mode;
- rounds and match results;
- DEMO scenes;
- endings and credits;
- Continue within the current application session.

The supplied game contains ten Story routes.

Lucy’s Story route has also been completed manually from beginning to end on
the Android version. Other routes have automated progression coverage but have
not all been completed manually.

## Input

Android input is converted into frame-numbered input state consumed by the
simulation.

Touch input supports multiple simultaneous pointers.

The project also includes configurable physical-gamepad mappings, although
controller compatibility still benefits from testing across more real devices.

Input state is cleared appropriately when pausing, losing focus or leaving a
match.

## Rendering

The original game uses a 4:3 play area.

On modern screens, additional horizontal space is used for touch controls where
possible rather than covering the original combat HUD.

Rendering uses Android Canvas plus converted original image resources.

Some behaviour of the original Windows renderer, especially uncommon visual
effects and exact compositing details, may still differ slightly.

## Audio

Original WAV and MIDI resources are retained.

Android playback uses platform audio APIs.

Exact MIDI timbre may differ from the original Windows environment because it
depends on the Android device and available MIDI implementation.

## Determinism, replay and rollback

Simulation state can be serialized into deterministic snapshots.

Snapshots include the gameplay state required for restore/resimulation,
including simulation RNG and command/input history.

The project contains:

- replay recording;
- state hashing;
- snapshot restore;
- local rollback/resimulation tests.

Experimental networking code also exists, but public internet multiplayer is
not part of the current RC release.

## Android lifecycle

Pause, Home and application lifecycle events are handled separately from game
simulation.

Continue restores the current in-memory session.

If Android terminates the application process, the current fight is not
persisted to disk.

## Build

Current Android configuration:

- Android 10+ (`minSdk 29`);
- compile / target SDK 35;
- Java runtime;
- Gradle / Android Gradle Plugin build;
- no NDK requirement.

See `BUILDING.md` for exact build instructions.

## Preservation policy

The project avoids inventing replacement behaviour where the original game data
or executable behaviour is uncertain.

Where possible, behaviour is derived from:

1. the original game data;
2. the original Windows executable;
3. reproducible comparison tests.

Unknown or imperfectly reproduced behaviour is documented rather than silently
replaced with guessed mechanics.

## Current status

The public release is `v1.0.0-rc1`.

It is a playable Android release candidate, not a claim of perfect
frame-by-frame equivalence with the original Windows runtime.

See:

- `KNOWN_ISSUES.md`
- `TEST_RESULTS_RC1A.md`
- `CHANGELOG.md`
- `FORMAT_NOTES.md`

for the current public status and remaining limitations.
