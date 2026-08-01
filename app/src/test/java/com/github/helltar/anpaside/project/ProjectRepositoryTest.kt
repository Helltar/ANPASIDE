package com.github.helltar.anpaside.project

import com.github.helltar.anpaside.foundation.AppDirectories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun createsCompleteProjectAndListsIt() {
        val fixture = fixture()

        val project = fixture.repository.create("Demo", TEMPLATES, overwrite = false)

        assertEquals(listOf("Demo"), fixture.repository.listNames())
        assertEquals("program demo; begin end.", project.mainModule.readText())
        assertTrue(project.resourcesDirectory.resolve("icon.png").isFile)
        assertTrue(project.configFile.isFile)
    }

    @Test
    fun projectNameThatIsNotAPascalIdentifierUsesMainModule() {
        val fixture = fixture()

        val project = fixture.repository.create("My Game", TEMPLATES, overwrite = false)

        assertEquals("main", project.mainModuleName)
        assertEquals("program main; begin end.", project.mainModule.readText())
        assertEquals("My Game", project.metadata.name)
    }

    @Test
    fun failedCreateLeavesExistingProjectUntouched() {
        val fixture = fixture()
        val existing = fixture.repository.create("Demo", TEMPLATES, overwrite = false)
        existing.mainModule.writeText("keep me")
        fixture.directories.templateIcon.delete()

        assertThrows(Exception::class.java) {
            fixture.repository.create("Demo", TEMPLATES, overwrite = true)
        }

        assertEquals("keep me", fixture.repository.open("Demo").mainModule.readText())
        assertFalse(fixture.directories.projectsDirectory.resolve(".Demo.creating").exists())
    }

    @Test
    fun projectLookupCannotEscapeProjectsDirectory() {
        val fixture = fixture()

        assertThrows(IllegalArgumentException::class.java) {
            fixture.repository.projectDirectory("../outside")
        }
    }

    private fun fixture(): Fixture {
        val root = temporaryFolder.newFolder()
        val privateFiles = File(root, "private").apply { mkdirs() }
        val workspace = File(root, "workspace").apply { mkdirs() }
        val templates = File(privateFiles, "templates").apply { mkdirs() }
        val icon = File(templates, "icon.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val directories =
            AppDirectories(
                privateDataDirectory = File(root, "data"),
                privateFilesDirectory = privateFiles,
                runtimeLibraryDirectory = File(privateFiles, "rtl"),
                templatesDirectory = templates,
                templateIcon = icon,
                compilerExecutable = File(root, "mp3cc"),
                workspaceDirectory = workspace,
                projectsDirectory = File(workspace, "projects"),
                exportDirectory = File(root, "exports")
            )

        return Fixture(directories, ProjectRepository(directories))
    }

    private data class Fixture(
        val directories: AppDirectories,
        val repository: ProjectRepository
    )

    private companion object {
        val TEMPLATES =
            ProjectTemplates(
                mainModule = "program %s; begin end.",
                unitModule = "unit %s; end.",
                gitIgnore = "bin/"
            )
    }
}
