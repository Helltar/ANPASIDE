package com.github.helltar.anpaside

import android.app.Application
import javax.microedition.util.ContextHolder

class AnpasideApplication : Application() {

    val container: AppContainer by lazy(LazyThreadSafetyMode.NONE) {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()

        // the embedded j2me runtime reaches for the application context from everywhere
        ContextHolder.setApplication(this)
    }
}
