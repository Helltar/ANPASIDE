package com.github.helltar.anpaside.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFileTest {

    @Test
    fun selectionChangesDoNotMarkFileAsModified() {
        val file = OpenFile("/project/src/main.pas", "begin\nend.")

        file.onValueChange(file.value.copy(selection = TextRange(6)))

        assertFalse(file.isModified)
        assertEquals(TextRange(6), file.value.selection)
    }

    @Test
    fun insertReplacesSelectionAndMovesCaret() {
        val file = OpenFile("/project/src/main.pas", "old value")
        file.onValueChange(TextFieldValue(file.value.text, TextRange(0, 3)))

        file.insert("new")

        assertEquals("new value", file.value.text)
        assertEquals(TextRange(3), file.value.selection)
        assertTrue(file.isModified)
    }

    @Test
    fun externalCaretMoveTargetsRequestedSourceLine() {
        val file = OpenFile("/project/src/main.pas", "one\ntwo\nthree")

        file.moveCaretToLine(3)

        assertEquals(TextRange(8), file.value.selection)
        assertEquals(1, file.caretRequest)
        assertFalse(file.isModified)
    }
}
