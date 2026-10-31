# TagShot

[![Author](https://img.shields.io/badge/Author-killindodo-blue.svg)](https://github.com/killindodo)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin_1.9.24-purple.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose_BOM-red.svg)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Database-Room_SQLite_2.6.1-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Unit Tests](https://img.shields.io/badge/Unit_Tests-99_Passed-brightgreen.svg)]()

> Developed with ❤️ by **[killindodo](https://github.com/killindodo)**.

TagShot is a native, offline, privacy-first Android photo indexing and visual management application. Built from scratch with **Kotlin**, **Jetpack Compose**, and **Room SQLite**, it enables instant multi-token search, customizable tag styling, multi-system UI theming, and perceptual duplicate detection without requiring storage, camera, or internet permissions.

---

## Key Features

- **Zero-Permission Privacy:** No dangerous permissions declared (no `READ_MEDIA_IMAGES`, `CAMERA`, or `INTERNET`). Media ingestion is performed safely through Android's modern Photo Picker (`PickMultipleVisualMedia`) and system camera contract (`TakePicture` via private `FileProvider`).
- **6 Dynamic UI Design Systems:** Complete visual transformation at runtime:
  - **Liquid Glass:** Frosted acrylic glass containers with specular borders and ambient neon glows.
  - **Material You:** Google M3 tonal surfaces, stadium pills, and fluid dynamic curves.
  - **Samsung One UI:** 20–26dp curvatures with ergonomic thumb-zone reachability.
  - **HyperOS / MIUI:** Continuous super-ellipse squircles and high-contrast surfaces.
  - **Retro Windows 95:** 0dp sharp geometry, hardware-accelerated 3D raised and sunken bevels (`Modifier.retroBevel`), battleship gray panels, and classic taskbar navigation.
  - **Pixel Clean:** Minimalist flat surfaces with 0.5dp razor-thin borders and zero blur.
- **Dynamic Settings Mutual Exclusion:** Intelligently adapts and locks conflicting options with visual lock badges (`🔒`), keeping theme selections consistent.
- **Dual-Engine Duplicate Finder:**
  - **Bitwise Exact:** SHA-256 byte hashing skips duplicate file imports automatically.
  - **Perceptual Similarity:** Offline 64-bit difference hashing (`dHash`) identifies similar photos with Hamming distance matching, side-by-side comparison, quality scoring, and storage reclaim in the Decision Hub dialog.
- **Unicode-Aware Instant Search:** Case-insensitive, NFKC-normalized multi-token AND search across photos, tags, and notes with sub-millisecond response.
- **Date Range & Timeline Filters:** Filter photos by date ranges with one-tap presets ("Today", "This Month", "Last 30 Days", "This Year") or custom date pickers.
- **Tag Style & Scrubber Customizers:** Customize tag badges (Capsule, Squircle, Sharp, Cyber, Leaf, Dot), anchor positions, color presets, and fast-scroll scrubber styles (Cupertino, Minimal, Pill, Retro Windows).
- **8-Second Delete Undo:** Deleting photos triggers an 8-second safety window via a snackbar before files are permanently purged from private storage.
- **Tag Manager:** Comprehensive tag management with rename, automatic merge detection, and global deletion.
- **Creator Easter Egg:** Hidden 7-tap trigger in Settings unlocking the Guest of Honor celebration and connecting directly to GitHub.

---

## Technical Stack

- **Language:** Kotlin 1.9.24
- **UI Framework:** Jetpack Compose (BOM 2024.06.00), Material 3
- **Database:** Room 2.6.1 SQLite with foreign key cascades and relational queries
- **Image Pipeline:** BitmapFactory downsampling with EXIF orientation correction and Coil 2.6.0 async loader
- **Testing:** 99 JVM unit tests covering domain math, ViewModels, filters, sorters, and duplicate algorithms
- **Target OS:** Android 8.0 (API 26) through Android 14 (API 34)

---

## Build Instructions (Termux / Linux / Android)

TagShot is designed to compile cleanly inside Termux directly on ARM64 Android devices as well as on standard Linux/macOS/Windows workstations.

### Prerequisites (Termux)
```bash
pkg update && pkg upgrade -y
pkg install -y openjdk-17 git curl unzip aapt2 android-tools
```

### Build Commands
```bash
# Compile and build the debug APK:
./gradlew assembleDebug -Dorg.gradle.parallel=false

# Run all 99 JVM unit tests:
./gradlew testDebugUnitTest -Dorg.gradle.parallel=false

# Run Android Lint:
./gradlew lintDebug -Dorg.gradle.parallel=false
```

### Installation
Install directly to your device via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Or copy to Downloads for direct package installer launch:
```bash
cp app/build/outputs/apk/debug/app-debug.apk ~/storage/downloads/tagshot-debug.apk
```

---

## License & Attribution

Designed and created by **[killindodo](https://github.com/killindodo)**. Open-source under the MIT License.
