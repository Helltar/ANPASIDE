package com.github.helltar.anpaside.ui.editor

internal enum class TokenKind { KEYWORD, STRING, NUMBER, COMMENT }

internal data class PascalToken(val kind: TokenKind, val start: Int, val end: Int)

// the part of the source, in source offsets, that is worth painting
internal data class HighlightWindow(val start: Int, val end: Int) {
    companion object {
        val ALL = HighlightWindow(0, Int.MAX_VALUE)
    }
}

internal object PascalHighlighter {

    private val keywordsByLength = setOf(
        "and", "array", "begin", "break", "bytecode", "case", "const", "div",
        "do", "downto", "else", "end", "exit", "false", "file", "finalization",
        "for", "forever", "forward", "function", "if", "implementation", "in",
        "initialization", "inline", "interface", "mod", "not", "of", "or", "packed",
        "procedure", "program", "record", "repeat", "result", "set", "shl", "shr",
        "then", "to", "true", "type", "unit", "until", "uses", "ushr", "var",
        "while", "with", "xor"
    ).groupBy(String::length)

    // the whole text is always scanned, a comment opened far above still has to be known about,
    // but only tokens inside the window are reported: painting thousands of them on every
    // keystroke is what made a long file slow to type in
    fun tokens(text: String, window: HighlightWindow = HighlightWindow.ALL): List<PascalToken> {
        val tokens = ArrayList<PascalToken>()

        fun mark(kind: TokenKind, start: Int, end: Int) {
            if (end > window.start && start < window.end) {
                tokens += PascalToken(kind, start, end)
            }
        }

        var offset = 0

        while (offset < text.length) {
            val start = offset

            when {
                text[offset] == '\'' || text[offset] == '"' -> {
                    offset = quotedStringEnd(text, offset)
                    mark(TokenKind.STRING, start, offset)
                }

                text[offset] == '#' -> {
                    offset = characterCodeEnd(text, offset)
                    mark(TokenKind.STRING, start, offset)
                }

                text.startsWith("/*", offset) -> {
                    val end = text.indexOf("*/", offset + 2)
                    offset = if (end < 0) text.length else end + 2
                    mark(TokenKind.COMMENT, start, offset)
                }

                text.startsWith("(*", offset) -> {
                    val end = text.indexOf("*)", offset + 2)
                    offset = if (end < 0) text.length else end + 2
                    mark(TokenKind.COMMENT, start, offset)
                }

                text[offset] == '{' -> {
                    val end = text.indexOf('}', offset + 1)
                    offset = if (end < 0) text.length else end + 1
                    mark(TokenKind.COMMENT, start, offset)
                }

                text.startsWith("//", offset) -> {
                    offset = lineEnd(text, offset + 2)
                    mark(TokenKind.COMMENT, start, offset)
                }

                text[offset] == '$' && offset + 1 < text.length && text[offset + 1].isLetterOrDigit() -> {
                    offset += 2

                    while (offset < text.length && text[offset].isLetterOrDigit()) {
                        offset++
                    }

                    mark(TokenKind.NUMBER, start, offset)
                }

                text[offset].isDigit() -> {
                    offset = numberEnd(text, offset)

                    if (offset == text.length || !text[offset].isIdentifierPart()) {
                        mark(TokenKind.NUMBER, start, offset)
                    }
                }

                text[offset].isIdentifierStart() -> {
                    offset++

                    while (offset < text.length && text[offset].isIdentifierPart()) {
                        offset++
                    }

                    if (isKeyword(text, start, offset)) {
                        mark(TokenKind.KEYWORD, start, offset)
                    }
                }

                else -> offset++
            }
        }

        return tokens
    }

    private fun quotedStringEnd(text: String, start: Int): Int {
        val quote = text[start]
        var offset = start + 1

        while (offset < text.length && text[offset] != '\n' && text[offset] != '\r') {
            if (text[offset] != quote) {
                offset++
                continue
            }

            // midletpascal escapes the active quote by doubling it: 'It''s'.
            if (offset + 1 < text.length && text[offset + 1] == quote) {
                offset += 2
            } else {
                return offset + 1
            }
        }

        return offset
    }

    private fun characterCodeEnd(text: String, start: Int): Int {
        var offset = start + 1

        if (offset < text.length && text[offset] == '$') {
            offset++

            while (offset < text.length && text[offset].isLetterOrDigit()) {
                offset++
            }
        } else {
            while (offset < text.length && text[offset].isDigit()) {
                offset++
            }
        }

        return offset
    }

    private fun lineEnd(text: String, start: Int): Int {
        val end = text.indexOf('\n', start)
        return if (end < 0) text.length else end
    }

    private fun numberEnd(text: String, start: Int): Int {
        var offset = start

        while (offset < text.length && text[offset].isDigit()) {
            offset++
        }

        if (offset < text.length && text[offset] == '.' &&
            (offset + 1 == text.length || text[offset + 1] != '.')
        ) {
            offset++

            while (offset < text.length && text[offset].isDigit()) {
                offset++
            }
        }

        return offset
    }

    private fun isKeyword(text: String, start: Int, end: Int): Boolean =
        keywordsByLength[end - start]?.any { keyword ->
            text.regionMatches(start, keyword, 0, keyword.length, ignoreCase = true)
        } == true

    private fun Char.isIdentifierStart() = this == '_' || isLetter()

    private fun Char.isIdentifierPart() = this == '_' || isLetterOrDigit()
}
