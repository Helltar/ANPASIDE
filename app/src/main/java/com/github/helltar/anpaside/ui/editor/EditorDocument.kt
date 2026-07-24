package com.github.helltar.anpaside.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.io.File

data class DocumentSnapshot(
    val path: String,
    val text: String,
    internal val revision: Int
)

// one editor document maps to one tab and keeps ui-only selection state
class EditorDocument(path: String, text: String) {

    var path by mutableStateOf(path)
        private set

    val name: String
        get() = File(path).name

    var value by mutableStateOf(TextFieldValue(text))
        private set

    var isModified by mutableStateOf(false)
        private set

    private var revision = 0

    var verticalScrollOffset: Int = 0
        private set

    var horizontalScrollOffset: Int = 0
        private set

    // bumped whenever the caret is moved from outside the editor, so that the editor
    // knows it has to scroll to it
    var caretRequest by mutableIntStateOf(0)
        private set

    fun onValueChange(newValue: TextFieldValue) {
        if (newValue.text != value.text) {
            revision++
            isModified = true
        }

        value = newValue
    }

    fun insertText(insertedText: String) {
        val start = minOf(value.selection.start, value.selection.end)
        val end = maxOf(value.selection.start, value.selection.end)
        val text =
            value.text.substring(0, start) +
                    insertedText +
                    value.text.substring(end)

        onValueChange(TextFieldValue(text, TextRange(start + insertedText.length)))
    }

    // moves the caret only, the text is untouched, so the file does not become modified
    fun moveCaretToLine(line: Int) {
        value = value.copy(selection = TextRange(offsetOfLine(line)))
        caretRequest++
    }

    fun snapshot(): DocumentSnapshot =
        DocumentSnapshot(path = path, text = value.text, revision = revision)

    fun markSaved(snapshot: DocumentSnapshot) {
        if (snapshot.path == path && snapshot.revision == revision) {
            isModified = false
        }
    }

    fun relocate(newPath: String) {
        path = newPath
    }

    fun updateVerticalScroll(offset: Int) {
        verticalScrollOffset = offset
    }

    fun updateHorizontalScroll(offset: Int) {
        horizontalScrollOffset = offset
    }

    private fun offsetOfLine(line: Int): Int {
        val text = value.text
        var offset = 0

        repeat((line - 1).coerceAtLeast(0)) {
            val lineEnd = text.indexOf('\n', offset)

            if (lineEnd < 0) {
                return offset
            }

            offset = lineEnd + 1
        }

        return offset.coerceAtMost(text.length)
    }
}
