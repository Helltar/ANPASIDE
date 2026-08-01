package com.github.helltar.anpaside.ui.editor

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PascalFoldingTest {

    @Test
    fun findsNestedBeginBlocksAroundCase() {
        val source = """
            begin
                case value of
                    1: begin
                        value := 2;
                    end;
                end;
            end.
        """.trimIndent()

        val blocks = PascalFolding.findBlocks(source)

        assertEquals(listOf(1 to 7, 3 to 5), blocks.map { it.startLine to it.endLine })
    }

    @Test
    fun ignoresKeywordsInsideStringsAndComments() {
        val source = """
            begin
                text := 'begin end';
                other := "end begin";
                // begin end
                { begin end }
                (* begin
                   end *)
                /* begin end */
            end.
        """.trimIndent()

        val blocks = PascalFolding.findBlocks(source)

        assertEquals(1, blocks.size)
        assertEquals(1, blocks.single().startLine)
        assertEquals(9, blocks.single().endLine)
    }

    @Test
    fun handlesRecordsAndSkipsSingleLineBlocks() {
        val source = """
            type Item = record
                value: Integer;
            end;

            begin begin end;
            end.
        """.trimIndent()

        val blocks = PascalFolding.findBlocks(source)

        assertEquals(1, blocks.size)
        assertEquals(5, blocks.single().startLine)
        assertEquals(6, blocks.single().endLine)
    }

    @Test
    fun replacesHiddenSourceAndMapsOffsets() {
        val source = "begin\n    value := 1;\nend;\nafter"
        val block = PascalFolding.findBlocks(source).single()
        val transformed = PascalFolding.transform(AnnotatedString(source), listOf(block))

        assertEquals("begin … end;\nafter", transformed.text.text)
        assertEquals(block.hiddenStart, transformed.offsetMapping.originalToTransformed(block.hiddenStart))
        assertEquals(8, transformed.offsetMapping.originalToTransformed(block.hiddenStart + 1))
        assertEquals(8, transformed.offsetMapping.originalToTransformed(block.hiddenEnd))
        assertEquals(block.hiddenStart, transformed.offsetMapping.transformedToOriginal(5))
        assertEquals(block.hiddenEnd, transformed.offsetMapping.transformedToOriginal(6))
        assertEquals(block.hiddenEnd, transformed.offsetMapping.transformedToOriginal(8))
    }

    @Test
    fun outerFoldHidesNestedFoldMarkerWithoutDiscardingItsState() {
        val source = """
            begin
                begin
                    value := 1;
                end;
            end.
        """.trimIndent()
        val blocks = PascalFolding.findBlocks(source)
        val starts = blocks.mapTo(mutableSetOf(), PascalFoldBlock::startOffset)

        val active = PascalFolding.activeBlocks(blocks, starts)
        val visible = PascalFolding.visibleBlocks(blocks, active)

        assertEquals(listOf(blocks.first()), active)
        assertEquals(listOf(blocks.first()), visible)
        assertTrue(starts.contains(blocks.last().startOffset))
    }
}
