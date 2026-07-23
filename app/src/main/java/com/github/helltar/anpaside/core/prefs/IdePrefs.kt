package com.github.helltar.anpaside.core.prefs

import android.content.Context
import androidx.core.content.edit
import com.github.helltar.anpaside.core.Paths

class IdePrefs(context: Context) {

    private val prefs = context.getSharedPreferences("ide_config", Context.MODE_PRIVATE)

    var assetsInstalled: Boolean
        get() = prefs.getBoolean(KEY_INSTALL, false)
        set(value) = prefs.edit { putBoolean(KEY_INSTALL, value) }

    var assetsVersion: Int
        get() = prefs.getInt(KEY_ASSETS_VERSION, 0)
        set(value) = prefs.edit { putInt(KEY_ASSETS_VERSION, value) }

    var globalLibsDir: String
        get() = prefs.getString(KEY_GLOBAL_LIBS_DIR, Paths.globalLibsDir.path).orEmpty()
        set(value) = prefs.edit { putString(KEY_GLOBAL_LIBS_DIR, value) }

    // when off, the built jar is handed to whatever app the user has for .jar files
    var embeddedEmulatorEnabled: Boolean
        get() = prefs.getBoolean(KEY_EMBEDDED_EMULATOR, true)
        set(value) = prefs.edit { putBoolean(KEY_EMBEDDED_EMULATOR, value) }

    // the canvas the embedded emulator gives the midlet; this is what GetWidth and
    // GetHeight return in pascal. zero means "follow the device screen proportions"
    var midletScreenWidth: Int
        get() = prefs.getInt(KEY_MIDLET_SCREEN_WIDTH, DEFAULT_SCREEN_WIDTH)
        set(value) = prefs.edit { putInt(KEY_MIDLET_SCREEN_WIDTH, value) }

    var midletScreenHeight: Int
        get() = prefs.getInt(KEY_MIDLET_SCREEN_HEIGHT, DEFAULT_SCREEN_HEIGHT)
        set(value) = prefs.edit { putInt(KEY_MIDLET_SCREEN_HEIGHT, value) }

    var midletKeyboardEnabled: Boolean
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

        // width x height, 0 x 0 asks the emulator to match the device aspect ratio
        val SCREEN_SIZES = listOf(
            DEFAULT_SCREEN_WIDTH to DEFAULT_SCREEN_HEIGHT,
            176 to 220,
            320 to 240,
            320 to 480,
            0 to 0
        )
    }
}
