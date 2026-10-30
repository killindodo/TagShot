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
- Status: In Progress







