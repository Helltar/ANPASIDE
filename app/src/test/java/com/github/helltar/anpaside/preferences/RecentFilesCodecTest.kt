package com.github.helltar.anpaside.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentFilesCodecTest {

    @Test
    fun roundTripsPathsWithoutDelimiterRestrictions() {
        val paths =
            listOf(
                "/projects/a, b/src/main.pas",
                "/projects/colon:project/src/юніт.pas",
                "/projects/multiline/src/a\nb.pas"
            )

        assertEquals(paths, RecentFilesCodec.decode(RecentFilesCodec.encode(paths)))
    }

    @Test
    fun rejectsTruncatedOrMalformedData() {
        assertEquals(emptyList<String>(), RecentFilesCodec.decode("x:path"))
        assertEquals(emptyList<String>(), RecentFilesCodec.decode("10:short"))
        assertEquals(emptyList<String>(), RecentFilesCodec.decode("-1:"))
        assertEquals(emptyList<String>(), RecentFilesCodec.decode("2147483647:path"))
    }
}

class FoldingStateCodecTest {

    @Test
    fun roundTripsPathsAndSortedOffsets() {
        val states =
            mapOf(
                "/projects/a: b/src/main.pas" to setOf(42, 5, 100),
                "/projects/юніт/src/a\nb.pas" to setOf(0, 12)
            )

        assertEquals(states, FoldingStateCodec.decode(FoldingStateCodec.encode(states)))
    }

    @Test
    fun omitsEmptyStatesAndRejectsMalformedData() {
        assertEquals("", FoldingStateCodec.encode(mapOf("/main.pas" to emptySet())))
        assertEquals(emptyMap<String, Set<Int>>(), FoldingStateCodec.decode("x:path"))
        assertEquals(emptyMap<String, Set<Int>>(), FoldingStateCodec.decode("10:short"))
        assertEquals(emptyMap<String, Set<Int>>(), FoldingStateCodec.decode("4:path2:1:"))
        assertEquals(emptyMap<String, Set<Int>>(), FoldingStateCodec.decode("4:path1:-1:"))
    }
}
