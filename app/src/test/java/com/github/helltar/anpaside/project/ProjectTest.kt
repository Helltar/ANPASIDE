package com.github.helltar.anpaside.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Properties

class ProjectTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun opensConfigDerivesPathsAndPersistsChanges() {
        val projectDir = temporaryFolder.newFolder("game")
        val config = File(projectDir, "game.aproj")
        Properties().apply {
            setProperty("Name", "My Game")
            setProperty("Vendor", "Helltar")
            setProperty("Version", "2.5")
            setProperty("MainModule", "main")
            setProperty("MathType", "2")
            setProperty("CanvasType", "1")
            setProperty("ApkKeyboard", "true")
            setProperty("ApkOrientation", "landscape")
        }.run { config.outputStream().use { store(it, null) } }

        val project = Project.open(config)

        assertEquals(projectDir, project.rootDirectory)
        assertEquals(config, project.configFile)
        assertEquals(File(projectDir, "src/main.pas"), project.mainModule)
        assertEquals(File(projectDir, "bin/My Game.jar"), project.outputJar)
        assertEquals("Helltar", project.metadata.vendor)
        assertEquals(2, project.compilerSettings.mathType)
        assertTrue(project.apkKeyboardEnabled)
        assertEquals(ApkOrientation.LANDSCAPE, project.apkOrientation)

        project.updateMetadata(project.metadata.copy(version = "3.0"))
        project.updateApkKeyboard(false)
        project.updateApkOrientation(ApkOrientation.PORTRAIT)
        project.save()

        val saved = Properties().apply { config.inputStream().use(::load) }
        assertEquals("3.0", saved.getProperty("Version"))
        assertEquals("false", saved.getProperty("ApkKeyboard"))
        assertEquals("portrait", saved.getProperty("ApkOrientation"))
    }

    @Test
    fun corruptCompilerNumbersFallBackToFormatDefaults() {
        val projectDir = temporaryFolder.newFolder("corrupt")
        val config =
            File(projectDir, "corrupt.aproj").apply {
                writeText(
                    "Name=game\nMainModule=main\nMathType=nope\nCanvasType=\n" +
                            "ApkOrientation=sideways"
                )
            }

        val project = Project.open(config)

        assertEquals(0, project.compilerSettings.mathType)
        assertEquals(1, project.compilerSettings.canvasType)
        assertFalse(project.apkKeyboardEnabled)
        assertEquals(ApkOrientation.PORTRAIT, project.apkOrientation)
    }

    @Test
    fun apkSettingsHaveSafeDefaults() {
        val projectDir = temporaryFolder.newFolder("keyboard-default")
        val oldConfig =
            File(projectDir, "old.aproj").apply {
                writeText("Name=game\nMainModule=main")
            }
        val oldProject = Project.open(oldConfig)
        val newProject = Project.create(File(projectDir, "new.aproj"), "game")

        assertFalse(oldProject.apkKeyboardEnabled)
        assertFalse(newProject.apkKeyboardEnabled)
        assertEquals(ApkOrientation.PORTRAIT, oldProject.apkOrientation)
        assertEquals(ApkOrientation.PORTRAIT, newProject.apkOrientation)
    }

    @Test
    fun metadataCannotEscapeBinaryDirectoryThroughJarName() {
        val projectDir = temporaryFolder.newFolder("unsafe")
        val config = File(projectDir, "unsafe.aproj")
        val project = Project.create(config, "game")

        assertThrows(IllegalArgumentException::class.java) {
            project.updateMetadata(project.metadata.copy(name = "../outside"))
        }
    }

    @Test
    fun metadataRoundTripsNonAsciiText() {
        val projectDir = temporaryFolder.newFolder("unicode")
        val config = File(projectDir, "unicode.aproj")
        val project = Project.create(config, "game")
        val metadata = MidletMetadata("Гра", "Розробник", "1.0")

        project.updateMetadata(metadata)
        project.save()

        assertEquals(metadata, Project.open(config).metadata)
    }
}
