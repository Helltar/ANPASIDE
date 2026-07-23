package com.github.helltar.anpaside.core.project

import com.github.helltar.anpaside.core.ensureDirectory
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.File

// packs the whole project into a zip to hand out through a share intent. the archive goes to
// destDir (the cache dir), so it is the system's business to clean it up afterwards.
// throws on failure - the caller reports it
fun exportProject(project: Project, destDir: File): File {
    destDir.ensureDirectory()

    val archive = destDir.resolve(project.dir.name + ".zip")

    // a fresh archive every time, zip4j would otherwise add to the previous export
    archive.delete()

    val params = ZipParameters().apply {
        compressionMethod = CompressionMethod.DEFLATE
        compressionLevel = CompressionLevel.ULTRA
        // build leftovers are reproducible from the sources, they only bloat the archive
        excludeFileFilter = ExcludeFileFilter { it.startsWith(project.prebuildDir) }
    }

    ZipFile(archive).use { it.addFolder(project.dir, params) }

    return archive
}
