package com.github.helltar.anpaside.core.project

import com.github.helltar.anpaside.core.Paths.BIN
import com.github.helltar.anpaside.core.Paths.EXT_JAR
import com.github.helltar.anpaside.core.Paths.EXT_PAS
import com.github.helltar.anpaside.core.Paths.LIBS
import com.github.helltar.anpaside.core.Paths.PREBUILD
import com.github.helltar.anpaside.core.Paths.RES
import com.github.helltar.anpaside.core.Paths.SRC
import com.github.helltar.anpaside.core.ensureDirectory
import java.io.File
import java.util.Properties

// a project is an .aproj file (a java properties file) plus the directory tree around it;
// every path is derived from where that config file lives. open() and create() are the only
// ways to get one, so a Project always refers to a real location - there is no "not open" state
class Project private constructor(val configFile: File, private val properties: Properties) {

    val dir: File = configFile.parentFile!!

    val srcDir: File get() = dir.resolve(SRC)
    val libsDir: File get() = dir.resolve(LIBS)
    val resDir: File get() = dir.resolve(RES)
    val binDir: File get() = dir.resolve(BIN)
    val prebuildDir: File get() = dir.resolve(PREBUILD)

    val mainModuleFile: File get() = srcDir.resolve(mainModuleName + EXT_PAS)
    val jarFile: File get() = binDir.resolve(midletName + EXT_JAR)

    var mainModuleName: String
        get() = properties.getProperty("MainModule", "")
        set(value) {
            properties.setProperty("MainModule", value)
        }

    var mathType: Int
        get() = properties.getProperty("MathType", "0").toInt()
        set(value) {
            properties.setProperty("MathType", value.toString())
        }

    var canvasType: Int
        get() = properties.getProperty("CanvasType", "1").toInt()
        set(value) {
            properties.setProperty("CanvasType", value.toString())
        }

    var midletName: String
        get() = properties.getProperty("Name", "app")
        set(value) {
            properties.setProperty("Name", value)
        }

    var midletVendor: String
        get() = properties.getProperty("Vendor", "vendor")
        set(value) {
            properties.setProperty("Vendor", value)
        }

    var midletVersion: String
        get() = properties.getProperty("Version", "1")
        set(value) {
            properties.setProperty("Version", value)
        }

    fun createDirectories() = listOf(binDir, srcDir, libsDir, resDir, prebuildDir).forEach { it.ensureDirectory() }

    fun save() = configFile.outputStream().use { properties.store(it, null) }

    companion object {
        fun open(configFile: File): Project =
            Project(configFile, Properties().apply { configFile.inputStream().use(::load) })

        // a fresh project carrying only default config; the caller creates the directories
        // and writes the initial source files
        fun create(configFile: File, name: String): Project =
            Project(configFile, Properties()).apply {
                midletName = name
                mainModuleName = name.lowercase()
                midletVendor = "vendor"
                midletVersion = "1.0"
            }
    }
}
