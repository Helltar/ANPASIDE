package com.github.helltar.anpaside.player

import android.app.Application
import javax.microedition.util.ContextHolder

class PlayerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // the embedded j2me runtime reaches for the application context from everywhere,
        // and this runs in both the launcher process and the midlet one
        ContextHolder.setApplication(this)
    }
}
