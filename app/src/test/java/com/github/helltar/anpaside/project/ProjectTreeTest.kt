package com.github.helltar.anpaside.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectTreeTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun buildsSortedVisibleTreeAndSkipsPrebuild() {
        val root = temporaryFolder.newFolder("project")
        val project = Project.create(File(root, "project.aproj"), "main")
        project.save()
        val bin = File(root, "bin").apply { mkdir() }
        val src = File(root, "src").apply { mkdir() }
        File(root, "prebuild").mkdir()
        File(src, "main.pas").writeText("begin end.")
        File(root, ".gitignore").writeText("bin/")
        File(root, "icon.png").writeBytes(byteArrayOf(1))
        File(root, "README.MD").writeText("readme")

        val nodes = buildProjectTree(project, setOf(src.path))

        assertEquals(
            listOf("bin", "src", "main.pas", ".gitignore", "icon.png", "project.aproj", "README.MD"),
            nodes.map(ProjectTreeEntry::name)
        )
        assertEquals(
            listOf(0, 0, 1, 0, 0, 0, 0),
            nodes.map(ProjectTreeEntry::nestingLevel)
        )
        assertFalse(nodes.first { it.file == bin }.isExpanded)
        assertTrue(nodes.first { it.file == src }.isExpanded)
        assertTrue(nodes.first { it.name == "main.pas" }.isTextFile)
        assertTrue(nodes.first { it.name == ".gitignore" }.isTextFile)
        assertTrue(nodes.first { it.name == "README.MD" }.isTextFile)
        assertFalse(nodes.first { it.name == "icon.png" }.isTextFile)
        assertFalse(nodes.first { it.name == "project.aproj" }.isTextFile)
        assertFalse(nodes.first { it.name == "project.aproj" }.canRename)
        assertFalse(nodes.first { it.name == "main.pas" }.canDelete)
        assertFalse(nodes.any { it.name == "prebuild" })
    }

    @Test
    fun collapsedDirectoryDoesNotExposeChildren() {
        val root = temporaryFolder.newFolder("project")
        val project = Project.create(File(root, "project.aproj"), "main")
        project.save()
        val src = File(root, "src").apply { mkdir() }
        File(src, "main.pas").writeText("begin end.")

        val nodes = buildProjectTree(project, emptySet())

        assertEquals(listOf("src", "project.aproj"), nodes.map(ProjectTreeEntry::name))
        assertFalse(nodes.first().isExpanded)
    }
}
