package com.github.helltar.anpaside.core

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException

// low-level file helpers. they throw on failure and never log or touch string resources;
// the callers that know the user's intent turn a failure into a localized message

// create this directory and any missing parents, returning it
fun File.ensureDirectory(): File {
    if (!isDirectory && !mkdirs()) {
        throw IOException("cannot create directory: $path")
    }

    return this
}

// copy this file into targetDir under its own name, overwriting; throws if the source is missing
fun File.copyInto(targetDir: File): File {
    if (!exists()) {
        throw IOException("file not found: $path")
    }

    return copyTo(targetDir.resolve(name), overwrite = true)
}

// copy the bytes behind a document-picker content uri into destDir, returning the new file.
// the picker hands back a content uri, so the bytes have to be pulled through the resolver
fun importContent(resolver: ContentResolver, uri: Uri, destDir: File): File {
    val dest = destDir.resolve(resolver.displayName(uri))

    resolver.openInputStream(uri).use { input ->
        input ?: throw IOException("cannot open $uri")
        dest.outputStream().use(input::copyTo)
    }

    return dest
}

private fun ContentResolver.displayName(uri: Uri): String {
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getString(0)?.takeIf { it.isNotEmpty() }?.let {
                // a picked name may carry path separators, keep the last segment only
                return it.substringAfterLast('/')
            }
        }
    }

    return uri.lastPathSegment?.substringAfterLast('/') ?: throw IOException("cannot resolve name for $uri")
}
