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

    val temporary =
        Files.createTempFile(directory.toPath(), ".$name.", ".tmp").toFile()

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
