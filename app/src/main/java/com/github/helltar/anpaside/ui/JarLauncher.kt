package com.github.helltar.anpaside.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.github.helltar.anpaside.BuildConfig
import com.github.helltar.anpaside.core.IdeLog
import java.io.File
import ru.playsoftware.j2meloader.MidletRunner

// runs the built jar in the emulator bundled with the ide: the classes are dexed and
// handed to the emulator activity, which lives in its own process
fun runJar(
    context: Context,
    filename: String,
    projectName: String,
    screenWidth: Int,
    screenHeight: Int,
    showKeyboard: Boolean
): Boolean =
    try {
        MidletRunner.run(
            context,
            File(filename),
            midletDirName(projectName, filename),
            screenWidth,
            screenHeight,
            showKeyboard
        )

        true
    } catch (e: Exception) {
        IdeLog.error(e)
        false
    }

// hands the jar to an external j2me emulator, returns false when there is none installed
fun runJarExternally(context: Context, filename: String): Boolean =
    try {
        val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, File(filename))

        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            setDataAndType(uri, "application/java-archive")
        }

        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: Exception) {
        IdeLog.error(e)
        false
    }

// one directory per project, so a rebuild replaces the converted midlet instead of piling up
private fun midletDirName(projectName: String, jarFilename: String): String {
    val name = projectName.ifEmpty { File(jarFilename).nameWithoutExtension }
    return name.replace(Regex("[^A-Za-z0-9_.-]"), "_")
}
