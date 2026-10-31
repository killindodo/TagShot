# TagShot Technical Architecture

> Native, offline, privacy-first Android photo indexing application by **[killindodo](https://github.com/killindodo)**.

---

## 1. System Overview

TagShot is built entirely with **Kotlin**, **Jetpack Compose**, and **Room SQLite**, prioritizing user privacy, instant multi-token search, and rich visual customization.

```
┌─────────────────────────────────────────────────────────────┐
│                       UI Presentation Layer                  │
│   MainScreen • PhotoDetailScreen • SettingsScreen          │
│   Dynamic Design System Engine (Liquid Glass, Win95, etc.) │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow / Actions
┌──────────────────────────────▼──────────────────────────────┐
│                      PhotoViewModel                          │
│   SavedStateHandle • Filtering • Scrubber • Selection State │
└──────────────────────────────┬──────────────────────────────┘
                               │ Coroutines / Flow
┌──────────────────────────────▼──────────────────────────────┐
│                    Domain & Data Pipeline                    │
│   PhotoRepository • ImportUseCase • DuplicateFinder         │
│   TagNormalizer • ThumbnailGenerator • OrphanSweeper        │
└──────────────────────────────┬──────────────────────────────┘
                               │ DAO
┌──────────────────────────────▼──────────────────────────────┐
│                    Persistence & Storage                     │
│   Room SQLite (AppDatabase) • Relative Filesystem Storage    │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Core Pillars & Design

### A. Privacy-First Zero-Permission Model
- **No Dangerous Permissions**: Zero Android runtime permissions requested (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `CAMERA`, or `INTERNET` are entirely absent from the manifest).
- **System Contracts**: Uses modern Android Photo Picker (`ActivityResultContracts.PickMultipleVisualMedia`) and system camera intent via private `FileProvider`.
- **Sandboxed Storage**: All media and thumbnails are stored in application-private storage (`context.filesDir/photos/orig/` and `context.filesDir/photos/thumbs/`).

### B. Dynamic UI Design System
A runtime design system switcher powers 6 completely distinct visual paradigms:
1. **Liquid Glass**: Translucent frosted acrylic containers with specular borders and ambient neon halos.
2. **Material You**: Google M3 expressive tonal surfaces, stadium pills, and fluid dynamic curves.
3. **Samsung One UI**: Generous 20–26dp curvatures with ergonomic thumb-zone reachability.
4. **HyperOS / MIUI**: Continuous super-ellipse squircles with high-contrast surfaces.
5. **Retro Windows 95**: 0dp sharp geometry, hardware-accelerated 3D raised and sunken bevels (`Modifier.retroBevel`), battleship gray panels, and authentic taskbar navigation dock.
6. **Pixel Clean**: Minimalist flat surfaces with 0.5dp razor-thin borders and zero blur.

Provided globally across all subtrees via `LocalUiDesignSystem`.

### C. Offline Dual-Engine Duplicate Detection
- **Level 1 (Bitwise Exact)**: SHA-256 byte hashing computed during image ingestion to prevent exact duplicate file imports.
- **Level 2 (Perceptual Similarity)**: Offline 64-bit difference hashing (`dHash`) computed on a normalized 9×8 grayscale bitmap grid. Compares adjacent horizontal gradients and matches photos using Hamming distance with quality scoring and storage reclamation in the Decision Hub.

### D. Unicode-Aware Instant Search & Normalization
- Tags are Unicode-normalized via **NFKC** normalization.
- Queries are split into multi-token sequences with boolean AND matching, handling diacritics, case-insensitivity, and prefix matches at sub-millisecond speeds.

---

## 3. Directory Structure

```
tagshot/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tagshot/app/
│   │   │   │   ├── data/           # Room Database, DAOs, Entities, Repository
│   │   │   │   ├── domain/         # Pure Kotlin math, Normalizer, DuplicateFinder
│   │   │   │   ├── ui/             # Jetpack Compose Screens, ViewModels, Dialogs
│   │   │   │   │   ├── components/ # Reusable UI components, photo cells, docks
│   │   │   │   │   └── theme/      # Design systems, palettes, tokens, shapes
│   │   │   │   └── TagShotApp.kt   # Application container and startup sweep
│   │   │   └── res/                # XML drawables, colors, file_paths
│   │   └── test/                   # 99 JVM Unit Tests
│   └── build.gradle.kts
├── docs/                           # Architecture, decisions, dependencies, changelog
├── gradle/                         # Gradle wrapper
└── README.md                       # Project overview and build instructions
```
