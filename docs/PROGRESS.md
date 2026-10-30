# PROGRESS.md — TagShot Build & Verification Progress

## Phase 0: Environment + Skeleton
- Status: PASSED
- Bootstrap verification:
  - `uname -m`: aarch64
  - `java -version`: OpenJDK 17.0.21
  - `android.jar`: present at `/data/data/com.termux/files/home/android-sdk/platforms/android-34/android.jar`
  - `aapt2`: `/data/data/com.termux/files/usr/bin/aapt2`
  - `gradle`: wrapper 8.7
  - Storage: 8.2 GB free
- Gate Command:
```bash
./gradlew assembleDebug
```
- Output:
```
> Task :app:compileDebugKotlin
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:dexBuilderDebug
> Task :app:mergeDebugGlobalSynthetics
> Task :app:processDebugJavaRes
> Task :app:mergeProjectDexDebug
> Task :app:mergeDebugJavaResource
> Task :app:packageDebug
> Task :app:createDebugApkListingFileRedirect
> Task :app:assembleDebug

BUILD SUCCESSFUL in 5m 42s
36 actionable tasks: 36 executed
```
- Artifact: `app/build/outputs/apk/debug/app-debug.apk` verified (16 MB).

---

## Phase 1: Domain + Tests
- Status: PASSED
- Implemented Section 5 components:
  - `Limits.kt`
  - `TagNormalizer.kt`
  - `TagParser.kt`
  - `PhotoFilter.kt`
  - `PhotoSorter.kt`
  - `TagColor.kt`
  - `ThumbnailMath.kt`
  - `ZoomMath.kt`
- Test Suites: 35 tests, 0 failures, 100% pass rate.
- Gate Command:
```bash
./gradlew testDebugUnitTest --tests "com.tagshot.app.domain.*"
```
- Output:
```
> Task :app:bundleDebugClassesToCompileJar
> Task :app:bundleDebugClassesToRuntimeJar
> Task :app:kspDebugUnitTestKotlin
> Task :app:processDebugJavaRes
> Task :app:compileDebugUnitTestKotlin
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 3m 52s
29 actionable tasks: 13 executed, 16 up-to-date
```

---

## Phase 2: Database Layer
- Status: PASSED
- Schema Verification: PASSED (`app/schemas/com.tagshot.app.data.AppDatabase/1.json` generated and verified).
- KSP & Kotlin compilation: PASSED.
- Decision D11 executed: Moved `PhotoDaoTest` to Android connected instrumentation tests (`app/src/androidTest/java/com/tagshot/app/data/PhotoDaoTest.kt`).
- Gate Command:
```bash
./gradlew connectedDebugAndroidTest
```
- Output:
```
> Task :app:connectedDebugAndroidTest
Starting 7 tests on I2221 - 16

I2221 - 16 Tests 7/7 completed. (0 skipped) (0 failed)
Finished 7 tests on I2221 - 16

BUILD SUCCESSFUL in 2m 37s
66 actionable tasks: 27 executed, 6 from cache, 33 up-to-date
```
- Total tests: 7 tests completed, 0 failures, 0 skipped.

---

## Phase 3: File Pipeline + Import
- Status: PASSED
- Implemented components:
  - `ThumbnailGenerator.kt`: EXIF orientations 1..8 matrix transformations, downsampling, and JPEG compression.
  - `PhotoFileManager.kt`: App-private storage paths (`photos/orig/`, `photos/thumbs/`, `cache/camera/`).
  - `OrphanSweeper.kt`: Cleanup of unreferenced files and camera cache.
  - `ImportUseCase.kt`: Sequential import, SHA-256 deduplication, EXIF extraction, and compensation on failure.
- Tests in `src/androidTest`:
  - `ThumbnailGeneratorTest.kt`: 2 tests (matrix orientation pixel verification + dimensions).
  - `ImportUseCaseTest.kt`: 3 tests (import & duplicate skip + unreadable stream + orphan sweeper).
- Gate Command:
```bash
./gradlew connectedDebugAndroidTest
```
- Output:
```
> Task :app:connectedDebugAndroidTest
Starting 12 tests on I2221 - 16

Finished 12 tests on I2221 - 16

BUILD SUCCESSFUL in 1m 22s
66 actionable tasks: 10 executed, 56 up-to-date
```
- Total instrumentation tests: 12 passed, 0 failed, 0 skipped.

---

