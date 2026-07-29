package com.github.helltar.anpaside.foundation

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

fun File.createDirectories(): File {
    if (isDirectory) {
        return this
    }

    if (exists() || !mkdirs()) {
        throw IOException("Cannot create directory: $path")
    }

    return this
}

fun File.copyToDirectory(directory: File): File {
    if (!isFile) {
        throw IOException("File not found: $path")
    }

    return copyTo(directory.resolve(name), overwrite = true)
}

fun File.deleteOrThrow() {
    if (exists() && !deleteRecursively()) {
        throw IOException("Cannot delete: $path")
    }
}

fun replaceDirectory(staging: File, target: File, backup: File) {
    require(staging.isDirectory) { "Staging directory does not exist: ${staging.path}" }

    if (backup.exists() && !target.exists()) {
        move(backup, target)
    }

    backup.deleteOrThrow()

    if (target.exists()) {
        move(target, backup)
    }

    try {
        move(staging, target)
    } catch (error: Exception) {
        if (!target.exists() && backup.exists()) {
            runCatching { move(backup, target) }
        }

        throw error
    }

    // the replacement is already valid; a stale backup can be retried on the next run
    backup.deleteRecursively()
}

fun File.writeTextAtomically(text: String) {
    writeAtomically { output -> output.write(text.toByteArray(Charsets.UTF_8)) }
}

fun File.writeAtomically(write: (OutputStream) -> Unit) {
    val directory = parentFile ?: File(".")
    directory.createDirectories()

    // deliberately not Files.createTempFile: that one creates the file readable by its owner
    // only, and the rename below carries the mode over to the project file. On the external
    // storage the workspace lives on, such a file is no longer readable over adb or MTP,
    // which is the whole reason the workspace is there
    val temporary = directory.resolve(".$name.${System.nanoTime()}.tmp")

    if (!temporary.createNewFile()) {
        throw IOException("Cannot create a temporary file: ${temporary.path}")
    }

    try {
        temporary.outputStream().use(write)

        try {
            Files.move(
                temporary.toPath(),
                toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temporary.toPath(),
                toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    } catch (error: Exception) {
        temporary.delete()
        throw error
    }
}

fun File.requireDirectChildName(name: String): File {
    require(name.isSafeFileName()) { "Invalid file name: $name" }
    return resolve(name)
}

fun String.isSafeFileName(): Boolean =
    isNotBlank() &&
            trim() == this &&
            this != "." &&
            this != ".." &&
            '/' !in this &&
            '\\' !in this &&
            none(Char::isISOControl)

fun File.requireInside(root: File, allowRoot: Boolean = false): File {
    val canonicalRoot = root.canonicalFile
    val canonicalFile = canonicalFile
    val inside = canonicalFile == canonicalRoot || canonicalFile.toPath().startsWith(canonicalRoot.toPath())

    require(inside && (allowRoot || canonicalFile != canonicalRoot)) {
        "Path is outside the project: $path"
    }

    return canonicalFile
}

private fun move(source: File, target: File) {
    try {
        Files.move(
            source.toPath(),
            target.toPath(),
            StandardCopyOption.ATOMIC_MOVE
        )
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(source.toPath(), target.toPath())
    }
}
