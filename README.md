# TagShot

TagShot is a fast, offline, privacy-first native Android photo indexing application built with Kotlin, Jetpack Compose, and Room SQLite. It empowers users to capture or import photos, attach custom tags, and instantly filter or search them using Unicode-aware multi-token AND matching.

---

## Features

- **Privacy-First & Permission-Free:** Zero dangerous Android permissions requested (no `CAMERA`, no storage permissions, no internet). Photos are imported using the modern Android Photo Picker (`PickMultipleVisualMedia`) and system camera contract (`TakePicture` via private `FileProvider`).
- **Instant Search & Filter:** Unicode-aware (NFKC normalized), case-insensitive search with multi-word AND semantics. Dynamic filter chip row displaying live library counts ("All", "Untagged", and `#tag (count)`).
- **Fast Adaptive Grid:** 110 dp adaptive thumbnail grid with up to two colored tag badges per cell (with contrast meeting WCAG 2.1 AA standards) and "+N" overflow.
- **Detail Viewer & Focal-Invariant Zoom:** Horizontal swipe pager (`HorizontalPager`), smooth pinch-to-zoom (1× to 8×), focal-invariant panning, double-tap zoom toggle (1× ↔ 3×), and non-interfering gesture detection.
- **Photo Metadata & Note Editor:** Bottom sheet displaying dates, image dimensions, file size, inline tag add/remove, and an editable photo note (up to 500 characters).
- **Bulk Selection Mode:** Long-press to select multiple photos. Add tags in bulk, remove specific tags, share to other apps, or delete with confirmation.
- **8-Second Delete Undo:** Deleting photos provides an immediate 8-second undo window via an action snackbar before files are permanently removed from disk.
- **Tag Manager:** View all tags and usage counts; rename tags with automatic merge detection when renaming to an existing tag; delete tags everywhere without removing photo files.
- **Deduplication:** Automatic SHA-256 byte hashing skips duplicate imports and reports skipped count.
- **Orphan File Sweeping:** Startup cleaner checks for unreferenced disk files left by unexpected process termination.

---

## Technical Architecture

- **Language & Runtime:** Kotlin 1.9.24, OpenJDK 17.
- **UI Toolkit:** Jetpack Compose (BOM 2024.06.00), Material 3 design, dynamic colors on Android 12+ (API 31+).
- **Persistence:** Room 2.6.1 SQLite database with full foreign key constraints and cascade rules.
- **Image Pipeline:** Android `BitmapFactory` downsampling with EXIF orientation correction (tags 1–8) and Coil 2.6.0 async image loader.
- **Architecture Pattern:** MVVM with unidirectional data flow and manual dependency injection (`AppContainer`). State preserved across process death via `SavedStateHandle`.
- **Target SDK:** Minimum SDK 26 (Android 8.0), Target SDK 34 (Android 14).

---

## Termux Build Instructions

TagShot is designed and verified to build directly inside Termux on an ARM64 Android device.

### 1. Prerequisites
```bash
pkg update && pkg upgrade -y
pkg install -y openjdk-17 git curl unzip aapt2 android-tools
```

### 2. Gradle AAPT2 Override
Because Android SDK's bundled `aapt2` is an x86_64 ELF binary that cannot execute directly on ARM64 Termux Bionic libc, configure Gradle to use Termux's native `aapt2` binary in `~/.gradle/gradle.properties`:
```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
org.gradle.jvmargs=-Xmx1536m -XX:+UseSerialGC -Dfile.encoding=UTF-8
org.gradle.workers.max=2
org.gradle.parallel=false
org.gradle.caching=true
kotlin.compiler.execution.strategy=in-process
```

### 3. Build & Test Commands
```bash
# Compile and package debug APK:
./gradlew assembleDebug

# Run all 40 JVM domain and ViewModel unit tests:
./gradlew testDebugUnitTest

# Run all 12 on-device connected instrumentation tests (via ADB Wireless Debugging):
./gradlew connectedDebugAndroidTest

# Run Android Lint:
./gradlew lintDebug
```

---

## Installing on Device

You can install the debug APK directly via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Or copy it to your device's Downloads directory (requires `termux-setup-storage`):
```bash
cp app/build/outputs/apk/debug/app-debug.apk ~/storage/downloads/tagshot-debug.apk
```
Then tap the APK inside your device's Files app to install.

---

## Limitations

- **No Cloud Backup:** TagShot stores photos and database records strictly in private internal app storage (`context.filesDir`). Uninstalling the application deletes all saved photos, notes, and tags permanently.
- **Single-Threaded Gradle on Phone:** Due to device RAM and thermal boundaries, Gradle builds should be run sequentially (avoid parallel tasks).
