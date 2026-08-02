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
    fun findsWholeProcedureAndFunctionAroundTheirBeginBlocks() {
        val source = """
            procedure updateValue(value: Integer);
            var
                nextValue: Integer;
            begin
                nextValue := value + 1;
            end;

            function doubled(
                value: Integer;
                extra: Integer
            ): Integer;
            begin
                doubled := value * 2 + extra;
            end;
        """.trimIndent()

        val blocks = PascalFolding.findBlocks(source)

        assertEquals(
            listOf(1 to 6, 4 to 6, 8 to 14, 12 to 14),
            blocks.map { it.startLine to it.endLine }
        )
    }

    @Test
    fun findsRoutineWhenItsBeginBlockIsOnOneLine() {
        val source = """
            procedure updateValue;
            begin value := 1; end;
        """.trimIndent()

        val blocks = PascalFolding.findBlocks(source)

        assertEquals(listOf(1 to 2), blocks.map { it.startLine to it.endLine })
    }

    @Test
    fun routineFoldKeepsHeaderAndClosingEndVisible() {
        val source = """
            function updatedValue(value: Integer; extra: Integer): Integer;
            var resultValue: Integer;
            begin
                resultValue := value + extra;
                updatedValue := resultValue;
            end;
        """.trimIndent()
        val routine = PascalFolding.findBlocks(source).first()

        val transformed = PascalFolding.transform(AnnotatedString(source), listOf(routine))

        assertEquals(
            "function updatedValue(value: Integer; extra: Integer): Integer; … end;",
            transformed.text.text
        )
    }

    @Test
    fun skipsInterfaceAndForwardRoutineDeclarations() {
        val source = """
            unit values;
            interface
            procedure declaredOnly;
            function calculated: Integer;
            implementation

            procedure declaredOnly;
            begin
                value := 1;
            end;

            procedure forwarded; forward;

            begin
                declaredOnly;
            end.
        """.trimIndent()

        val blocks = PascalFolding.findBlocks(source)
        val routineBlocks = blocks.filter { block ->
            val isProcedure = source.regionMatches(
                thisOffset = block.startOffset,
                other = "procedure",
                otherOffset = 0,
                length = "procedure".length,
                ignoreCase = true
            )
            val isFunction = source.regionMatches(
                thisOffset = block.startOffset,
                other = "function",
                otherOffset = 0,
                length = "function".length,
                ignoreCase = true
            )
            isProcedure || isFunction
        }

        assertEquals(1, routineBlocks.size)
        assertEquals(7 to 10, routineBlocks.single().let { it.startLine to it.endLine })
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
