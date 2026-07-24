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
