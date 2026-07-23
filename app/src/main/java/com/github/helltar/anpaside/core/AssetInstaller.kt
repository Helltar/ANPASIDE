package com.github.helltar.anpaside.core

import android.content.res.AssetManager
import com.github.helltar.anpaside.core.Paths.ASSET_RTL
import com.github.helltar.anpaside.core.Paths.ASSET_TEMPLATES
import com.github.helltar.anpaside.core.Paths.rtlDir
import java.io.File
import java.io.IOException

class AssetInstaller(private val assetManager: AssetManager) {

    // bump when bundled assets change so installed copies get refreshed
    companion object {
        const val ASSETS_VERSION = 5
    }

    fun install(): Boolean {
        if (!installRtl()) {
            return false
        }

        if (!copyAssets(ASSET_TEMPLATES, Paths.templatesDir)) {
            return false
        }

        removeLegacyAssets()
        return true
    }

    private fun installRtl(): Boolean {
        return try {
            val archivePath = "$ASSET_RTL/${RtlArchive.FILE_NAME}"
            val archive = assetManager.open(archivePath).use { RtlArchive.read(it.readBytes()) }
            val destination = rtlDir.ensureDirectory()

            archive.forEach { (name, bytes) ->
                File(destination, name).writeBytes(bytes)
            }

            destination.listFiles().orEmpty()
                .filter { it.isFile && it.name !in archive }
                .forEach {
                    if (!it.delete()) {
                        throw IOException("Cannot delete ${it.path}")
                    }
                }

            true
        } catch (e: Exception) {
            IdeLog.error(e)
            false
        }
    }

    private fun removeLegacyAssets() {
        val legacyRtlDirectory = Paths.dataDir.resolve("stubs")
        val legacyTemplateIcon = Paths.filesDir.resolve("icon.png")

        if (legacyRtlDirectory.exists() && !legacyRtlDirectory.deleteRecursively()) {
            IdeLog.error(IOException("Cannot delete ${legacyRtlDirectory.path}"))
        }

        if (legacyTemplateIcon.exists() && !legacyTemplateIcon.delete()) {
            IdeLog.error(IOException("Cannot delete ${legacyTemplateIcon.path}"))
        }
    }

    private fun copyAssets(assetPath: String, destination: File): Boolean {
        return try {
            val assets = assetManager.list(assetPath).orEmpty()

            if (assets.isNotEmpty()) {
                destination.ensureDirectory()
                assets.all { copyAssets("$assetPath/$it", File(destination, it)) }
            } else {
                assetManager.open(assetPath).use { input ->
                    destination.outputStream().use(input::copyTo)
                }

                true
            }
        } catch (e: IOException) {
            IdeLog.error(e)
            false
        }
    }
}
