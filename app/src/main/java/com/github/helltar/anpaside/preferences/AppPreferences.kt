package com.github.helltar.anpaside.preferences

import android.content.Context
import androidx.core.content.edit
import java.io.File

class AppPreferences(
    context: Context,
    private val defaultGlobalLibrariesDirectory: File
) {

    private val prefs = context.getSharedPreferences("ide_config", Context.MODE_PRIVATE)

    var assetsInstalled: Boolean
        get() = prefs.getBoolean(KEY_INSTALL, false)
        set(value) = prefs.edit { putBoolean(KEY_INSTALL, value) }

    var assetsVersion: Int
        get() = prefs.getInt(KEY_ASSETS_VERSION, 0)
        set(value) = prefs.edit { putInt(KEY_ASSETS_VERSION, value) }

    var globalLibrariesDirectory: String
        get() =
            prefs.getString(KEY_GLOBAL_LIBS_DIR, defaultGlobalLibrariesDirectory.path)
                ?.takeIf(String::isNotBlank)
                ?: defaultGlobalLibrariesDirectory.path
        set(value) = prefs.edit { putString(KEY_GLOBAL_LIBS_DIR, value) }

    // when off, the built jar is handed to whatever app the user has for .jar files
    var builtInEmulatorEnabled: Boolean
        get() = prefs.getBoolean(KEY_EMBEDDED_EMULATOR, true)
        set(value) = prefs.edit { putBoolean(KEY_EMBEDDED_EMULATOR, value) }

    // the canvas the embedded emulator gives the midlet; this is what GetWidth and
    // GetHeight return in pascal. zero means "follow the device screen proportions"
    var screenWidth: Int
        get() = prefs.getInt(KEY_MIDLET_SCREEN_WIDTH, DEFAULT_SCREEN_WIDTH)
        set(value) = prefs.edit { putInt(KEY_MIDLET_SCREEN_WIDTH, value) }

    var screenHeight: Int
        get() = prefs.getInt(KEY_MIDLET_SCREEN_HEIGHT, DEFAULT_SCREEN_HEIGHT)
        set(value) = prefs.edit { putInt(KEY_MIDLET_SCREEN_HEIGHT, value) }

    var virtualKeyboardEnabled: Boolean
        get() = prefs.getBoolean(KEY_MIDLET_KEYBOARD, true)
        set(value) = prefs.edit { putBoolean(KEY_MIDLET_KEYBOARD, value) }

    companion object {
        private const val KEY_INSTALL = "install"
        private const val KEY_ASSETS_VERSION = "update_assets"
        private const val KEY_GLOBAL_LIBS_DIR = "global_libs_dir"
        private const val KEY_EMBEDDED_EMULATOR = "embedded_emulator"
        private const val KEY_MIDLET_SCREEN_WIDTH = "midlet_screen_width"
        private const val KEY_MIDLET_SCREEN_HEIGHT = "midlet_screen_height"
        private const val KEY_MIDLET_KEYBOARD = "midlet_keyboard"

        const val DEFAULT_SCREEN_WIDTH = 240
        const val DEFAULT_SCREEN_HEIGHT = 320

        // zero by zero asks the emulator to match the device aspect ratio
        val availableScreenSizes = listOf(
            ScreenSize(DEFAULT_SCREEN_WIDTH, DEFAULT_SCREEN_HEIGHT),
            ScreenSize(176, 220),
            ScreenSize(320, 240),
            ScreenSize(320, 480),
            ScreenSize(0, 0)
        )
    }
}

data class ScreenSize(val width: Int, val height: Int)
