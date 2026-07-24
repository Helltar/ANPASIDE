package com.github.helltar.anpaside.preferences

import android.content.Context
import androidx.core.content.edit

interface EditorSessionPreferences {
    var recentFiles: List<String>
    var lastProject: String
}

class EditorPreferences(context: Context) : EditorSessionPreferences {

    private val prefs = context.getSharedPreferences("editor_config", Context.MODE_PRIVATE)

    override var recentFiles: List<String>
        get() {
            val encoded = prefs.getString(KEY_RECENT_FILES_V2, null)
            return if (encoded != null) {
                RecentFilesCodec.decode(encoded)
            } else {
                // migrate the delimiter-based format used before 2.0
                prefs.getString(KEY_RECENT_FILES_LEGACY, "")
                    .orEmpty()
                    .split(", ")
                    .filter(String::isNotEmpty)
            }
        }
        set(value) = prefs.edit {
            putString(KEY_RECENT_FILES_V2, RecentFilesCodec.encode(value))
            remove(KEY_RECENT_FILES_LEGACY)
        }

    override var lastProject: String
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
        private const val KEY_RECENT_FILES_LEGACY = "recent_filenames"
        private const val KEY_RECENT_FILES_V2 = "recent_files_v2"
        private const val KEY_LAST_PROJECT = "last_project"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_HIGHLIGHTER_ENABLED = "highlighter_enabled"
        private const val KEY_LINE_NUMBERS = "line_numbers"
        private const val KEY_WORD_WRAP = "word_wrap"

        const val DEFAULT_FONT_SIZE = 14
        val FONT_SIZE_RANGE = 8..24
    }
}

internal object RecentFilesCodec {

    fun encode(paths: List<String>): String =
        buildString {
            paths.forEach { path ->
                append(path.length)
                append(':')
                append(path)
            }
        }

    fun decode(encoded: String): List<String> {
        val paths = mutableListOf<String>()
        var offset = 0

        while (offset < encoded.length) {
            val separator = encoded.indexOf(':', offset)

            if (separator < 0) {
                return emptyList()
            }

            val length = encoded.substring(offset, separator).toIntOrNull() ?: return emptyList()
            val start = separator + 1

            if (length < 0 || length > encoded.length - start) {
                return emptyList()
            }

            val end = start + length
            paths += encoded.substring(start, end)
            offset = end
        }

        return paths
    }
}
