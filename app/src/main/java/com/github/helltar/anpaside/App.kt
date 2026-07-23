package com.github.helltar.anpaside

import android.app.Application
import android.content.Context
import javax.microedition.util.ContextHolder

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        // the embedded j2me runtime reaches for the application context from everywhere
        ContextHolder.setApplication(this)
    }

    companion object {
        private lateinit var instance: App

        // core reaches the application context for its file locations (see Paths); string
        // resources are resolved in the ui layer, never here
        val context: Context get() = instance
    }
}
