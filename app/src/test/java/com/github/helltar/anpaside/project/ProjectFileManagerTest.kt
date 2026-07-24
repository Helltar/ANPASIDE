package com.github.helltar.anpaside.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.FileAlreadyExistsException

class ProjectFileManagerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val files = ProjectFileManager()

    @Test
    fun renameStaysInsideProjectAndPreservesContents() {
        val project = project()
        val source = project.mainModule.apply { writeText("source") }

        val renamed = files.rename(project, source.path, "main.pas")

        assertEquals(project.sourcesDirectory.resolve("main.pas"), renamed)
        assertEquals("source", renamed.readText())
        assertEquals("main", Project.open(project.configFile).mainModuleName)
    }

    @Test
    fun renameRejectsTraversalAndExistingTarget() {
        val project = project()
        val source = project.sourcesDirectory.resolve("main.pas").apply { writeText("source") }
        project.sourcesDirectory.resolve("other.pas").writeText("other")

        assertThrows(IllegalArgumentException::class.java) {
            files.rename(project, source.path, "../outside.pas")
        }
        assertThrows(FileAlreadyExistsException::class.java) {
            files.rename(project, source.path, "other.pas")
        }
        assertTrue(source.exists())
    }

    @Test
    fun deleteRejectsFilesOutsideProject() {
        val project = project()
        val outside = temporaryFolder.newFile("outside.pas")

        assertThrows(IllegalArgumentException::class.java) {
            files.delete(project, outside.path)
        }
        assertTrue(outside.exists())
    }

    @Test
    fun requiredProjectEntriesCannotBeDeletedOrRenamed() {
        val project = project()
        project.mainModule.writeText("source")

        assertThrows(IllegalArgumentException::class.java) {
            files.delete(project, project.configFile.path)
        }
        assertThrows(IllegalArgumentException::class.java) {
            files.delete(project, project.mainModule.path)
        }
        assertThrows(IllegalArgumentException::class.java) {
            files.rename(project, project.sourcesDirectory.path, "source")
        }

        assertTrue(project.configFile.exists())
        assertTrue(project.mainModule.exists())
        assertTrue(project.sourcesDirectory.exists())
    }

    private fun project(): Project {
        val directory = temporaryFolder.newFolder("project-${System.nanoTime()}")
        val project = Project.create(File(directory, "game.aproj"), "game")
        project.createDirectoryStructure()
        project.save()
        return project
    }
}
