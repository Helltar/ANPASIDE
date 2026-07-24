package com.github.helltar.anpaside.assets

import android.content.res.AssetManager
import com.github.helltar.anpaside.foundation.AppDirectories
import com.github.helltar.anpaside.foundation.createDirectories
import com.github.helltar.anpaside.foundation.deleteOrThrow
import com.github.helltar.anpaside.foundation.replaceDirectory
import com.github.helltar.anpaside.foundation.writeAtomically
import java.io.File

class AssetInstaller(
    private val assetManager: AssetManager,
    private val directories: AppDirectories
) {

    // bump when bundled assets change so installed copies get refreshed
    companion object {
        const val ASSETS_VERSION = 5
    }

    fun install(): Result<Unit> = runCatching {
        installRuntimeLibrary()
        copyAssetTree(AppDirectories.ASSET_TEMPLATES, directories.templatesDirectory)
        removeLegacyAssets()
    }

    private fun installRuntimeLibrary() {
        val archivePath = "${AppDirectories.ASSET_RUNTIME_LIBRARY}/${RtlArchive.FILE_NAME}"
        val archive = assetManager.open(archivePath).use { RtlArchive.read(it.readBytes()) }
        val destination = directories.runtimeLibraryDirectory
        val staging = directories.privateFilesDirectory.resolve(".rtl.installing")
        val backup = directories.privateFilesDirectory.resolve(".rtl.backup")

        staging.deleteOrThrow()
        staging.createDirectories()

        try {
            archive.forEach { (name, bytes) ->
                File(staging, name).writeBytes(bytes)
            }

            replaceDirectory(staging, destination, backup)
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
    }

    private fun removeLegacyAssets() {
        directories.privateDataDirectory.resolve("stubs").deleteOrThrow()
        directories.privateFilesDirectory.resolve("icon.png").deleteOrThrow()
    }

    private fun copyAssetTree(assetPath: String, destination: File) {
        val children = assetManager.list(assetPath).orEmpty()

        if (children.isNotEmpty()) {
            destination.createDirectories()
            children.forEach { copyAssetTree("$assetPath/$it", destination.resolve(it)) }
        } else {
            destination.writeAtomically { output ->
                assetManager.open(assetPath).use { input ->
                    input.copyTo(output)
                }
            }
        }
    }
}
