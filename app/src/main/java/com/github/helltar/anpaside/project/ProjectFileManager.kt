package com.github.helltar.anpaside.project

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.github.helltar.anpaside.foundation.ProjectLayout
import com.github.helltar.anpaside.foundation.deleteOrThrow
import com.github.helltar.anpaside.foundation.isSafeFileName
import com.github.helltar.anpaside.foundation.requireInside
import com.github.helltar.anpaside.foundation.writeAtomically
import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files

class ProjectFileManager {

    fun createModule(
        project: Project,
        name: String,
        template: String,
        overwrite: Boolean
    ): File {
        require(ProjectNames.isValidModuleName(name)) { "Invalid module name: $name" }

        val module = project.sourcesDirectory
            .resolve(name + ProjectLayout.PASCAL_EXTENSION)
            .requireInside(project.rootDirectory)

        if (module.exists()) {
            check(overwrite) { "Module already exists: $name" }
            module.deleteOrThrow()
        }

        module.writeAtomically { output ->
            output.write(template.format(name).toByteArray(Charsets.UTF_8))
        }
        return module
    }

    fun delete(project: Project, path: String) {
        val file = File(path).requireInside(project.rootDirectory)
        require(!project.isFixedEntry(file) && file != project.mainModule) {
            "Cannot delete a required project entry: ${file.path}"
        }
        file.deleteOrThrow()
    }

    fun rename(project: Project, path: String, newName: String): File {
        require(ProjectNames.isValidEntryName(newName)) { "Invalid file name: $newName" }

        val source = File(path).requireInside(project.rootDirectory)
        require(!project.isFixedEntry(source)) {
            "Cannot rename a fixed project entry: ${source.path}"
        }

        val parent = requireNotNull(source.parentFile) { "File has no parent: ${source.path}" }
        val target = parent.resolve(newName).requireInside(project.rootDirectory)
        val isMainModule = source == project.mainModule

        if (isMainModule) {
            require(target.extension.equals(ProjectLayout.PASCAL_EXTENSION.removePrefix("."), true)) {
                "Main module must remain a Pascal source"
            }
            require(ProjectNames.isValidModuleName(target.nameWithoutExtension)) {
                "Invalid main module name: ${target.nameWithoutExtension}"
            }
        }

        if (target.exists()) {
            throw FileAlreadyExistsException(target.path)
        }

        Files.move(source.toPath(), target.toPath())

        if (isMainModule) {
            try {
                project.updateMainModule(target.nameWithoutExtension)
                project.save()
            } catch (error: Exception) {
                project.updateMainModule(source.nameWithoutExtension)
                runCatching { Files.move(target.toPath(), source.toPath()) }
                throw error
            }
        }

        return target
    }

    fun import(
        resolver: ContentResolver,
        uri: Uri,
        project: Project,
        destinationPath: String
    ): File {
        val destinationDirectory =
            File(destinationPath).requireInside(project.rootDirectory, allowRoot = true)

        require(destinationDirectory.isDirectory) {
            "Import destination is not a directory: $destinationPath"
        }

        val displayName = resolver.displayName(uri)
        require(displayName.isSafeFileName()) { "Invalid document name: $displayName" }

        val destination = destinationDirectory.resolve(displayName).requireInside(project.rootDirectory)

        destination.writeAtomically { output ->
            resolver.openInputStream(uri).use { input ->
                input ?: throw IOException("Cannot open $uri")
                input.copyTo(output)
            }
        }

        return destination
    }

    private fun ContentResolver.displayName(uri: Uri): String {
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)?.takeIf(String::isNotEmpty)?.let { return it }
            }
        }

        return uri.lastPathSegment?.substringAfterLast('/')
            ?: throw IOException("Cannot resolve name for $uri")
    }
}
