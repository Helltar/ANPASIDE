package com.github.helltar.anpaside.preferences

import android.content.Context
import androidx.core.content.edit

interface EditorSessionPreferences {
    var recentFiles: List<String>
    var foldedBlockOffsets: Map<String, Set<Int>>
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

    override var foldedBlockOffsets: Map<String, Set<Int>>
        get() = FoldingStateCodec.decode(prefs.getString(KEY_FOLDED_BLOCKS, "").orEmpty())
        set(value) = prefs.edit {
            putString(KEY_FOLDED_BLOCKS, FoldingStateCodec.encode(value))
        }

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
        private const val KEY_FOLDED_BLOCKS = "folded_blocks_v1"
        private const val KEY_LAST_PROJECT = "last_project"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_HIGHLIGHTER_ENABLED = "highlighter_enabled"
        private const val KEY_LINE_NUMBERS = "line_numbers"
        private const val KEY_WORD_WRAP = "word_wrap"

        const val DEFAULT_FONT_SIZE = 14
        val FONT_SIZE_RANGE = 8..24
    }
}

internal object FoldingStateCodec {

    fun encode(states: Map<String, Set<Int>>): String =
        buildString {
            states.toSortedMap().forEach { (path, offsets) ->
                if (offsets.isEmpty()) {
                    return@forEach
                }

                append(path.length)
                append(':')
                append(path)
                append(offsets.size)
                append(':')
                offsets.sorted().forEach { offset ->
                    append(offset)
                    append(':')
                }
            }
        }

    fun decode(encoded: String): Map<String, Set<Int>> {
        val states = mutableMapOf<String, Set<Int>>()
        var position = 0

        while (position < encoded.length) {
            val pathLength = encoded.readNumber(position) ?: return emptyMap()
            position = pathLength.nextPosition

            if (pathLength.value > encoded.length - position) {
                return emptyMap()
            }

            val pathEnd = position + pathLength.value
            val path = encoded.substring(position, pathEnd)
            position = pathEnd

            val count = encoded.readNumber(position) ?: return emptyMap()
            position = count.nextPosition

            if (count.value > (encoded.length - position) / 2) {
                return emptyMap()
            }

            val offsets = mutableSetOf<Int>()

            repeat(count.value) {
                val offset = encoded.readNumber(position) ?: return emptyMap()
                offsets += offset.value
                position = offset.nextPosition
            }

            if (offsets.isNotEmpty()) {
                states[path] = offsets
            }
        }

        return states
    }

    private fun String.readNumber(start: Int): EncodedNumber? {
        val separator = indexOf(':', start)

        if (separator < 0) {
            return null
        }

        val value = substring(start, separator).toIntOrNull()?.takeIf { it >= 0 } ?: return null
        return EncodedNumber(value = value, nextPosition = separator + 1)
    }

    private data class EncodedNumber(
        val value: Int,
        val nextPosition: Int
    )
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
