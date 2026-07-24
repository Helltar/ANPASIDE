package com.github.helltar.anpaside.ui.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.github.helltar.anpaside.BuildConfig
import java.io.File

// hands a file out of the app data dir, the only way to get one to another app
fun shareFile(
    context: Context,
    filePath: String,
    onError: (Throwable) -> Unit
): Boolean =
    try {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = mimeType(file)
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        context.startActivity(Intent.createChooser(intent, file.name))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (error: Exception) {
        onError(error)
        false
    }

private fun mimeType(file: File) = when (file.extension.lowercase()) {
    "jar" -> "application/java-archive"
    "zip" -> "application/zip"
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "pas", "txt", "inc" -> "text/plain"
    else -> "application/octet-stream"
}
