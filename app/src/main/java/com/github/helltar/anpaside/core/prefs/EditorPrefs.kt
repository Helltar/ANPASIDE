package com.github.helltar.anpaside.core.prefs

import android.content.Context
import androidx.core.content.edit

class EditorPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("editor_config", Context.MODE_PRIVATE)

    var recentFiles: List<String>
        get() = prefs.getString(KEY_RECENT_FILES, "").orEmpty().split(", ").filter { it.isNotEmpty() }
        set(value) = prefs.edit { putString(KEY_RECENT_FILES, value.joinToString(", ")) }

    var lastProject: String
        get() = prefs.getString(KEY_LAST_PROJECT, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_LAST_PROJECT, value) }

    var fontSize: Int
        get() = prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
        set(value) = prefs.edit { putInt(KEY_FONT_SIZE, value) }

    var highlighterEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIGHLIGHTER_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_HIGHLIGHTER_ENABLED, value) }

    var lineNumbersEnabled: Boolean
        get() = prefs.getBoolean(KEY_LINE_NUMBERS, true)
        set(value) = prefs.edit { putBoolean(KEY_LINE_NUMBERS, value) }

    // off by default: code lines run long and horizontal scrolling keeps them readable
    var wordWrapEnabled: Boolean
        get() = prefs.getBoolean(KEY_WORD_WRAP, false)
        set(value) = prefs.edit { putBoolean(KEY_WORD_WRAP, value) }

    companion object {
        private const val KEY_RECENT_FILES = "recent_filenames"
        private const val KEY_LAST_PROJECT = "last_project"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_HIGHLIGHTER_ENABLED = "highlighter_enabled"
        private const val KEY_LINE_NUMBERS = "line_numbers"
        private const val KEY_WORD_WRAP = "word_wrap"

        const val DEFAULT_FONT_SIZE = 14
        const val MIN_FONT_SIZE = 8
        const val MAX_FONT_SIZE = 24
    }
}
