package com.github.helltar.anpaside.compiler

import com.github.helltar.anpaside.foundation.LogSeverity
import com.github.helltar.anpaside.foundation.ProcessResult
import com.github.helltar.anpaside.foundation.ProcessRunner
import com.github.helltar.anpaside.project.Project
import net.lingala.zip4j.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectBuildPipelineTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun compilesDependenciesCopiesRuntimeAndLibrariesAndPackagesResources() {
        val fixture = fixture()
        fixture.project.librariesDirectory
            .resolve("Lib_sensor.class")
            .writeBytes(byteArrayOf(3))
        fixture.project.resourcesDirectory.resolve("icon.png").writeBytes(byteArrayOf(4))

        val runner =
            ProcessRunner { arguments ->
                assertEquals(fixture.bundledLibraries.path, arguments.valueAfter("-l"))
                assertEquals(
                    fixture.project.librariesDirectory.path,
                    arguments.valueAfter("-p")
                )

                val source = File(arguments.valueAfter("-s"))
                val output = File(arguments.valueAfter("-o"))
                val detecting = "-d" in arguments

                when {
                    detecting && source == fixture.project.mainModule ->
                        ProcessResult.Completed(0, "^0helper")

                    detecting ->
                        ProcessResult.Completed(0, "")

                    source == fixture.project.mainModule -> {
                        output.resolve("M.class").writeBytes(byteArrayOf(1))
                        output.resolve("unused.bsf").writeBytes(byteArrayOf(2))
                        ProcessResult.Completed(0, "^1sensor\n^2F.class")
                    }

                    else -> {
                        output.resolve("helper.class").writeBytes(byteArrayOf(5))
                        ProcessResult.Completed(0, "")
                    }
                }
            }

        val report = fixture.pipeline(runner).build()

        assertTrue(report.succeeded)
        val entries =
            ZipFile(report.outputJar).use { archive ->
                archive.fileHeaders.map { it.fileName }.toSet()
            }
        assertTrue("M.class" in entries)
        assertTrue("helper.class" in entries)
        assertTrue("Lib_sensor.class" in entries)
        assertTrue("F.class" in entries)
        assertTrue("FW.class" in entries)
        assertTrue("icon.png" in entries)
        assertFalse("unused.bsf" in entries)
        assertTrue(report.logEntries.any { it.severity == LogSeverity.INFO })
        assertFalse(MULTI_TOUCH in report.manifest())
    }

    @Test
    fun takesABundledLibraryTheProjectDoesNotHaveAndAsksForMultiTouch() {
        val fixture = fixture()
        fixture.bundledLibraries.resolve("Lib_touch.class").writeBytes(byteArrayOf(7))

        val report = fixture.pipeline(mainModuleUsing("^1touch")).build()

        assertTrue(report.succeeded)
        assertTrue("Lib_touch.class" in report.entries())
        assertTrue(report.manifest().lines().contains(MULTI_TOUCH))
    }

    @Test
    fun prefersTheProjectsOwnCopyOfABundledLibrary() {
        val fixture = fixture()
        fixture.bundledLibraries.resolve("Lib_touch.class").writeBytes(byteArrayOf(7))
        fixture.project.librariesDirectory.resolve("Lib_touch.class").writeBytes(byteArrayOf(8))

        val report = fixture.pipeline(mainModuleUsing("^1touch")).build()

        val library =
            ZipFile(report.outputJar).use { archive ->
                archive.getInputStream(archive.getFileHeader("Lib_touch.class")).readBytes()
            }
        assertEquals(listOf<Byte>(8), library.toList())
    }

    @Test
    fun failsOnALibraryNeitherTheProjectNorTheIdeHas() {
        val fixture = fixture()

        val report = fixture.pipeline(mainModuleUsing("^1missing")).build()

        assertFalse(report.succeeded)
        assertTrue(report.logEntries.any { it.text.endsWith("Lib_missing.class") })
    }

    @Test
    fun numbersRecordClassesAcrossModulesSoTheyDoNotOverwriteEachOther() {
        val fixture = fixture()
        val recordIds = mutableMapOf<String, String>()

        val runner =
            ProcessRunner { arguments ->
                val source = File(arguments.valueAfter("-s"))
                val output = File(arguments.valueAfter("-o"))

                when {
                    "-d" in arguments && source == fixture.project.mainModule ->
                        ProcessResult.Completed(0, "^0helper")

                    "-d" in arguments -> ProcessResult.Completed(0, "")

                    // the unit declares two record types, the main module one
                    source == fixture.project.mainModule -> {
                        recordIds[source.name] = arguments.valueAfter("-r")
                        output.resolve("M.class").writeBytes(byteArrayOf(1))
                        ProcessResult.Completed(0, "^3R_2.class")
                    }

                    else -> {
                        recordIds[source.name] = arguments.valueAfter("-r")
                        output.resolve("helper.class").writeBytes(byteArrayOf(5))
                        ProcessResult.Completed(0, "^3R_0.class\n^3R_1.class")
                    }
                }
            }

        val report = fixture.pipeline(runner).build()

        assertTrue(report.succeeded)
        assertEquals("0", recordIds["helper.pas"])
        assertEquals("2", recordIds["game.pas"])
    }

    @Test
    fun detectPassOfALaterUnitDoesNotNumberItsRecordsFromZeroAgain() {
        val fixture = fixture()
        val detectIds = mutableMapOf<String, String>()

        val runner =
            ProcessRunner { arguments ->
                val source = File(arguments.valueAfter("-s"))
                val output = File(arguments.valueAfter("-o"))

                when {
                    "-d" in arguments -> {
                        detectIds[source.name] = arguments.valueAfter("-r")

                        ProcessResult.Completed(
                            0,
                            if (source == fixture.project.mainModule) "^0first\n^0second" else ""
                        )
                    }

                    source == fixture.project.mainModule -> {
                        output.resolve("M.class").writeBytes(byteArrayOf(1))
                        ProcessResult.Completed(0, "")
                    }

                    // each unit declares one record type
                    else -> {
                        val id = arguments.valueAfter("-r")
                        output.resolve(source.nameWithoutExtension + ".class").writeBytes(byteArrayOf(5))
                        ProcessResult.Completed(0, "^3R_$id.class")
                    }
                }
            }

        fixture.project.sourcesDirectory.resolve("first.pas").writeText("unit first;")
        fixture.project.sourcesDirectory.resolve("second.pas").writeText("unit second;")

        val report = fixture.pipeline(runner).build()

        assertTrue(report.succeeded)
        assertEquals("0", detectIds["first.pas"])
        assertEquals("1", detectIds["second.pas"])
    }

    @Test
    fun startsRecordNumberingOverOnEveryBuild() {
        val fixture = fixture()
        val recordIds = mutableListOf<String>()

        val runner =
            ProcessRunner { arguments ->
                if ("-d" in arguments) {
                    ProcessResult.Completed(0, "")
                } else {
                    recordIds += arguments.valueAfter("-r")
                    File(arguments.valueAfter("-o")).resolve("M.class").writeBytes(byteArrayOf(1))
                    ProcessResult.Completed(0, "^3R_0.class")
                }
            }

        // prebuild/ is wiped for each build, so the second one has to number from zero again
        val pipeline = fixture.pipeline(runner)
        pipeline.build()
        pipeline.build()

        assertEquals(listOf("0", "0"), recordIds)
    }

    @Test
    fun reportsSilentCompilerExitAndDoesNotCreateJar() {
        val fixture = fixture()
        val runner = ProcessRunner { ProcessResult.Completed(137, "") }

        val report = fixture.pipeline(runner).build()

        assertFalse(report.succeeded)
        assertNull(report.outputJar)
        assertTrue(report.logEntries.any { it.text == "compiler exit 137" })
        assertFalse(fixture.project.outputJar.exists())
    }

    @Test
    fun rejectsDependencyPathsOutsideSourceDirectory() {
        val fixture = fixture()
        val runner =
            ProcessRunner { arguments ->
                if ("-d" in arguments) {
                    ProcessResult.Completed(0, "^0../outside")
                } else {
                    ProcessResult.Completed(0, "")
                }
            }

        val report = fixture.pipeline(runner).build()

        assertFalse(report.succeeded)
        assertTrue(report.logEntries.any { it.severity == LogSeverity.ERROR })
    }

    @Test
    fun rejectsRuntimeMarkersThatContainPaths() {
        val fixture = fixture()
        val runner =
            ProcessRunner { arguments ->
                if ("-d" in arguments) {
                    ProcessResult.Completed(0, "")
                } else {
                    File(arguments.valueAfter("-o"))
                        .resolve("M.class")
                        .writeBytes(byteArrayOf(1))
                    ProcessResult.Completed(0, "^2../F.class")
                }
            }

        val report = fixture.pipeline(runner).build()

        assertFalse(report.succeeded)
        assertTrue(report.logEntries.any { it.severity == LogSeverity.ERROR })
    }

    private fun mainModuleUsing(output: String) =
        ProcessRunner { arguments ->
            if ("-d" in arguments) {
                ProcessResult.Completed(0, "")
            } else {
                File(arguments.valueAfter("-o")).resolve("M.class").writeBytes(byteArrayOf(1))
                ProcessResult.Completed(0, output)
            }
        }

    private fun BuildReport.entries(): Set<String> =
        ZipFile(outputJar).use { archive -> archive.fileHeaders.map { it.fileName }.toSet() }

    private fun BuildReport.manifest(): String =
        ZipFile(outputJar).use { archive ->
            archive.getInputStream(archive.getFileHeader("META-INF/MANIFEST.MF"))
                .use { it.readBytes().decodeToString() }
        }

    private fun fixture(): Fixture {
        val projectDirectory = temporaryFolder.newFolder("project-${System.nanoTime()}")
        val project = Project.create(File(projectDirectory, "game.aproj"), "game")
        project.createDirectoryStructure()
        project.save()
        project.mainModule.writeText("program game; begin end.")
        project.sourcesDirectory.resolve("helper.pas").writeText("unit helper; end.")

        val runtime = temporaryFolder.newFolder("rtl-${System.nanoTime()}")
        File(runtime, "FW.class").writeBytes(byteArrayOf(1))
        File(runtime, "F.class").writeBytes(byteArrayOf(2))
        val bundledLibraries = temporaryFolder.newFolder("libs-${System.nanoTime()}")
        val compiler = temporaryFolder.newFile("compiler-${System.nanoTime()}")

        return Fixture(project, compiler, runtime, bundledLibraries)
    }

    private data class Fixture(
        val project: Project,
        val compiler: File,
        val runtime: File,
        val bundledLibraries: File
    ) {
        fun pipeline(runner: ProcessRunner) =
            ProjectBuildPipeline(
                messages = MESSAGES,
                project = project,
                compilerExecutable = compiler,
                runtimeLibraryDirectory = runtime,
                bundledLibrariesDirectory = bundledLibraries,
                processRunner = runner
            )
    }

    private fun List<String>.valueAfter(option: String): String =
        get(indexOf(option) + 1)

    private companion object {
        const val MULTI_TOUCH = "Nokia-UI-Enhancement: EnableMultiPointTouchEvents"

        val MESSAGES =
            BuildMessages(
                manifestTemplate = "%s\n%s\n%s\n%s\n%d\n%d",
                fileNotFound = "file not found",
                compilerExitTemplate = "compiler exit %d",
                mainClassMissing = "main class missing",
                archiveCreationFailed = "archive failed",
                buildSucceeded = "build succeeded"
            )
    }
}
