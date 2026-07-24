package com.github.helltar.anpaside.project

import com.github.helltar.anpaside.foundation.createDirectories
import com.github.helltar.anpaside.foundation.deleteOrThrow
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.File

// packs the whole project into a zip to hand out through a share intent.
// destinationDirectory is a cache location, so the system may clean it afterwards.
// throws on failure; the caller reports it
class ProjectArchiveExporter {

    fun export(project: Project, destinationDirectory: File): File {
        destinationDirectory.createDirectories()

        val archive = destinationDirectory.resolve(project.rootDirectory.name + ".zip")

        // zip4j appends by default, so every export starts from an empty archive
        archive.deleteOrThrow()

        val parameters =
            ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = CompressionLevel.ULTRA
                // build leftovers are reproducible from the sources
                excludeFileFilter = ExcludeFileFilter { it.startsWith(project.buildDirectory) }
            }

        ZipFile(archive).use { it.addFolder(project.rootDirectory, parameters) }

        return archive
    }
}
