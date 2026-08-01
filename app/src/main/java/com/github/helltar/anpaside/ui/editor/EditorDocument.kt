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

// one editor document maps to one tab and keeps its selection and folding state
class EditorDocument(
    path: String,
    text: String,
    collapsedFoldStarts: Set<Int> = emptySet(),
    private val onFoldStateChange: (String, Set<Int>) -> Unit = { _, _ -> }
) {

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

    var collapsedFoldStarts by mutableStateOf(validFoldStarts(text, collapsedFoldStarts))
        private set

    // bumped whenever the caret is moved from outside the editor, so that the editor
    // knows it has to scroll to it
    var caretRequest by mutableIntStateOf(0)
        private set

    fun onValueChange(newValue: TextFieldValue) {
        if (newValue.text != value.text) {
            if (collapsedFoldStarts.isNotEmpty()) {
                val oldText = value.text
                val change = findTextChange(oldText, newValue.text)
                val touchedStarts = PascalFolding.findBlocks(oldText)
                    .filter { block ->
                        block.startOffset in collapsedFoldStarts && change.touches(block)
                    }
                    .mapTo(mutableSetOf(), PascalFoldBlock::startOffset)
                val remappedStarts = remapFoldStarts(
                    starts = collapsedFoldStarts - touchedStarts,
                    change = change,
                    adjustment = newValue.text.length - oldText.length
                )
                val validStarts = PascalFolding.findBlocks(newValue.text)
                    .mapTo(mutableSetOf(), PascalFoldBlock::startOffset)
                updateFoldStarts(remappedStarts.intersect(validStarts))
            }

            revision++
            isModified = true
        } else if (collapsedFoldStarts.isNotEmpty() && newValue.selection != value.selection) {
            val selectedFoldStarts = PascalFolding.findBlocks(value.text)
                .filter { block ->
                    block.startOffset in collapsedFoldStarts &&
                            (newValue.selection.start in block.hiddenStart..block.hiddenEnd ||
                                    newValue.selection.end in block.hiddenStart..block.hiddenEnd)
                }
                .mapTo(mutableSetOf(), PascalFoldBlock::startOffset)
            updateFoldStarts(collapsedFoldStarts - selectedFoldStarts)
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
        val offset = offsetOfLine(line)

        if (collapsedFoldStarts.isNotEmpty()) {
            val containingBlocks = PascalFolding.findBlocks(value.text).filter { block ->
                offset in block.hiddenStart until block.hiddenEnd
            }
            updateFoldStarts(
                collapsedFoldStarts - containingBlocks.map(PascalFoldBlock::startOffset).toSet()
            )
        }

        value = value.copy(selection = TextRange(offset))
        caretRequest++
    }

    internal fun toggleFold(block: PascalFoldBlock) {
        if (block.startOffset in collapsedFoldStarts) {
            updateFoldStarts(collapsedFoldStarts - block.startOffset)
            return
        }

        updateFoldStarts(collapsedFoldStarts + block.startOffset)

        if (value.selection.start in (block.hiddenStart + 1) until block.hiddenEnd ||
            value.selection.end in (block.hiddenStart + 1) until block.hiddenEnd
        ) {
            value = value.copy(selection = TextRange(block.hiddenStart))
        }
    }

    internal fun retainFoldStarts(validStarts: Set<Int>) {
        updateFoldStarts(collapsedFoldStarts.intersect(validStarts))
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

    private fun updateFoldStarts(starts: Set<Int>) {
        if (starts == collapsedFoldStarts) {
            return
        }

        collapsedFoldStarts = starts
        onFoldStateChange(path, starts)
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

    private fun remapFoldStarts(
        starts: Set<Int>,
        change: TextChange,
        adjustment: Int
    ): Set<Int> {
        if (starts.isEmpty()) {
            return starts
        }

        return starts.mapNotNullTo(mutableSetOf()) { start ->
            when {
                start < change.start -> start
                start >= change.oldEnd -> start + adjustment
                else -> null
            }
        }
    }

    private fun findTextChange(oldText: String, newText: String): TextChange {
        val commonLength = minOf(oldText.length, newText.length)
        var start = 0

        while (start < commonLength && oldText[start] == newText[start]) {
            start++
        }

        var suffixLength = 0

        while (suffixLength < commonLength - start &&
            oldText[oldText.length - suffixLength - 1] == newText[newText.length - suffixLength - 1]
        ) {
            suffixLength++
        }

        return TextChange(
            start = start,
            oldEnd = oldText.length - suffixLength
        )
    }

    private data class TextChange(
        val start: Int,
        val oldEnd: Int
    ) {
        fun touches(block: PascalFoldBlock): Boolean =
            if (start == oldEnd) {
                start in block.hiddenStart..block.hiddenEnd
            } else {
                start < block.hiddenEnd && oldEnd > block.hiddenStart
            }
    }
}

private fun validFoldStarts(text: String, starts: Set<Int>): Set<Int> {
    if (starts.isEmpty()) {
        return starts
    }

    val validStarts = PascalFolding.findBlocks(text)
        .mapTo(mutableSetOf(), PascalFoldBlock::startOffset)
    return starts.intersect(validStarts)
}
