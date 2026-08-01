package com.github.helltar.anpaside.preferences

import android.content.Context
import androidx.core.content.edit

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("ide_config", Context.MODE_PRIVATE)

    var assetsInstalled: Boolean
        get() = prefs.getBoolean(KEY_INSTALL, false)
        set(value) = prefs.edit { putBoolean(KEY_INSTALL, value) }

    var assetsVersion: Int
        get() = prefs.getInt(KEY_ASSETS_VERSION, 0)
        set(value) = prefs.edit { putInt(KEY_ASSETS_VERSION, value) }

    // when off, the built jar is handed to whatever app the user has for .jar files
    var builtInEmulatorEnabled: Boolean
        get() = prefs.getBoolean(KEY_EMBEDDED_EMULATOR, true)
        set(value) = prefs.edit { putBoolean(KEY_EMBEDDED_EMULATOR, value) }

    // the canvas the embedded emulator gives the midlet; this is what GetWidth and
    // GetHeight return in pascal. zero follows the device proportions, while a negative
    // size uses the native display resolution
    var screenWidth: Int
        get() = prefs.getInt(KEY_MIDLET_SCREEN_WIDTH, FIT_SCREEN_DIMENSION)
        set(value) = prefs.edit { putInt(KEY_MIDLET_SCREEN_WIDTH, value) }

    var screenHeight: Int
        get() = prefs.getInt(KEY_MIDLET_SCREEN_HEIGHT, FIT_SCREEN_DIMENSION)
        set(value) = prefs.edit { putInt(KEY_MIDLET_SCREEN_HEIGHT, value) }

    var virtualKeyboardEnabled: Boolean
        get() = prefs.getBoolean(KEY_MIDLET_KEYBOARD, true)
        set(value) = prefs.edit { putBoolean(KEY_MIDLET_KEYBOARD, value) }

    companion object {
        private const val KEY_INSTALL = "install"
        private const val KEY_ASSETS_VERSION = "update_assets"
        private const val KEY_EMBEDDED_EMULATOR = "embedded_emulator"
        private const val KEY_MIDLET_SCREEN_WIDTH = "midlet_screen_width"
        private const val KEY_MIDLET_SCREEN_HEIGHT = "midlet_screen_height"
        private const val KEY_MIDLET_KEYBOARD = "midlet_keyboard"

        private const val FIT_SCREEN_DIMENSION = 0
        private const val CLASSIC_SCREEN_WIDTH = 240
        private const val CLASSIC_SCREEN_HEIGHT = 320

        // zero by zero matches the device aspect ratio; negative dimensions ask the
        // emulator to expose the native display resolution
        val availableScreenSizes = listOf(
            ScreenSize(CLASSIC_SCREEN_WIDTH, CLASSIC_SCREEN_HEIGHT),
            ScreenSize(320, 240),
            ScreenSize(FIT_SCREEN_DIMENSION, FIT_SCREEN_DIMENSION),
            ScreenSize(-1, -1)
        )
    }
}

data class ScreenSize(val width: Int, val height: Int) {
    val usesNativeResolution: Boolean
        get() = width < 0 && height < 0
}
