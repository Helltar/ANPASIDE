package com.github.helltar.anpaside.compiler

import com.github.helltar.anpaside.foundation.LogEntry
import com.github.helltar.anpaside.foundation.LogSeverity
import com.github.helltar.anpaside.foundation.ProcessResult
import com.github.helltar.anpaside.foundation.ProcessRunner
import com.github.helltar.anpaside.foundation.ProjectLayout
import com.github.helltar.anpaside.foundation.SystemProcessRunner
import com.github.helltar.anpaside.foundation.copyToDirectory
import com.github.helltar.anpaside.foundation.createDirectories
import com.github.helltar.anpaside.foundation.deleteOrThrow
import com.github.helltar.anpaside.foundation.requireDirectChildName
import com.github.helltar.anpaside.foundation.requireInside
import com.github.helltar.anpaside.project.Project
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.File
import java.io.IOException

data class BuildReport(
    val outputJar: File?,
    val logEntries: List<LogEntry>
) {
    val succeeded: Boolean
        get() = outputJar != null
}

// compiles pascal sources with mp3cc and packages the resulting j2me jar
class ProjectBuildPipeline(
    private val messages: BuildMessages,
    private val project: Project,
    private val compilerExecutable: File,
    private val runtimeLibraryDirectory: File,
    private val bundledLibrariesDirectory: File,
    private val processRunner: ProcessRunner = SystemProcessRunner()
) {

    private val metadata = project.metadata
    private val compilerSettings = project.compilerSettings
    private val outputJar = project.outputJar
    private val logEntries = mutableListOf<LogEntry>()
    private val compilingSources = mutableSetOf<String>()
    private val usedLibraries = mutableSetOf<String>()
    private var nextRecordId = 0

    fun build(): BuildReport {
        logEntries.clear()
        compilingSources.clear()
        usedLibraries.clear()
        nextRecordId = 0

        val outputJar =
            try {
                prepare()

                if (!compileSource(project.mainModule) || !mainClassExists()) {
                    null
                } else {
                    writeManifest()
                    packJar()
                    logSuccess()
                    outputJar
                }
            } catch (error: Exception) {
                error(error.message ?: error.toString())
                null
            }

        return BuildReport(outputJar, logEntries.toList())
    }

    private fun compileSource(sourceFile: File): Boolean {
        val source = sourceFile.requireInside(project.sourcesDirectory)

        // a cycle is left for the compiler to diagnose after the first dependency pass
        if (!compilingSources.add(source.path)) {
            return true
        }

        // first pass detects units so every dependency is compiled before the parent module
        val detect = runCompiler(source, detectUnits = true) ?: return false

        if (!compilerSucceeded(detect, includeOutput = false)) {
            compilingSources.remove(source.path)
            return false
        }

        for (unit in CompilerOutput.units(detect.output)) {
            val unitFile = project.sourcesDirectory
                .resolve(unit + ProjectLayout.PASCAL_EXTENSION)
                .requireInside(project.sourcesDirectory)

            // a unit named but absent is left for the real pass to report as a compiler error
            if (!unitFile.exists()) {
                error(messages.fileNotFound + ": " + unitFile.path)
                continue
            }

            val unitClass = project.buildDirectory.resolve(unit + ProjectLayout.CLASS_EXTENSION)

            if (!unitClass.exists() && !compileSource(unitFile)) {
                compilingSources.remove(source.path)
                return false
            }
        }

        val result = runCompiler(source, detectUnits = false) ?: return false
        compilingSources.remove(source.path)

        if (!compilerSucceeded(result, includeOutput = true)) {
            return false
        }

        // every module compiles into the same prebuild/, so the next one has to keep counting
        // where this one stopped instead of writing its own R_0.class over this one's
        nextRecordId = CompilerOutput.nextRecordId(result.output, nextRecordId)

        plainLines(CompilerOutput.clean(result.output))
        return copyRuntimeClasses(result.output) && copyLibraries(result.output)
    }

    // a prebuild without M.class means the compiler claimed success but wrote nothing,
    // and the jar would install into the emulator and do nothing at all
    private fun mainClassExists(): Boolean {
        if (project.buildDirectory.resolve(ProjectLayout.MAIN_CLASS).exists()) {
            return true
        }

        error(messages.mainClassMissing)
        return false
    }

    private fun runCompiler(file: File, detectUnits: Boolean): ProcessResult.Completed? {
        val args = mutableListOf(
            compilerExecutable.path,
            "-s", file.path,
            "-o", project.buildDirectory.path,
            // the compiler looks in the project's libs/ first and falls back to the bundled ones
            "-l", bundledLibrariesDirectory.path,
            "-p", project.librariesDirectory.path,
            "-m", compilerSettings.mathType.toString(),
            "-c", compilerSettings.canvasType.toString(),
            // the detect pass of a unit writes its record classes too, only without reporting
            // them; numbered from zero it would put its own R_0.class over the one a module
            // compiled earlier owns, so it gets the ids the real pass is about to write again
            "-r", nextRecordId.toString()
        )

        if (detectUnits) {
            args.add("-d")
        }

        return when (val result = processRunner.run(args)) {
            is ProcessResult.Completed -> result
            is ProcessResult.Failed -> {
                error(result.error)
                null
            }
        }
    }

    private fun compilerSucceeded(
        result: ProcessResult.Completed,
        includeOutput: Boolean
    ): Boolean {
        val output = result.output

        if (CompilerOutput.hasErrors(output)) {
            errorLines(CompilerOutput.clean(output))
            return false
        }

        // mp3cc returns its error count, so an unmarked non-zero exit means the process died
        if (result.exitCode != 0) {
            error(messages.compilerExit(result.exitCode))

            if (includeOutput) {
                errorLines(CompilerOutput.clean(output))
            }

            return false
        }

        return true
    }

    private fun copyLibraries(output: String) =
        CompilerOutput.libraries(output).all { library ->
            val fileName = "Lib_$library${ProjectLayout.CLASS_EXTENSION}"

            val source =
                project.librariesDirectory.requireDirectChildName(fileName).takeIf(File::exists)
                    ?: bundledLibrariesDirectory.requireDirectChildName(fileName)
                        .takeIf(File::exists)

            if (source == null) {
                error(
                    messages.fileNotFound +
                            ": " +
                            project.librariesDirectory.resolve(fileName).path
                )
                return@all false
            }

            source.copyToDirectory(project.buildDirectory)
            usedLibraries += library
            true
        }

    private fun copyRuntimeClasses(output: String): Boolean =
        CompilerOutput.runtimeClasses(output).all { className ->
            val source = runtimeLibraryDirectory.requireDirectChildName(className)

            if (!source.isFile) {
                error(messages.fileNotFound + ": " + source.path)
                false
            } else {
                source.copyToDirectory(project.buildDirectory)
                true
            }
        }

    private fun prepare() {
        project.createDirectoryStructure()
        project.buildDirectory.listFiles().orEmpty().forEach(File::deleteOrThrow)

        // zip4j adds to an existing archive, so without this a renamed or deleted module would
        // keep its class inside the jar for every later build
        outputJar.deleteOrThrow()

        runtimeLibraryDirectory
            .resolve(ProjectLayout.FRAMEWORK_CLASS)
            .copyToDirectory(project.buildDirectory)
    }

    // written once every module is compiled, since what it declares depends on the libraries
    private fun writeManifest() {
        val metaInf = project.buildDirectory.resolve("META-INF").createDirectories()
        metaInf.resolve("MANIFEST.MF").writeText(manifest())
    }

    private fun manifest(): String {
        val midp = if (compilerSettings.canvasType < 1) 1 else 2
        val cldc = if (midp == 2) 1 else 0

        val manifest =
            messages.manifestTemplate.format(
                metadata.name, metadata.vendor,
                metadata.name, metadata.version,
                cldc, midp
            )

        // a runtime hands a midlet the second finger only when the manifest asks for it, and
        // lib_sensor would mistake it for the first one, so only a multi-touch library asks
        return if (usedLibraries.any { it in MULTI_TOUCH_LIBRARIES }) {
            manifest.trimEnd() + "\n" + MULTI_TOUCH_ATTRIBUTE
        } else {
            manifest
        }
    }

    private fun packJar() {
        try {
            zipFolder(project.buildDirectory, outputJar)

            // res/ is appended into the same jar; skip it when there is nothing to add
            if (!project.resourcesDirectory.listFiles().isNullOrEmpty()) {
                zipFolder(project.resourcesDirectory, outputJar)
            }
        } catch (error: Exception) {
            throw IOException(
                messages.archiveCreationFailed +
                        ": " +
                        outputJar.path +
                        " (" +
                        error.message +
                        ")",
                error
            )
        }
    }

    private fun zipFolder(directory: File, archive: File) {
        val parameters = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            compressionLevel = CompressionLevel.ULTRA
            isIncludeRootFolder = false
            // the compiler resolves units through .bsf symbol files, the midlet never reads them
            excludeFileFilter =
                ExcludeFileFilter { it.name.endsWith(ProjectLayout.SYMBOL_EXTENSION) }

        }

        ZipFile(archive).use { it.addFolder(directory, parameters) }
    }

    private fun logSuccess() {
        val jar = outputJar

        info(
            messages.buildSucceeded + "\n" +
                    "${ProjectLayout.BINARY_DIRECTORY}/${jar.name}\n" +
                    "${jar.length() / 1024} KB"
        )
    }

    private fun plain(message: String) = add(message, LogSeverity.PLAIN)

    private fun info(message: String) = add(message, LogSeverity.INFO)

    private fun error(message: String) = add(message, LogSeverity.ERROR)

    private fun error(error: Throwable) = error(error.message ?: error.toString())

    private fun add(message: String, severity: LogSeverity) {
        val text = message.trim()

        if (text.isNotEmpty()) {
            logEntries += LogEntry(text, severity)
        }
    }

    // one message per line: an error line carries "unit.pas(12)", which the log panel turns
    // into a tap that opens the file at that line
    private fun plainLines(text: String) = text.lines().forEach(::plain)

    private fun errorLines(text: String) = text.lines().forEach(::error)

    private companion object {
        val MULTI_TOUCH_LIBRARIES = setOf("touch")
        const val MULTI_TOUCH_ATTRIBUTE = "Nokia-UI-Enhancement: EnableMultiPointTouchEvents"
    }
}
