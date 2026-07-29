package com.github.helltar.anpaside.foundation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files

class FileSystemTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun atomicWriteReplacesContentsWithoutLeavingTemporaryFile() {
        val file = temporaryFolder.newFile("config.aproj").apply { writeText("old") }

        file.writeTextAtomically("new")

        assertEquals("new", file.readText())
        assertEquals(
            emptyList<String>(),
            requireNotNull(file.parentFile)
                .list()
                .orEmpty()
                .filter { it.startsWith(".config.aproj.") }
        )
    }

    // the workspace lives on external storage, where a file written owner-only stops being
    // readable from a pc over adb or MTP
    @Test
    fun atomicWriteLeavesTheSamePermissionsAsAnOrdinaryWrite() {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))

        val directory = temporaryFolder.newFolder("project")
        val ordinary = File(directory, "ordinary.aproj").apply { writeText("x") }
        val atomic = File(directory, "atomic.aproj")

        atomic.writeTextAtomically("x")

        assertEquals(
            Files.getPosixFilePermissions(ordinary.toPath()),
            Files.getPosixFilePermissions(atomic.toPath())
        )
    }

    @Test
    fun projectBoundaryRejectsSiblingsAndRoot() {
        val root = temporaryFolder.newFolder("project")
        val source = File(root, "src").apply { mkdir() }
        val sibling = temporaryFolder.newFile("outside.pas")

        assertEquals(source.canonicalFile, source.requireInside(root))
        assertThrows(IllegalArgumentException::class.java) { sibling.requireInside(root) }
        assertThrows(IllegalArgumentException::class.java) { root.requireInside(root) }
        assertEquals(root.canonicalFile, root.requireInside(root, allowRoot = true))
    }

    @Test
    fun safeFileNameRejectsPathTraversal() {
        assertEquals(true, "main.pas".isSafeFileName())
        assertEquals(false, "../main.pas".isSafeFileName())
        assertEquals(false, "..".isSafeFileName())
        assertEquals(false, "dir\\main.pas".isSafeFileName())
        assertEquals(false, " main.pas".isSafeFileName())
        assertEquals(false, "main\n.pas".isSafeFileName())
    }

    @Test
    fun directoryReplacementMovesStagingIntoPlaceAndRemovesBackup() {
        val target = temporaryFolder.newFolder("target")
        File(target, "old.txt").writeText("old")
        val staging = temporaryFolder.newFolder("staging")
        File(staging, "new.txt").writeText("new")
        val backup = File(target.parentFile, "backup")

        replaceDirectory(staging, target, backup)

        assertEquals("new", File(target, "new.txt").readText())
        assertEquals(false, File(target, "old.txt").exists())
        assertEquals(false, staging.exists())
        assertEquals(false, backup.exists())
    }
}
