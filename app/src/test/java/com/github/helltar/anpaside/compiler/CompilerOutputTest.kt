package com.github.helltar.anpaside.compiler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompilerOutputTest {

    @Test
    fun parsesDependencyMarkersIncludingLastLineWithoutNewline() {
        val output = "^0first\nnoise\n^0second\n^1sensor\n^2FW.class"

        assertEquals(listOf("first", "second"), CompilerOutput.units(output))
        assertEquals(listOf("sensor"), CompilerOutput.libraries(output))
        assertEquals(listOf("FW.class"), CompilerOutput.runtimeClasses(output))
    }

    @Test
    fun continuesRecordNumberingPastTheHighestClassAModuleWrote() {
        val output = "^3R_0.class\n^3R_1.class\n^3R_2.class"

        assertEquals(listOf("R_0.class", "R_1.class", "R_2.class"), CompilerOutput.recordClasses(output))
        assertEquals(3, CompilerOutput.nextRecordId(output, 0))
    }

    @Test
    fun keepsTheRecordCounterWhenAModuleDeclaresNoRecords() {
        assertEquals(4, CompilerOutput.nextRecordId("^2F.class", 4))
        assertEquals(0, CompilerOutput.nextRecordId("", 0))
    }

    @Test
    fun neverMovesTheRecordCounterBackwards() {
        // a module numbered from -r 5 reports R_5, which must not pull the counter down to 6
        assertEquals(9, CompilerOutput.nextRecordId("^3R_5.class", 9))
        assertEquals(6, CompilerOutput.nextRecordId("^3R_5.class", 2))
    }

    @Test
    fun detectsEveryCompilerFailureMarker() {
        assertTrue(CompilerOutput.hasErrors("[Pascal Error] main.pas(2): bad token"))
        assertTrue(CompilerOutput.hasErrors("[Compiler Error] internal failure"))
        assertTrue(CompilerOutput.hasErrors("Fatal error: out of memory"))
        assertFalse(CompilerOutput.hasErrors("Compiled successfully"))
    }

    @Test
    fun cleansProgressAndInternalMarkersForTheLog() {
        val output = """
            @25
            [Pascal Error] main.pas(7): bad token
            ^1sensor
            ^2FW.class
            ^3Record1
        """.trimIndent()

        assertEquals(
            "main.pas(7): bad token\nLib: sensor\nFW.class\nRecord1",
            CompilerOutput.clean(output)
        )
    }
}
