package com.github.helltar.anpaside.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText

private const val FOLD_PLACEHOLDER = " … "

internal data class PascalFoldBlock(
    val startOffset: Int,
    val startTokenEnd: Int,
    val endOffset: Int,
    val startLine: Int,
    val endLine: Int
) {
    val hiddenStart: Int
        get() = startTokenEnd

    val hiddenEnd: Int
        get() = endOffset
}

internal object PascalFolding {

    fun findBlocks(text: String): List<PascalFoldBlock> {
        val openers = ArrayDeque<Opener>()
        val blocks = mutableListOf<PascalFoldBlock>()
        var offset = 0
        var line = 1

        while (offset < text.length) {
            val start = offset
            val end =
                when {
                    text[offset] == '\'' || text[offset] == '"' -> quotedStringEnd(text, offset)
                    text.startsWith("/*", offset) -> delimitedEnd(text, offset + 2, "*/")
                    text.startsWith("(*", offset) -> delimitedEnd(text, offset + 2, "*)")
                    text[offset] == '{' -> delimitedEnd(text, offset + 1, "}")
                    text.startsWith("//", offset) -> lineEnd(text, offset + 2)
                    text[offset].isIdentifierStart() -> identifierEnd(text, offset)
                    else -> offset + 1
                }

            if (text[offset].isIdentifierStart()) {
                when {
                    text.tokenEquals(offset, end, "begin") -> {
                        openers.addLast(Opener(OpenerKind.BEGIN, offset, end, line))
                    }

                    text.tokenEquals(offset, end, "case") -> {
                        // a variant record's case shares the record's end instead of owning one
                        if (openers.lastOrNull()?.kind != OpenerKind.RECORD) {
                            openers.addLast(Opener(OpenerKind.CASE, offset, end, line))
                        }
                    }

                    text.tokenEquals(offset, end, "record") -> {
                        openers.addLast(Opener(OpenerKind.RECORD, offset, end, line))
                    }

                    text.tokenEquals(offset, end, "end") -> {
                        val opener = if (openers.isEmpty()) null else openers.removeLast()

                        if (opener?.kind == OpenerKind.BEGIN && opener.line < line) {
                            blocks += PascalFoldBlock(
                                startOffset = opener.offset,
                                startTokenEnd = opener.tokenEnd,
                                endOffset = offset,
                                startLine = opener.line,
                                endLine = line
                            )
                        }
                    }
                }
            }

            line += text.countNewlines(start, end)
            offset = end
        }

        return blocks.sortedBy(PascalFoldBlock::startOffset)
    }

    fun activeBlocks(
        blocks: List<PascalFoldBlock>,
        collapsedStarts: Set<Int>
    ): List<PascalFoldBlock> {
        val active = mutableListOf<PascalFoldBlock>()

        for (block in blocks) {
            if (block.startOffset !in collapsedStarts) {
                continue
            }

            if (active.none { block.startOffset in it.hiddenStart until it.hiddenEnd }) {
                active += block
            }
        }

        return active
    }

    fun visibleBlocks(
        blocks: List<PascalFoldBlock>,
        activeBlocks: List<PascalFoldBlock>
    ): List<PascalFoldBlock> =
        blocks.filter { block ->
            activeBlocks.none { folded ->
                folded.startOffset != block.startOffset &&
                        block.startOffset in folded.hiddenStart until folded.hiddenEnd
            }
        }

