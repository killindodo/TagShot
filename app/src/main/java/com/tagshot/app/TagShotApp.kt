package com.tagshot.app

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.tagshot.app.data.AppContainer
import com.tagshot.app.data.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TagShotApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Run orphan sweeper once at app start per Section 7 & 8 (skip in instrumentation test environment)
        val isInstrumentationTest = runCatching {
            Class.forName("androidx.test.runner.AndroidJUnitRunner")
        }.isSuccess

        if (!isInstrumentationTest) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    container.orphanSweeper.sweep()
                } catch (_: Exception) {
                    // Ignore sweep errors at startup
                }
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .bitmapConfig(Bitmap.Config.HARDWARE)
            .crossfade(true)
            .build()
    }
}
