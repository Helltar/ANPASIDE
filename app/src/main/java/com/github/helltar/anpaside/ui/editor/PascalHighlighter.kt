package com.github.helltar.anpaside.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.github.helltar.anpaside.ui.theme.SyntaxColors

object PascalHighlighter {

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
    // but only tokens inside the window get a span: laying out thousands of spans on every
    // keystroke is what made a long file slow to type in
    internal fun highlight(
        text: String,
        colors: SyntaxColors,
        window: HighlightWindow = HighlightWindow.ALL
    ): AnnotatedString {
        val builder = AnnotatedString.Builder(text)

        fun mark(style: SpanStyle, start: Int, end: Int) {
            if (end > window.start && start < window.end) {
                builder.addStyle(style, start, end)
            }
        }

        val stringStyle = SpanStyle(color = colors.string)
        val numberStyle = SpanStyle(color = colors.number)
        val keywordStyle = SpanStyle(color = colors.keyword)
        val commentStyle = SpanStyle(color = colors.comment)
        var offset = 0

        while (offset < text.length) {
            val start = offset

            when {
                text[offset] == '\'' || text[offset] == '"' -> {
                    offset = quotedStringEnd(text, offset)
                    mark(stringStyle, start, offset)
                }

                text[offset] == '#' -> {
                    offset = characterCodeEnd(text, offset)
                    mark(stringStyle, start, offset)
                }

                text.startsWith("/*", offset) -> {
                    val end = text.indexOf("*/", offset + 2)
                    offset = if (end < 0) text.length else end + 2
                    mark(commentStyle, start, offset)
                }

                text.startsWith("(*", offset) -> {
                    val end = text.indexOf("*)", offset + 2)
                    offset = if (end < 0) text.length else end + 2
                    mark(commentStyle, start, offset)
                }

                text[offset] == '{' -> {
                    val end = text.indexOf('}', offset + 1)
                    offset = if (end < 0) text.length else end + 1
                    mark(commentStyle, start, offset)
                }

                text.startsWith("//", offset) -> {
                    offset = lineEnd(text, offset + 2)
                    mark(commentStyle, start, offset)
                }

                text[offset] == '$' && offset + 1 < text.length && text[offset + 1].isLetterOrDigit() -> {
                    offset += 2

                    while (offset < text.length && text[offset].isLetterOrDigit()) {
                        offset++
                    }

                    mark(numberStyle, start, offset)
                }

                text[offset].isDigit() -> {
                    offset = numberEnd(text, offset)

                    if (offset == text.length || !text[offset].isIdentifierPart()) {
                        mark(numberStyle, start, offset)
                    }
                }

                text[offset].isIdentifierStart() -> {
                    offset++

                    while (offset < text.length && text[offset].isIdentifierPart()) {
                        offset++
                    }

                    if (isKeyword(text, start, offset)) {
                        mark(keywordStyle, start, offset)
                    }
                }

                else -> offset++
            }
        }

        return builder.toAnnotatedString()
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

// the part of the source, in source offsets, that is worth giving spans to
internal data class HighlightWindow(val start: Int, val end: Int) {
    companion object {
        val ALL = HighlightWindow(0, Int.MAX_VALUE)
    }
}

// syntax highlighting only paints; optional folding supplies its own source offset mapping
internal class PascalVisualTransformation(
    private val colors: SyntaxColors?,
    private val highlightWindow: HighlightWindow = HighlightWindow.ALL,
    private val activeFoldBlocks: List<PascalFoldBlock> = emptyList()
) : VisualTransformation {

    private var lastText: String? = null
    private var lastResult: AnnotatedString? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted =
            if (colors == null) {
                text
            } else {
                lastResult.takeIf { text.text == lastText }
                    ?: PascalHighlighter.highlight(text.text, colors, highlightWindow).also {
                        lastText = text.text
                        lastResult = it
                    }
            }

        return PascalFolding.transform(highlighted, activeFoldBlocks)
    }
}
