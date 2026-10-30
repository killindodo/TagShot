package com.tagshot.app

import android.app.Application
import com.tagshot.app.data.AppContainer
import com.tagshot.app.data.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TagShotApp : Application() {

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
}
