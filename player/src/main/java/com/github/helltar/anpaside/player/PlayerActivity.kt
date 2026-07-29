package com.github.helltar.anpaside.player

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import java.io.File
import java.io.IOException
import ru.playsoftware.j2meloader.config.Config

/**
 * The launcher of an exported midlet.
 *
 * The apk carries the already converted midlet in its assets, because the emulator loads the
 * classes from a real dex file on disk and cannot read one out of an archive. The files are
 * unpacked into the directories the runtime expects, once per installed version, and the
 * runtime activity takes over from there.
 */
class PlayerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val midletDirectory = installBundledMidlet()
            Config.startApp(this, title.toString(), midletDirectory.path, false)
        } catch (error: Exception) {
            Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
        }

        finish()
    }

    private fun installBundledMidlet(): File {
        val midletDirectory = File(Config.getAppDir(), MIDLET_DIRECTORY)
        val configDirectory = File(Config.getConfigsDir(), MIDLET_DIRECTORY)
        val marker = File(midletDirectory, INSTALL_MARKER)
        val version = packageManager.getPackageInfo(packageName, 0).lastUpdateTime.toString()

        if (marker.isFile && marker.readText() == version) {
            return midletDirectory
        }

        unpackAssets(midletDirectory, configDirectory)
        marker.writeText(version)

        return midletDirectory
    }

    // the emulator keeps the converted midlet and its profile in two separate directories,
    // and a midlet without a profile sends it to a settings screen this app does not have
    private fun unpackAssets(midletDirectory: File, configDirectory: File) {
        createDirectory(midletDirectory)
        createDirectory(configDirectory)

        for (name in assets.list(ASSET_DIRECTORY).orEmpty()) {
            val target =
                if (name == CONFIG_FILE) {
                    File(configDirectory, name)
                } else {
                    File(midletDirectory, name)
                }

            assets.open("$ASSET_DIRECTORY/$name").use { input ->
                target.outputStream().use(input::copyTo)
            }
        }
    }

    private fun createDirectory(directory: File) {
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Can't create directory: $directory")
        }
    }

    private companion object {
        const val ASSET_DIRECTORY = "midlet"
        const val MIDLET_DIRECTORY = "midlet"
        const val CONFIG_FILE = "config.json"
        const val INSTALL_MARKER = ".installed"
    }
}
