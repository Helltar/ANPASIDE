package com.github.helltar.anpaside.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.github.helltar.anpaside.ui.theme.DarkSyntaxColors
import org.junit.Assert.assertEquals
import org.junit.Test

class PascalHighlighterTest {

    @Test
    fun usesCompilerTokenPrecedence() {
        val source = """
            if true then
                s := 'begin 12';
                // end
                { var }
                result := #65;
        """.trimIndent()
        val highlighted = PascalHighlighter.highlight(source, DarkSyntaxColors)

        assertEquals(DarkSyntaxColors.keyword, highlighted.colorAt(source, "if"))
        assertEquals(DarkSyntaxColors.keyword, highlighted.colorAt(source, "true"))
        assertEquals(DarkSyntaxColors.keyword, highlighted.colorAt(source, "then"))
        assertEquals(DarkSyntaxColors.string, highlighted.colorAt(source, "begin"))
        assertEquals(DarkSyntaxColors.string, highlighted.colorAt(source, "12"))
        assertEquals(DarkSyntaxColors.comment, highlighted.colorAt(source, "end"))
        assertEquals(DarkSyntaxColors.comment, highlighted.colorAt(source, "var"))
        assertEquals(DarkSyntaxColors.keyword, highlighted.colorAt(source, "result"))
        assertEquals(DarkSyntaxColors.string, highlighted.colorAt(source, "#65"))
    }

    @Test
    fun supportsEveryCompilerCommentAndStringForm() {
        val source = """
            a := 'It''s';
            b := "begin";
            /* slash */
            (* paren *)
            { brace }
        """.trimIndent()
        val highlighted = PascalHighlighter.highlight(source, DarkSyntaxColors)

        assertEquals(DarkSyntaxColors.string, highlighted.colorAt(source, "It''s"))
        assertEquals(DarkSyntaxColors.string, highlighted.colorAt(source, "\"begin\""))
        assertEquals(DarkSyntaxColors.comment, highlighted.colorAt(source, "slash"))
        assertEquals(DarkSyntaxColors.comment, highlighted.colorAt(source, "paren"))
        assertEquals(DarkSyntaxColors.comment, highlighted.colorAt(source, "brace"))
    }

    @Test
    fun highlightsCompilerNumberFormsWithoutTouchingIdentifiers() {
        val source = "a := 12; b := 3.14; c := 1..2; d := ${'$'}FF; abc12 := 0;"
        val highlighted = PascalHighlighter.highlight(source, DarkSyntaxColors)

        assertEquals(DarkSyntaxColors.number, highlighted.colorAt(source, "12"))
        assertEquals(DarkSyntaxColors.number, highlighted.colorAt(source, "3.14"))
        assertEquals(DarkSyntaxColors.number, highlighted.colorAt(source, "1.."))
        assertEquals(DarkSyntaxColors.number, highlighted.colorAt(source, "2; d"))
        assertEquals(DarkSyntaxColors.number, highlighted.colorAt(source, "${'$'}FF"))
        assertEquals(Color.Unspecified, highlighted.colorAt(source, "abc12"))
    }

    private fun AnnotatedString.colorAt(source: String, marker: String): Color {
        val offset = source.indexOf(marker)
        require(offset >= 0) { "Marker not found: $marker" }

        return spanStyles.firstOrNull { offset >= it.start && offset < it.end }
            ?.item
            ?.color
            ?: Color.Unspecified
    }
}
