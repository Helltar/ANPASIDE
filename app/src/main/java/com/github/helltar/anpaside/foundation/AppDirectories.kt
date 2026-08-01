package com.github.helltar.anpaside.foundation

import android.content.Context
import java.io.File

/**
 * Resolves every app-owned directory from an application context.
 *
 * Keeping these paths in an injected value makes filesystem code independent from a global
 * `Application` instance and gives tests an explicit seam for temporary directories.
 */
data class AppDirectories(
    val privateDataDirectory: File,
    val privateFilesDirectory: File,
    val runtimeLibraryDirectory: File,
    val templatesDirectory: File,
    val templateIcon: File,
    val compilerExecutable: File,
    val workspaceDirectory: File,
    val projectsDirectory: File,
    val exportDirectory: File
) {

    companion object {
        const val COMPILER_EXECUTABLE = "libmp3cc.so"
        const val ASSET_RUNTIME_LIBRARY = "rtl"
        const val ASSET_TEMPLATES = "templates"

        private const val TEMPLATE_ICON = "icon.png"
        private const val PROJECTS_DIRECTORY = "projects"
        private const val EXPORT_DIRECTORY = "export"

        fun from(context: Context): AppDirectories {
            val privateFilesDirectory = context.filesDir

            // mp3cc needs real posix paths, so the workspace cannot use SAF or MediaStore uris
            val workspaceDirectory = requireNotNull(context.getExternalFilesDir(null)) {
                "External files directory is unavailable"
            }

            return AppDirectories(
                privateDataDirectory = File(context.applicationInfo.dataDir),
                privateFilesDirectory = privateFilesDirectory,
                runtimeLibraryDirectory =
                    privateFilesDirectory.resolve(ASSET_RUNTIME_LIBRARY),
                templatesDirectory = privateFilesDirectory.resolve(ASSET_TEMPLATES),
                templateIcon =
                    privateFilesDirectory.resolve(ASSET_TEMPLATES).resolve(TEMPLATE_ICON),
                compilerExecutable =
                    File(context.applicationInfo.nativeLibraryDir, COMPILER_EXECUTABLE),
                workspaceDirectory = workspaceDirectory,
                projectsDirectory = workspaceDirectory.resolve(PROJECTS_DIRECTORY),
                // exports are shared immediately and may be cleaned by the system afterwards
                exportDirectory = context.cacheDir.resolve(EXPORT_DIRECTORY)
            )
        }
    }
}
