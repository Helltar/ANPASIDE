package com.github.helltar.anpaside.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.github.helltar.anpaside.BuildConfig
import com.github.helltar.anpaside.core.IdeLog
import java.io.File

// hands a file out of the app data dir, the only way to get one to another app
fun shareFile(context: Context, filename: String): Boolean =
    try {
        val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, File(filename))

        val intent = Intent(Intent.ACTION_SEND).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = mimeType(filename)
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        context.startActivity(Intent.createChooser(intent, File(filename).name))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: Exception) {
        IdeLog.error(e)
        false
    }

private fun mimeType(filename: String) = when (File(filename).extension.lowercase()) {
    "jar" -> "application/java-archive"
    "zip" -> "application/zip"
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "pas", "txt", "inc" -> "text/plain"
    else -> "application/octet-stream"
}
