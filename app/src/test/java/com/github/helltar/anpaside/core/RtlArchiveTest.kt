package com.github.helltar.anpaside.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.security.GeneralSecurityException

class RtlArchiveTest {

    @Test
    fun bundledArchiveContainsEveryClass() {
        val archive = readBundledArchive()

        assertEquals(RtlArchive.expectedClassNames, RtlArchive.read(archive).keys)
    }

    @Test(expected = GeneralSecurityException::class)
    fun rejectsModifiedArchive() {
        val archive = readBundledArchive()
        archive[archive.lastIndex] = (archive.last().toInt() xor 1).toByte()

        RtlArchive.read(archive)
    }

    private fun readBundledArchive(): ByteArray {
        return File("src/main/assets/rtl/${RtlArchive.FILE_NAME}").readBytes()
    }
}
