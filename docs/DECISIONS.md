# DECISIONS.md — TagShot Architectural Decisions

This document records the core architectural decisions and toolchain configurations for the TagShot project developed by killindodo.

## Core Architectural Decisions
- **D1:** Filter in pure Kotlin (`PhotoFilter`) over a Room `Flow` rather than SQLite `LIKE`. Delivers Unicode-correct (NFKC), multi-token, unit-testable search semantics.
- **D2:** Tag unique index on `normalizedName`; `name` preserves original casing entered by the user.
- **D3:** File paths stored in DB relative to `filesDir` (`photos/orig/<uuid>.<ext>` and `photos/thumbs/<uuid>.jpg`) for portability across storage moves and backups.
- **D4:** Tag color derived deterministically from an FNV-1a 32-bit hash of `normalizedName`.
- **D5:** No `CAMERA` permission declared in manifest. Uses system camera contract (`TakePicture`) with a private `FileProvider` URI for zero-permission privacy.
- **D6:** Domain logic verified with pure Kotlin JVM unit tests; SQLite DAO verified with on-device instrumentation tests.
- **D7:** Zero compiler warnings or errors; clean single-daemon Termux build support.
- **D8:** Complete visual management feature set: SHA-256 byte deduplication, 64-bit dHash perceptual duplicate detection, 8-second undo delete window, bulk multi-selection, tag manager (rename/merge/delete), horizontal swipe pager, and orphan file sweeper.
- **D9:** Dual sort options: `addedAt` desc/asc with tie-break by `id`, and EXIF `takenAt` timeline sorting.
- **D10:** Adaptive thumbnail grid with dynamic aspect-ratio cards, borderless photo wall, and Windows details table mode.
- **D11:** 6-system UI Design System engine (Material You, Liquid Glass, Samsung One UI, HyperOS, Retro Windows 95, Pixel Clean) driven by dynamic Compose tokens.

## Toolchain & Environment Configuration
- **E1:** Termux ARM64 native toolchain with `android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` configured in Gradle properties.
- **E2:** JVM heap configured to `-Xmx1536m` with `-XX:+UseSerialGC` for optimal execution on mobile devices.
- **E3:** Java toolchain pinned to OpenJDK 17.
- **E4:** Gradle wrapper pinned to 8.7 with Android Gradle Plugin 8.4.2.
- **E5:** Kotlin `1.9.24`, KSP `1.9.24-1.0.20`, Compose Compiler `1.5.14`, Compose BOM `2024.06.00`.
- **E6:** Single-threaded daemon configuration (`-Dorg.gradle.parallel=false`) ensuring stable memory headroom on ARM64 Linux.