## Phase 4: Repository + ViewModel
- Status: PASSED
- Implemented components:
  - `DeletedPhotoBackup.kt`: Immutable backup for 8-second undo window.
  - `PhotoRepository.kt` & `DefaultPhotoRepository`: Reactive data operations, atomic transactions, IO dispatchers.
  - `AppContainer.kt` & `DefaultAppContainer`: Manual dependency injection container.
  - `PhotoViewModel.kt`: Activity-scoped ViewModel with `SavedStateHandle` persistence, Flow combination, search/chip/sort/untagged filtering, selection mode, and undo timers.
- Test Suite: 40 tests (35 domain + 5 ViewModel tests), 0 failures, 100% pass rate.
- Gate Command:
```bash
./gradlew testDebugUnitTest
```
- Output:
```
> Task :app:compileDebugJavaWithJavac UP-TO-DATE
> Task :app:bundleDebugClassesToRuntimeJar UP-TO-DATE
> Task :app:bundleDebugClassesToCompileJar UP-TO-DATE
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest UP-TO-DATE
> Task :app:processDebugJavaRes UP-TO-DATE
> Task :app:kspDebugUnitTestKotlin
> Task :app:compileDebugUnitTestKotlin
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes UP-TO-DATE
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 39s
26 actionable tasks: 4 executed, 22 up-to-date
```

---

## Phase 5: Main Screen UI
- Status: PASSED
- Implemented components:
  - `MainScreen.kt`: Search bar (instant substring & tag match), sort dropdown menu, dynamic filter chip row ("All", "Untagged", and individual `#tag (count)` chips), adaptive grid (`GridCells.Adaptive(110.dp)`), Coil `AsyncImage` with `size(512)` fixed requests, expandable FAB (Camera & Gallery).
  - `TagInputDialog`: Live parsing preview, suggestion chips from existing tags, validation against Limits.
  - Camera & Gallery import flows: `ActivityResultContracts.TakePicture()` and `PickMultipleVisualMedia()`, pending camera state preserved via `rememberSaveable`.
- Gate Command:
```bash
./gradlew assembleDebug
```
- Output:
```
BUILD SUCCESSFUL in 1m 1s
37 actionable tasks: 8 executed, 29 up-to-date
```

---

## Phase 6: Detail + Zoom + Share
- Status: PASSED
- Implemented components:
  - `PhotoDetailScreen.kt`: `HorizontalPager` with `beyondBoundsPageCount = 1`, `userScrollEnabled` bound to `!isZoomed`.
  - `ZoomableImage.kt`: Focal invariant pinch-to-zoom (1× to 8×), pan, and double-tap toggle (1× ↔ 3×) using `ZoomMath`. Detail images capped by `Limits.DETAIL_MAX_EDGE` (3072 px).
  - Bottom info sheet: Formatted import and capture timestamps, dimensions, file size, editable note (debounced save up to 500 chars), tag flow row with direct tag removal and addition.
  - Sharing: Single and multiple photo sharing via `FileProvider` (`${applicationId}.fileprovider`) with read URI permissions.
- Gate: Tested and verified compilation with unit and connected tests.

---

## Phase 7: Bulk Mode, Undo, Tag Manager, Accessibility, Theming
- Status: PASSED
- Implemented components:
  - Bulk Selection Mode: Long-press on cells enters selection; multi-select top bar with "N selected", Add Tags, Remove Tag (shows dialog with distinct tags on selection), Share Selected, Delete Selected. Back exits selection first.
  - 8-Second Undo Window: `UiEvent.ShowUndoSnackbar` with action "Undo" triggers `repository.restorePhotos(backups)`. File deletion deferred until window closes.
  - `TagManagerScreen.kt`: Lists all tags with photo counts; Rename dialog with automatic merge detection if target normalized tag already exists; Delete dialog ("Remove #x from N photos? Photos are not deleted").
  - Theming & Edge-to-Edge: `TagShotTheme` with dynamic color for Android 12+ (API 31+) and fallback light/dark schemes; `enableEdgeToEdge()` enabled in `MainActivity.kt`.
  - Adaptive Launcher Icon: Vector adaptive icon (`ic_launcher_foreground.xml`) featuring tag badge over photo frame.
  - Accessibility: Touch targets ≥ 48 dp, `contentDescription` on all interactive icons.
- Verification Gates:
```bash
./gradlew testDebugUnitTest
```
- Output:
```
BUILD SUCCESSFUL in 2m 11s
26 actionable tasks: 14 executed, 12 up-to-date
```
```bash
./gradlew connectedDebugAndroidTest
```
- Output:
```
Starting 12 tests on I2221 - 16
Finished 12 tests on I2221 - 16
BUILD SUCCESSFUL in 58s
66 actionable tasks: 7 executed, 59 up-to-date
```









