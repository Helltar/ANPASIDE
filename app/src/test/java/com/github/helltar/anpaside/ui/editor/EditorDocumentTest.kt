package com.github.helltar.anpaside.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorDocumentTest {

    @Test
    fun selectionChangesDoNotMarkFileAsModified() {
        val file = EditorDocument("/project/src/main.pas", "begin\nend.")

        file.onValueChange(file.value.copy(selection = TextRange(6)))

        assertFalse(file.isModified)
        assertEquals(TextRange(6), file.value.selection)
    }

    @Test
    fun insertReplacesSelectionAndMovesCaret() {
        val file = EditorDocument("/project/src/main.pas", "old value")
        file.onValueChange(TextFieldValue(file.value.text, TextRange(0, 3)))

        file.insertText("new")

        assertEquals("new value", file.value.text)
        assertEquals(TextRange(3), file.value.selection)
        assertTrue(file.isModified)
    }

    @Test
    fun externalCaretMoveTargetsRequestedSourceLine() {
        val file = EditorDocument("/project/src/main.pas", "one\ntwo\nthree")

        file.moveCaretToLine(3)

        assertEquals(TextRange(8), file.value.selection)
        assertEquals(1, file.caretRequest)
        assertFalse(file.isModified)
    }

    @Test
    fun staleSaveDoesNotClearNewerEdits() {
        val file = EditorDocument("/project/src/main.pas", "old")
        file.onValueChange(TextFieldValue("first edit"))
        val snapshot = file.snapshot()
        file.onValueChange(TextFieldValue("second edit"))

        file.markSaved(snapshot)

        assertTrue(file.isModified)
        assertEquals("second edit", file.value.text)
    }

    @Test
    fun relocationPreservesEditorState() {
        val file = EditorDocument("/project/src/main.pas", "source")
        file.onValueChange(TextFieldValue("edited", TextRange(3)))

        file.relocate("/project/src/game.pas")

        assertEquals("game.pas", file.name)
        assertEquals("edited", file.value.text)
        assertEquals(TextRange(3), file.value.selection)
        assertTrue(file.isModified)
    }

    @Test
    fun collapsedBlockMovesWithAnEditBeforeIt() {
        val source = "header\nbegin\n    value := 1;\nend."
        val file = EditorDocument("/project/src/main.pas", source)
        val block = PascalFolding.findBlocks(source).single()
        file.toggleFold(block)

        file.onValueChange(TextFieldValue("x$source"))

        assertEquals(setOf(block.startOffset + 1), file.collapsedFoldStarts)
    }

    @Test
    fun externalCaretMoveExpandsContainingBlock() {
        val source = "begin\n    value := 1;\nend."
        val file = EditorDocument("/project/src/main.pas", source)
        file.toggleFold(PascalFolding.findBlocks(source).single())

        file.moveCaretToLine(2)

        assertTrue(file.collapsedFoldStarts.isEmpty())
        assertEquals(TextRange(6), file.value.selection)
        assertFalse(file.isModified)
    }

    @Test
    fun editInsideCollapsedBlockExpandsIt() {
        val source = "begin\n    value := 1;\nend."
        val file = EditorDocument("/project/src/main.pas", source)
        val block = PascalFolding.findBlocks(source).single()
        file.toggleFold(block)
        val insertion = source.indexOf("value")

        file.onValueChange(TextFieldValue(source.substring(0, insertion) + "x" + source.substring(insertion)))

        assertTrue(file.collapsedFoldStarts.isEmpty())
        assertTrue(file.isModified)
    }

    @Test
    fun movingCaretToFoldPlaceholderExpandsIt() {
        val source = "begin\n    value := 1;\nend."
        val file = EditorDocument("/project/src/main.pas", source)
        val block = PascalFolding.findBlocks(source).single()
        file.toggleFold(block)

        file.onValueChange(file.value.copy(selection = TextRange(block.hiddenEnd)))

        assertTrue(file.collapsedFoldStarts.isEmpty())
        assertFalse(file.isModified)
    }
}
