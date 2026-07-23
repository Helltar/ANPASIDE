package com.github.helltar.anpaside.core.project

import org.junit.Assert.assertEquals
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
        }.run { config.outputStream().use { store(it, null) } }

        val project = Project.open(config)

        assertEquals(projectDir, project.dir)
        assertEquals(config, project.configFile)
        assertEquals(File(projectDir, "src/main.pas"), project.mainModuleFile)
        assertEquals(File(projectDir, "bin/My Game.jar"), project.jarFile)
        assertEquals("Helltar", project.midletVendor)
        assertEquals(2, project.mathType)

        project.midletVersion = "3.0"
        project.save()

        val saved = Properties().apply { config.inputStream().use(::load) }
        assertEquals("3.0", saved.getProperty("Version"))
    }
}
