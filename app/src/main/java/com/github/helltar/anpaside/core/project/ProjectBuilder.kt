package com.github.helltar.anpaside.core.project

import android.content.Context
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.core.IdeLog
import com.github.helltar.anpaside.core.Paths.BIN
import com.github.helltar.anpaside.core.Paths.EXT_BSF
import com.github.helltar.anpaside.core.Paths.EXT_CLASS
import com.github.helltar.anpaside.core.Paths.EXT_PAS
import com.github.helltar.anpaside.core.Paths.FRAMEWORK_CLASS
import com.github.helltar.anpaside.core.Paths.MAIN_CLASS
import com.github.helltar.anpaside.core.ShellResult
import com.github.helltar.anpaside.core.copyInto
import com.github.helltar.anpaside.core.ensureDirectory
import com.github.helltar.anpaside.core.runCommand
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.File
import java.io.IOException

// compiles pascal sources with mp3cc and packs the result into a j2me jar.
// the compiler marks its output lines: ^0 - used unit, ^1 - used lib, ^2 - needed stub.
// operations that can fail throw with an already-localized message, which build() logs
class ProjectBuilder(
    private val context: Context,
    private val project: Project,
    private val compilerBinary: File,
    private val rtlDir: File,
    private val globalLibsDir: File
) {

    fun build(): Boolean {
        return try {
            prepare()

            if (!compile(project.mainModuleFile) || !mainClassExists()) {
                return false
            }

            packJar()
            logSuccess()
            true
        } catch (e: Exception) {
            IdeLog.error(e.message ?: e.toString())
            false
        }
    }

    private fun compile(file: File): Boolean {
        // first pass detects units so every dependency is compiled before the parent module
        val detect = runCompiler(file, detectUnits = true)

        if (!detect.started) {
            return false
        }

        for (unit in CompilerOutput.units(detect.output)) {
            val unitFile = project.srcDir.resolve(unit + EXT_PAS)

            // a unit named but absent is left for the real pass to report as a compiler error
            if (!unitFile.exists()) {
                logError(R.string.err_file_not_found, unitFile.path)
                continue
            }

            if (!project.prebuildDir.resolve(unit + EXT_CLASS).exists() && !compile(unitFile)) {
                return false
            }
        }

        val result = runCompiler(file, detectUnits = false)

        if (!result.started) {
            return false
        }

        val output = result.output

        if (CompilerOutput.hasErrors(output)) {
            logLines(CompilerOutput.clean(output), IdeLog::error)
            return false
        }

        // the compiler returns its error count, so a non-zero exit with no marked error means
        // the process itself died - killed by seccomp, bad arguments, out of memory
        if (result.exitCode != 0) {
            IdeLog.error(context.getString(R.string.err_compiler_exit, result.exitCode))
            logLines(CompilerOutput.clean(output), IdeLog::error)
            return false
        }

        logLines(CompilerOutput.clean(output), IdeLog::add)

        copyStubs(output)
        return copyLibs(output)
    }

    // a prebuild without M.class means the compiler claimed success but wrote nothing,
    // and the jar would install into the emulator and do nothing at all
    private fun mainClassExists(): Boolean {
        if (project.prebuildDir.resolve(MAIN_CLASS).exists()) {
            return true
        }

        IdeLog.error(context.getString(R.string.err_main_class_missing))
        return false
    }

    private fun runCompiler(file: File, detectUnits: Boolean): ShellResult {
        val args = mutableListOf(
            compilerBinary.path,
            "-s", file.path,
            "-o", project.prebuildDir.path,
            "-l", globalLibsDir.path,
            "-p", project.libsDir.path,
            "-m", project.mathType.toString(),
            "-c", project.canvasType.toString()
        )

        if (detectUnits) {
            args.add("-d")
        }

        return runCommand(args)
    }

    private fun copyLibs(output: String) = CompilerOutput.libs(output).all { lib ->
        val fileName = "Lib_$lib$EXT_CLASS"

        // project libs first, then global - the same order the compiler resolves them
        val source = project.libsDir.resolve(fileName).takeIf { it.exists() }
            ?: globalLibsDir.resolve(fileName).takeIf { it.exists() }

        if (source == null) {
            logError(R.string.err_file_not_found, globalLibsDir.resolve(fileName).path)
            return@all false
        }

        source.copyInto(project.prebuildDir)
        true
    }

    private fun copyStubs(output: String) =
        CompilerOutput.stubs(output).forEach { rtlDir.resolve(it).copyInto(project.prebuildDir) }

    private fun prepare() {
        project.createDirectories()

        project.prebuildDir.listFiles()?.forEach { it.deleteRecursively() }

        // zip4j adds to an existing archive, so without this a renamed or deleted module would
        // keep its class inside the jar for every later build
        project.jarFile.delete()

        val metaInf = project.prebuildDir.resolve("META-INF").ensureDirectory()
        metaInf.resolve("MANIFEST.MF").writeText(manifest())

        rtlDir.resolve(FRAMEWORK_CLASS).copyInto(project.prebuildDir)
    }

    private fun manifest(): String {
        val midp = if (project.canvasType < 1) 1 else 2
        val cldc = if (midp == 2) 1 else 0

        return context.getString(
            R.string.tpl_manifest,
            project.midletName, project.midletVendor,
            project.midletName, project.midletVersion,
            cldc, midp
        )
    }

    private fun packJar() {
        try {
            zipFolder(project.prebuildDir, project.jarFile, addToArchive = false)

            // res/ is appended into the same jar; skip it when there is nothing to add
            if (!project.resDir.listFiles().isNullOrEmpty()) {
                zipFolder(project.resDir, project.jarFile, addToArchive = true)
            }
        } catch (e: Exception) {
            throw IOException(
                context.getString(R.string.err_failed_create_archive) + ": " + project.jarFile.path + " (" + e.message + ")",
                e
            )
        }
    }

    private fun zipFolder(dir: File, jar: File, addToArchive: Boolean) {
        val params = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            compressionLevel = CompressionLevel.ULTRA
            isIncludeRootFolder = false
            // the compiler resolves units through .bsf symbol files, the midlet never reads them
            excludeFileFilter = ExcludeFileFilter { it.name.endsWith(EXT_BSF) }

            if (addToArchive) {
                rootFolderNameInZip = "/"
            }
        }

        ZipFile(jar).use { it.addFolder(dir, params) }
    }

    private fun logSuccess() {
        val jar = project.jarFile

        IdeLog.info(
            context.getString(R.string.msg_build_successfully) + "\n" +
                    "$BIN/${jar.name}\n" +
                    "${jar.length() / 1024} KB"
        )
    }

    private fun logError(resId: Int, arg: String) = IdeLog.error(context.getString(resId) + ": " + arg)

    // one message per line: an error line carries "unit.pas(12)", which the log panel turns
    // into a tap that opens the file at that line
    private fun logLines(text: String, log: (String) -> Unit) = text.lines().forEach(log)
}
