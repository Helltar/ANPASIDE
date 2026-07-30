package com.github.helltar.anpaside.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
            setProperty("Package", "midlet.mygame")
            setProperty("AppLabel", "My Game HD")
            setProperty("ApkVersionCode", "42")
            setProperty("IconBackground", "#102030")
        }.run { config.outputStream().use { store(it, null) } }

        val project = Project.open(config)

        assertEquals(projectDir, project.rootDirectory)
        assertEquals(config, project.configFile)
        assertEquals(File(projectDir, "src/main.pas"), project.mainModule)
        assertEquals(File(projectDir, "bin/My Game.jar"), project.outputJar)
        assertEquals("Helltar", project.metadata.vendor)
        assertEquals(2, project.compilerSettings.mathType)
        assertTrue(project.apkSettings.keyboardEnabled)
        assertEquals(ApkOrientation.LANDSCAPE, project.apkSettings.orientation)
        assertEquals("My Game HD", project.apkSettings.label)
        assertEquals(42, project.apkSettings.versionCode)
        assertEquals(0xFF102030.toInt(), project.apkSettings.iconBackgroundColor)

        project.updateMetadata(project.metadata.copy(version = "3.0"))
        project.updateApkSettings(
            project.apkSettings.copy(
                label = "",
                versionCode = null,
                iconBackground = "#ABCDEF",
                orientation = ApkOrientation.PORTRAIT,
                keyboardEnabled = false
            )
        )
        project.save()

        val saved = Properties().apply { config.inputStream().use(::load) }
        assertEquals("3.0", saved.getProperty("Version"))
        assertEquals("false", saved.getProperty("ApkKeyboard"))
        assertEquals("portrait", saved.getProperty("ApkOrientation"))
        assertEquals("", saved.getProperty("AppLabel"))
        assertEquals("#ABCDEF", saved.getProperty("IconBackground"))
        // clearing the override has to remove the key, not park a number in it
        assertNull(saved.getProperty("ApkVersionCode"))
    }

    @Test
    fun apkOverridesCanBeAddedToAProjectThatNeverHadThem() {
        val projectDir = temporaryFolder.newFolder("overrides")
        val config =
            File(projectDir, "overrides.aproj").apply {
                writeText("Name=CatchRect\nMainModule=main\nVersion=1.0")
            }
        val project = Project.open(config)

        project.updateApkSettings(
            project.apkSettings.copy(
                packageName = "midlet.catchrect",
                label = "Catch Rect HD",
                versionCode = 77,
                iconBackground = "#2E5E4E"
            )
        )
        project.save()

        val reopened = Project.open(config).apkSettings

        assertEquals("Catch Rect HD", reopened.label)
        assertEquals(77, reopened.versionCode)
        // the hash of a colour is escaped by Properties.store and has to survive the round trip
        assertEquals("#2E5E4E", reopened.iconBackground)
    }

    @Test
    fun labelAndVersionCodeFallBackToTheMidletMetadata() {
        val projectDir = temporaryFolder.newFolder("fallback")
        val config =
            File(projectDir, "fallback.aproj").apply {
                writeText("Name=Tank\nMainModule=main\nVersion=1.2\nApkVersionCode=0")
            }

        val settings = Project.open(config).apkSettings

        assertEquals("", settings.label)
        assertEquals("Tank", settings.labelOr("Tank"))
        // a version code of zero cannot be installed, so it is treated as absent
        assertNull(settings.versionCode)
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
        assertFalse(project.apkSettings.keyboardEnabled)
        assertEquals(ApkOrientation.PORTRAIT, project.apkSettings.orientation)
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

        for (project in listOf(oldProject, newProject)) {
            assertFalse(project.apkSettings.keyboardEnabled)
            assertEquals(ApkOrientation.PORTRAIT, project.apkSettings.orientation)
            assertEquals("", project.apkSettings.label)
            assertNull(project.apkSettings.versionCode)
            assertEquals(
                ApkSettings.DEFAULT_ICON_BACKGROUND,
                project.apkSettings.iconBackground
            )
        }
    }

    @Test
    fun aBrokenIconColourFallsBackToTheDefaultTile() {
        val projectDir = temporaryFolder.newFolder("colour")
        val config =
            File(projectDir, "colour.aproj").apply {
                writeText("Name=game\nMainModule=main\nIconBackground=nonsense")
            }

        val settings = Project.open(config).apkSettings

        assertEquals(ApkSettings.DEFAULT_ICON_BACKGROUND, settings.iconBackground)
        assertEquals(ApkSettings.DEFAULT_ICON_BACKGROUND_COLOR, settings.iconBackgroundColor)
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
