package com.github.helltar.anpaside.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.helltar.anpaside.core.IdeLog
import java.io.File
import java.io.IOException

// one open file = one editor tab
class OpenFile(val path: String, text: String) {

    val name: String = File(path).name

    var value by mutableStateOf(TextFieldValue(text))
        private set

    var isModified by mutableStateOf(false)
        private set

    // bumped whenever the caret is moved from outside the editor, so that the editor
    // knows it has to scroll to it
    var caretRequest by mutableIntStateOf(0)
        private set

    fun onValueChange(newValue: TextFieldValue) {
        if (newValue.text != value.text) {
            isModified = true
        }

        value = newValue
    }

    fun insert(str: String) {
        val start = minOf(value.selection.start, value.selection.end)
        val end = maxOf(value.selection.start, value.selection.end)
        val text = value.text.substring(0, start) + str + value.text.substring(end)

        onValueChange(TextFieldValue(text, TextRange(start + str.length)))
    }

    // moves the caret only, the text is untouched, so the file does not become modified
    fun moveCaretToLine(line: Int) {
        value = value.copy(selection = TextRange(offsetOfLine(line)))
        caretRequest++
    }

    fun save(): Boolean =
        try {
            File(path).writeText(value.text)
            isModified = false
            true
        } catch (e: IOException) {
            IdeLog.error(e)
            false
        }

    private fun offsetOfLine(line: Int): Int {
        val text = value.text
        var offset = 0

        repeat(line - 1) {
            val lineEnd = text.indexOf('\n', offset)

            if (lineEnd < 0) {
                return offset
            }

            offset = lineEnd + 1
        }

        return offset.coerceAtMost(text.length)
    }
}
