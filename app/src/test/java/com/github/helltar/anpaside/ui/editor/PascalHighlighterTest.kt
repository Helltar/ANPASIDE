package com.github.helltar.anpaside.ui.editor

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
        val highlighted = PascalHighlighter.tokens(source)

        assertEquals(TokenKind.KEYWORD, highlighted.colorAt(source, "if"))
        assertEquals(TokenKind.KEYWORD, highlighted.colorAt(source, "true"))
        assertEquals(TokenKind.KEYWORD, highlighted.colorAt(source, "then"))
        assertEquals(TokenKind.STRING, highlighted.colorAt(source, "begin"))
        assertEquals(TokenKind.STRING, highlighted.colorAt(source, "12"))
        assertEquals(TokenKind.COMMENT, highlighted.colorAt(source, "end"))
        assertEquals(TokenKind.COMMENT, highlighted.colorAt(source, "var"))
        assertEquals(TokenKind.KEYWORD, highlighted.colorAt(source, "result"))
        assertEquals(TokenKind.STRING, highlighted.colorAt(source, "#65"))
    }

    @Test
    fun paintsOnlyTheWindowButStillKnowsWhatWasOpenedAboveIt() {
        val source = "begin\n{ a comment\nthat goes on }\nend"
        val window = HighlightWindow(source.indexOf("that"), source.length)
        val highlighted = PascalHighlighter.tokens(source, window)

        assertEquals(TokenKind.COMMENT, highlighted.colorAt(source, "goes"))
        assertEquals(TokenKind.KEYWORD, highlighted.colorAt(source, "end"))
        assertEquals(
            0,
            highlighted.count { it.end <= window.start }
        )
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
        val highlighted = PascalHighlighter.tokens(source)

        assertEquals(TokenKind.STRING, highlighted.colorAt(source, "It''s"))
        assertEquals(TokenKind.STRING, highlighted.colorAt(source, "\"begin\""))
        assertEquals(TokenKind.COMMENT, highlighted.colorAt(source, "slash"))
        assertEquals(TokenKind.COMMENT, highlighted.colorAt(source, "paren"))
        assertEquals(TokenKind.COMMENT, highlighted.colorAt(source, "brace"))
    }

    @Test
    fun highlightsCompilerNumberFormsWithoutTouchingIdentifiers() {
        val source = "a := 12; b := 3.14; c := 1..2; d := ${'$'}FF; abc12 := 0;"
        val highlighted = PascalHighlighter.tokens(source)

        assertEquals(TokenKind.NUMBER, highlighted.colorAt(source, "12"))
        assertEquals(TokenKind.NUMBER, highlighted.colorAt(source, "3.14"))
        assertEquals(TokenKind.NUMBER, highlighted.colorAt(source, "1.."))
        assertEquals(TokenKind.NUMBER, highlighted.colorAt(source, "2; d"))
        assertEquals(TokenKind.NUMBER, highlighted.colorAt(source, "${'$'}FF"))
        assertEquals(null, highlighted.colorAt(source, "abc12"))
    }

    private fun List<PascalToken>.colorAt(source: String, marker: String): TokenKind? {
        val offset = source.indexOf(marker)
        require(offset >= 0) { "Marker not found: $marker" }

        return firstOrNull { offset >= it.start && offset < it.end }?.kind
    }
}
