# DECISIONS.md — TagShot Architectural & Operational Decisions

This document records architectural decisions, deviations, and version pins per the Implementation Plan v2 spec.

## Initial Decisions (from Spec Section 1)
- **D1:** Filter in pure Kotlin (`PhotoFilter`) over a Room `Flow` rather than SQLite `LIKE`. Pure-Kotlin Unicode-correct, unit-testable semantics.
- **D2:** Tag unique index on `normalizedName`; `name` preserves the user's casing.
- **D3:** File paths stored in DB relative to `filesDir` (`photos/orig/<uuid>.<ext>` and `photos/thumbs/<uuid>.jpg`).
- **D4:** Tag color derived deterministically from FNV-1a 32-bit hash of `normalizedName`.
- **D5:** No `CAMERA` permission in manifest. Uses system camera contract with `FileProvider` URI.
- **D6:** Room DAO tests use Robolectric JVM tests; domain logic tests use pure-Kotlin JVM tests.
- **D7:** Zero compiler/build errors; every warning reviewed and resolved or justified.
- **D8:** Duplicate detection via SHA-256, undo delete with 8-second window, bulk selection, tag manager (rename/merge/delete), swipe pager, untagged filter, orphan-file sweeper.
- **D9:** Sort by `addedAt` desc/asc with tie-break by `id`. `takenAt` from EXIF used for display.
- **D10:** Fixed square cells in grid with `ContentScale.Crop`.

## Toolchain & Environment Decisions
- **E1:** Termux ARM64 environment. Overrode `android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` in `~/.gradle/gradle.properties` due to stock AAPT2 being x86-64 ELF.
- **E2:** JVM heap pinned to `-Xmx1536m` in `~/.gradle/gradle.properties` with `-XX:+UseSerialGC` as total device RAM is 7,297 MB (< 8 GB).
- **E3:** Java toolchain pinned to OpenJDK 17.0.21 (`update-alternatives` manual mode). JDK 21 excluded per Section 2.2 step 4.
- **E4:** Gradle wrapper pinned to 8.7 (`distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip`). Compatible with AGP 8.4.2.
- **E5:** Kotlin `1.9.24`, KSP `1.9.24-1.0.20`, Compose Compiler Extension `1.5.14`, Compose BOM `2024.06.00`.
- **E6:** Deletion of existing `~/boultfit` (5.2 GB) authorized by human owner Surya to restore free disk space from 2.4 GB to 8.2 GB, satisfying Section 2.2 requirement of >= 4 GB free storage.

## Unverified Items
- None. All dependencies, toolchain items, and paths verified in this session.
