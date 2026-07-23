package com.github.helltar.anpaside.core

import com.github.helltar.anpaside.App
import java.io.File

// every well-known location and name the app works with. directories are File values,
// paths are only turned into strings at the two real boundaries: the native mp3cc argv
// and SharedPreferences storage
object Paths {

    // the compiler ships as a fake .so so the system unpacks it into nativeLibraryDir
    const val COMPILER_BIN = "libmp3cc.so"
    const val FRAMEWORK_CLASS = "FW.class"

    // the compiler always names the main program class M
    const val MAIN_CLASS = "M.class"

    // asset subdirectories bundled in the apk
    const val ASSET_RTL = "rtl"
    const val ASSET_TEMPLATES = "templates"

    // subdirectories of every project
    const val BIN = "bin"
    const val SRC = "src"
    const val LIBS = "libs"
    const val RES = "res"
    const val PREBUILD = "prebuild"

    const val EXT_PROJ = ".aproj"
    const val EXT_PAS = ".pas"
    const val EXT_JAR = ".jar"
    const val EXT_CLASS = ".class"

    // unit symbol table the compiler writes next to the class, only used while building
    const val EXT_BSF = ".bsf"

    private val context get() = App.context

    val dataDir: File get() = File(context.applicationInfo.dataDir)
    val filesDir: File get() = context.filesDir
    val rtlDir: File get() = filesDir.resolve(ASSET_RTL)
    val templatesDir: File get() = filesDir.resolve(ASSET_TEMPLATES)
    val templateIcon: File get() = templatesDir.resolve("icon.png")
    val compilerBinary: File get() = File(context.applicationInfo.nativeLibraryDir, COMPILER_BIN)

    // mp3cc is a native process that needs real POSIX paths, so the workdir stays on
    // external storage where SAF and content uris would be unusable for it
    val workDir: File get() = context.getExternalFilesDir(null)!!
    val projectsDir: File get() = workDir.resolve("projects")
    val globalLibsDir: File get() = workDir.resolve(LIBS)

    // exported archives are handed to another app right away, the system may drop them after
    val exportDir: File get() = context.cacheDir.resolve("export")
}
