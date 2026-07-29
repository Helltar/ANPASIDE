package com.github.helltar.anpaside.ui.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.github.helltar.anpaside.BuildConfig
import com.github.helltar.anpaside.project.ApkOrientation
import java.io.File
import ru.playsoftware.j2meloader.MidletRunner

// runs the built jar in the emulator bundled with the ide: the classes are dexed and
// handed to the emulator activity, which lives in its own process
fun launchInBuiltInEmulator(
    context: Context,
    jarPath: String,
    projectName: String,
    screenWidth: Int,
    screenHeight: Int,
    showKeyboard: Boolean,
    onError: (Throwable) -> Unit
): Boolean =
    try {
        MidletRunner.run(
            context,
            File(jarPath),
            midletDirectoryName(projectName, jarPath),
            screenWidth,
            screenHeight,
            showKeyboard
        )

        true
    } catch (error: Exception) {
        onError(error)
        false
    }

// converts the jar and returns the files an exported apk carries in its assets; exported apps
// always use the physical display resolution, independently of the ide emulator settings
fun convertMidletForExport(
    context: Context,
    jarPath: String,
    projectName: String,
    orientation: ApkOrientation,
    showKeyboard: Boolean
): Map<String, File> {
    val name = midletDirectoryName(projectName, jarPath)
    val directory = MidletRunner.install(context, File(jarPath), name)
    val profile =
        MidletRunner.installProfile(
            context,
            name,
            NATIVE_SCREEN_DIMENSION,
            NATIVE_SCREEN_DIMENSION,
            showKeyboard,
            orientation.profileValue
        )

    return buildMap {
        directory.listFiles().orEmpty().filter(File::isFile).forEach { file -> put(file.name, file) }
        put(profile.name, profile)
    }
}

// hands the jar to an external j2me emulator, returns false when there is none installed
fun launchInExternalEmulator(
    context: Context,
    jarPath: String,
    onError: (Throwable) -> Unit
): Boolean =
    try {
        val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, File(jarPath))

        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            setDataAndType(uri, "application/java-archive")
        }

        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (error: Exception) {
        onError(error)
        false
    }

// one directory per project, so a rebuild replaces the converted midlet instead of piling up
private fun midletDirectoryName(projectName: String, jarPath: String): String {
    val name = projectName.ifEmpty { File(jarPath).nameWithoutExtension }
    return name.replace(Regex("[^A-Za-z0-9_.-]"), "_")
}

private const val NATIVE_SCREEN_DIMENSION = -1
private const val PROFILE_ORIENTATION_PORTRAIT = 2
private const val PROFILE_ORIENTATION_LANDSCAPE = 3

private val ApkOrientation.profileValue: Int
    get() =
        when (this) {
            ApkOrientation.PORTRAIT -> PROFILE_ORIENTATION_PORTRAIT
            ApkOrientation.LANDSCAPE -> PROFILE_ORIENTATION_LANDSCAPE
        }
