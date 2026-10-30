# DEPENDENCIES.md — Verified Artifact Coordinates

All dependencies below were verified via HTTP HEAD/GET request returning `HTTP/2 200` per Section 2.4 protocol on 2026-10-30.

## Gradle Plugins & Core Toolchain
| Coordinate | Source | HTTP Status |
|---|---|---|
| `com.android.tools.build:gradle:8.4.2` | dl.google.com | 200 |
| `org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24` | repo1.maven.org | 200 |
| `com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.24-1.0.20` | plugins.gradle.org | 200 (via 303 redirect) |
| `androidx.compose.compiler:compiler:1.5.14` | dl.google.com | 200 |

## Runtime Dependencies
| Coordinate | Source | HTTP Status |
|---|---|---|
| `androidx.compose:compose-bom:2024.06.00` | dl.google.com | 200 |
| `androidx.core:core-ktx:1.13.1` | dl.google.com | 200 |
| `androidx.activity:activity-compose:1.9.0` | dl.google.com | 200 |
| `androidx.lifecycle:lifecycle-runtime-ktx:2.8.2` | dl.google.com | 200 |
| `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2` | dl.google.com | 200 |
| `androidx.lifecycle:lifecycle-runtime-compose:2.8.2` | dl.google.com | 200 |
| `androidx.navigation:navigation-compose:2.7.7` | dl.google.com | 200 |
| `androidx.room:room-runtime:2.6.1` | dl.google.com | 200 |
| `androidx.room:room-ktx:2.6.1` | dl.google.com | 200 |
| `androidx.room:room-compiler:2.6.1` | dl.google.com | 200 |
| `io.coil-kt:coil-compose:2.6.0` | repo1.maven.org | 200 |
| `androidx.exifinterface:exifinterface:1.3.7` | dl.google.com | 200 |

## Test Dependencies
| Coordinate | Source | HTTP Status |
|---|---|---|
| `junit:junit:4.13.2` | repo1.maven.org | 200 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1` | repo1.maven.org | 200 |
| `androidx.test:runner:1.5.2` | dl.google.com | 200 |
| `androidx.test.ext:junit:1.1.5` | dl.google.com | 200 |
| `androidx.test:core:1.5.0` | dl.google.com | 200 |
| `androidx.room:room-testing:2.6.1` | dl.google.com | 200 |