    fun transform(
        text: AnnotatedString,
        activeBlocks: List<PascalFoldBlock>
    ): TransformedText {
        val blocks = activeBlocks.filter { block ->
            block.hiddenStart in 0..text.length &&
                    block.hiddenEnd in 0..text.length &&
                    block.hiddenStart < block.hiddenEnd
        }

        if (blocks.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder()
        val replacements = ArrayList<FoldReplacement>(blocks.size)
        var sourceOffset = 0
        var transformedOffset = 0

        for (block in blocks) {
            if (block.hiddenStart < sourceOffset) {
                continue
            }

            builder.append(text.subSequence(sourceOffset, block.hiddenStart))
            transformedOffset += block.hiddenStart - sourceOffset
            val replacementStart = transformedOffset
            builder.append(FOLD_PLACEHOLDER)
            transformedOffset += FOLD_PLACEHOLDER.length
            replacements += FoldReplacement(
                originalStart = block.hiddenStart,
                originalEnd = block.hiddenEnd,
                transformedStart = replacementStart,
                transformedEnd = transformedOffset
            )
            sourceOffset = block.hiddenEnd
        }

        builder.append(text.subSequence(sourceOffset, text.length))

        return TransformedText(
            text = builder.toAnnotatedString(),
            offsetMapping = FoldOffsetMapping(replacements)
        )
    }

    private data class Opener(
        val kind: OpenerKind,
        val offset: Int,
        val tokenEnd: Int,
        val line: Int
    )

    private enum class OpenerKind {
        BEGIN,
        CASE,
        RECORD
    }

    private fun quotedStringEnd(text: String, start: Int): Int {
        val quote = text[start]
        var offset = start + 1

        while (offset < text.length && text[offset] != '\n' && text[offset] != '\r') {
            if (text[offset] != quote) {
                offset++
                continue
            }

            if (offset + 1 < text.length && text[offset + 1] == quote) {
                offset += 2
            } else {
                return offset + 1
            }
        }

        return offset
    }

    private fun delimitedEnd(text: String, contentStart: Int, delimiter: String): Int {
        val end = text.indexOf(delimiter, contentStart)
        return if (end < 0) text.length else end + delimiter.length
    }

    private fun lineEnd(text: String, contentStart: Int): Int {
        val end = text.indexOf('\n', contentStart)
        return if (end < 0) text.length else end
    }

    private fun identifierEnd(text: String, start: Int): Int {
        var offset = start + 1

        while (offset < text.length && text[offset].isIdentifierPart()) {
            offset++
        }

        return offset
    }

    private fun String.countNewlines(start: Int, end: Int): Int {
        var result = 0

        for (offset in start until end) {
            if (this[offset] == '\n') {
                result++
            }
        }

        return result
    }

    private fun String.tokenEquals(start: Int, end: Int, keyword: String): Boolean =
        end - start == keyword.length &&
                regionMatches(start, keyword, 0, keyword.length, ignoreCase = true)

    private fun Char.isIdentifierStart() = this == '_' || isLetter()

    private fun Char.isIdentifierPart() = this == '_' || isLetterOrDigit()
}

private data class FoldReplacement(
    val originalStart: Int,
    val originalEnd: Int,
    val transformedStart: Int,
    val transformedEnd: Int
)

private class FoldOffsetMapping(
    private val replacements: List<FoldReplacement>
) : OffsetMapping {

    override fun originalToTransformed(offset: Int): Int {
        var adjustment = 0

        for (replacement in replacements) {
            if (offset < replacement.originalStart) {
                break
            }

            if (offset <= replacement.originalEnd) {
                return if (offset == replacement.originalStart) {
                    replacement.transformedStart
                } else {
                    replacement.transformedEnd
                }
            }

            adjustment +=
                replacement.transformedEnd - replacement.transformedStart -
                        (replacement.originalEnd - replacement.originalStart)
        }

        return offset + adjustment
    }

    override fun transformedToOriginal(offset: Int): Int {
        var adjustment = 0

        for (replacement in replacements) {
            if (offset < replacement.transformedStart) {
                break
            }

            if (offset <= replacement.transformedEnd) {
                return if (offset == replacement.transformedStart) {
                    replacement.originalStart
                } else {
                    replacement.originalEnd
                }
            }

            adjustment +=
                replacement.originalEnd - replacement.originalStart -
                        (replacement.transformedEnd - replacement.transformedStart)
        }

        return offset + adjustment
    }
}
