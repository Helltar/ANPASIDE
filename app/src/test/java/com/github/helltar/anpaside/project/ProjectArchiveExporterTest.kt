package com.github.helltar.anpaside.project

import net.lingala.zip4j.ZipFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectArchiveExporterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun exportContainsSourcesAndExcludesPrebuildArtifacts() {
        val projectDir = temporaryFolder.newFolder("game")
        val config = File(projectDir, "game.aproj").apply { writeText("Name=game\nMainModule=main") }
        File(projectDir, "src").apply { mkdir() }.resolve("main.pas").writeText("begin end.")
        File(projectDir, "prebuild").apply { mkdir() }.resolve("M.class").writeBytes(byteArrayOf(1))

        val binaries = File(projectDir, "bin").apply { mkdir() }
        binaries.resolve("game.jar").writeBytes(byteArrayOf(1))
        binaries.resolve("game.apk").writeBytes(byteArrayOf(1))

        val destination = temporaryFolder.newFolder("export")
        val project = Project.open(config)

        val archive = ProjectArchiveExporter().export(project, destination)

        val entries = ZipFile(archive).use { zip -> zip.fileHeaders.map { it.fileName } }
        assertTrue(entries.any { it.endsWith("game.aproj") })
        assertTrue(entries.any { it.endsWith("src/main.pas") })
        assertTrue(entries.any { it.endsWith("bin/game.jar") })
        assertFalse(entries.any { it.contains("prebuild") })

        // an exported apk is megabytes of emulator runtime, not part of the project
        assertFalse(entries.any { it.endsWith(".apk") })
    }
}
