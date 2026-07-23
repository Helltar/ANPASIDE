package com.github.helltar.anpaside.core.project

import com.github.helltar.anpaside.core.Paths.PREBUILD
import java.io.File

// files the editor can open, everything else is only shared or deleted from the tree
private val TEXT_EXTENSIONS = setOf("pas", "inc", "txt", "md", "mf", "aproj", "gitignore", "properties")

// one row of the project file list: a file or a directory at a given nesting depth
data class TreeNode(val file: File, val depth: Int, val isExpanded: Boolean) {

    val name: String get() = file.name
    val path: String get() = file.path
    val isDirectory: Boolean get() = file.isDirectory
    val isText: Boolean get() = !isDirectory && file.extension.lowercase() in TEXT_EXTENSIONS
}

// flattens the project directory into visible rows: a directory contributes its children
// only while it is expanded. prebuild/ never shows up, it is the compiler's scratch dir
fun buildProjectTree(rootDir: File, expandedDirs: Set<String>): List<TreeNode> {
    val nodes = mutableListOf<TreeNode>()
    collect(rootDir, depth = 0, expandedDirs = expandedDirs, nodes = nodes)
    return nodes
}

private fun collect(dir: File, depth: Int, expandedDirs: Set<String>, nodes: MutableList<TreeNode>) {
    val children = dir.listFiles()
        .orEmpty()
        .filterNot { it.isDirectory && it.name == PREBUILD }
        .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })

    for (child in children) {
        val expanded = child.isDirectory && child.path in expandedDirs

        nodes.add(TreeNode(child, depth, expanded))

        if (expanded) {
            collect(child, depth + 1, expandedDirs, nodes)
        }
    }
}
