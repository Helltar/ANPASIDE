package com.github.helltar.anpaside.project

import com.github.helltar.anpaside.foundation.ProjectLayout
import java.io.File

// files the editor can open, everything else is only shared or deleted from the tree
private val TEXT_EXTENSIONS = setOf("pas", "inc", "txt", "md", "mf", "gitignore", "properties")

// one row of the project file list: a file or a directory at a given nesting depth
data class ProjectTreeEntry(
    val file: File,
    val nestingLevel: Int,
    val isExpanded: Boolean,
    val canRename: Boolean,
    val canDelete: Boolean
) {

    val name: String get() = file.name
    val path: String get() = file.path
    val isDirectory: Boolean get() = file.isDirectory
    val isProjectConfiguration: Boolean
        get() = file.extension.equals(ProjectLayout.PROJECT_EXTENSION.removePrefix("."), true)
    val isTextFile: Boolean
        get() =
            !isDirectory &&
                    !isProjectConfiguration &&
                    file.extension.lowercase() in TEXT_EXTENSIONS
}

// flattens the project directory into visible rows: a directory contributes its children
// only while it is expanded. prebuild/ never shows up, it is the compiler's scratch dir
fun buildProjectTree(
    project: Project,
    expandedDirectories: Set<String>
): List<ProjectTreeEntry> {
    val nodes = mutableListOf<ProjectTreeEntry>()
    collect(
        project = project,
        directory = project.rootDirectory,
        nestingLevel = 0,
        expandedDirectories = expandedDirectories,
        nodes = nodes
    )
    return nodes
}

private fun collect(
    project: Project,
    directory: File,
    nestingLevel: Int,
    expandedDirectories: Set<String>,
    nodes: MutableList<ProjectTreeEntry>
) {
    val children = directory.listFiles()
        .orEmpty()
        .filterNot { it.isDirectory && it.name == ProjectLayout.BUILD_DIRECTORY }
        .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })

    for (child in children) {
        val expanded = child.isDirectory && child.path in expandedDirectories

        val canRename = !project.isFixedEntry(child)
        val canDelete = canRename && child != project.mainModule

        nodes.add(
            ProjectTreeEntry(
                file = child,
                nestingLevel = nestingLevel,
                isExpanded = expanded,
                canRename = canRename,
                canDelete = canDelete
            )
        )

        if (expanded) {
            collect(project, child, nestingLevel + 1, expandedDirectories, nodes)
        }
    }
}
