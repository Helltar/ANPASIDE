package com.github.helltar.anpaside.project

import com.github.helltar.anpaside.foundation.AppDirectories
import com.github.helltar.anpaside.foundation.ProjectLayout
import com.github.helltar.anpaside.foundation.copyToDirectory
import com.github.helltar.anpaside.foundation.createDirectories
import com.github.helltar.anpaside.foundation.deleteOrThrow
import com.github.helltar.anpaside.foundation.isSafeFileName
import com.github.helltar.anpaside.foundation.requireDirectChildName
import com.github.helltar.anpaside.foundation.requireInside
import com.github.helltar.anpaside.foundation.replaceDirectory
import java.io.File

class ProjectRepository(
    private val directories: AppDirectories,
    private val archiveExporter: ProjectArchiveExporter = ProjectArchiveExporter()
) {

    val projectsDirectory: File
        get() = directories.projectsDirectory

    fun listNames(): List<String> =
        projectsDirectory.listFiles()
            .orEmpty()
            .asSequence()
            .filter(File::isDirectory)
            .filter { directory ->
                directory.resolve(directory.name + ProjectLayout.PROJECT_EXTENSION).isFile
            }
            .map(File::getName)
            .sortedWith(compareBy<String> { it.lowercase() }.thenBy { it })
            .toList()

    fun projectDirectory(name: String): File {
        require(name.isSafeFileName()) { "Invalid project name: $name" }
        return projectsDirectory.requireDirectChildName(name)
    }

    fun configFile(name: String): File =
        projectDirectory(name).resolve(name + ProjectLayout.PROJECT_EXTENSION)

    fun exists(name: String): Boolean =
        name.isSafeFileName() && projectDirectory(name).exists()

    fun open(name: String): Project = open(configFile(name))

    fun open(configFile: File): Project =
        Project.open(configFile.requireInside(projectsDirectory))

    fun create(name: String, templates: ProjectTemplates, overwrite: Boolean): Project {
        require(ProjectNames.isValidProjectName(name)) { "Invalid project name: $name" }

        projectsDirectory.createDirectories()

        val targetDirectory =
            projectDirectory(name).let { directory ->
                if (directory.exists()) {
                    directory.requireInside(projectsDirectory)
                } else {
                    directory
                }
            }

        if (targetDirectory.exists()) {
            check(overwrite) { "Project already exists: $name" }
        }

        val stagingDirectory = projectsDirectory.resolve(".$name.creating")
        val backupDirectory = projectsDirectory.resolve(".$name.backup")

        stagingDirectory.deleteOrThrow()

        try {
            val project =
                Project.create(
                    stagingDirectory.resolve(name + ProjectLayout.PROJECT_EXTENSION),
                    name
                )

            project.createDirectoryStructure()
            project.save()
            project.mainModule.writeText(templates.mainModule.format(project.mainModuleName))
            stagingDirectory.resolve(".gitignore").writeText(templates.gitIgnore)
            directories.templateIcon.copyToDirectory(project.resourcesDirectory)
            directories.globalLibrariesDirectory.createDirectories()

            replaceDirectory(stagingDirectory, targetDirectory, backupDirectory)
            return open(name)
        } catch (error: Exception) {
            stagingDirectory.deleteRecursively()
            throw error
        }
    }

    fun delete(name: String) {
        projectDirectory(name).requireInside(projectsDirectory).deleteOrThrow()
    }

    fun export(name: String): File =
        archiveExporter.export(open(name), directories.exportDirectory)
}
