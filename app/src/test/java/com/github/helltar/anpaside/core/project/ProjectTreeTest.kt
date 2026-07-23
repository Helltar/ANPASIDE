package com.github.helltar.anpaside.core.project

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
        val bin = File(root, "bin").apply { mkdir() }
        val src = File(root, "src").apply { mkdir() }
        File(root, "prebuild").mkdir()
        File(src, "main.pas").writeText("begin end.")
        File(root, ".gitignore").writeText("bin/")
        File(root, "icon.png").writeBytes(byteArrayOf(1))
        File(root, "README.MD").writeText("readme")

        val nodes = buildProjectTree(root, setOf(src.path))

        assertEquals(
            listOf("bin", "src", "main.pas", ".gitignore", "icon.png", "README.MD"),
            nodes.map(TreeNode::name)
        )
        assertEquals(listOf(0, 0, 1, 0, 0, 0), nodes.map(TreeNode::depth))
        assertFalse(nodes.first { it.file == bin }.isExpanded)
        assertTrue(nodes.first { it.file == src }.isExpanded)
        assertTrue(nodes.first { it.name == "main.pas" }.isText)
        assertTrue(nodes.first { it.name == ".gitignore" }.isText)
        assertTrue(nodes.first { it.name == "README.MD" }.isText)
        assertFalse(nodes.first { it.name == "icon.png" }.isText)
        assertFalse(nodes.any { it.name == "prebuild" })
    }

    @Test
    fun collapsedDirectoryDoesNotExposeChildren() {
        val root = temporaryFolder.newFolder("project")
        val src = File(root, "src").apply { mkdir() }
        File(src, "main.pas").writeText("begin end.")

        val nodes = buildProjectTree(root, emptySet())

        assertEquals(listOf("src"), nodes.map(TreeNode::name))
        assertFalse(nodes.single().isExpanded)
    }
}
