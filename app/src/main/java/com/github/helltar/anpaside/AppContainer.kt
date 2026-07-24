package com.github.helltar.anpaside

import android.app.Application
import com.github.helltar.anpaside.assets.AssetInstaller
import com.github.helltar.anpaside.assets.LicenseRepository
import com.github.helltar.anpaside.compiler.BuildMessages
import com.github.helltar.anpaside.compiler.ProjectBuildPipeline
import com.github.helltar.anpaside.foundation.AppDirectories
import com.github.helltar.anpaside.foundation.IdeLogger
import com.github.helltar.anpaside.foundation.StringResources
import com.github.helltar.anpaside.foundation.TextFileStore
import com.github.helltar.anpaside.preferences.EditorPreferences
import com.github.helltar.anpaside.preferences.AppPreferences
import com.github.helltar.anpaside.project.Project
import com.github.helltar.anpaside.project.ProjectFileManager
import com.github.helltar.anpaside.project.ProjectRepository
import com.github.helltar.anpaside.project.ProjectTemplates
import java.io.File

/**
 * Application-wide composition root.
 *
 * The app is small enough that explicit constructor injection is clearer than a dependency
 * injection framework. This is the only place that knows how concrete services are assembled.
 */
class AppContainer(application: Application) {
    val directories = AppDirectories.from(application)
    val strings = StringResources(application)
    val logger = IdeLogger()
    val editorPreferences = EditorPreferences(application)
    val appPreferences =
        AppPreferences(application, directories.globalLibrariesDirectory)
    val projectRepository = ProjectRepository(directories)
    val projectFileManager = ProjectFileManager()
    val textFileStore = TextFileStore()
    val assetInstaller = AssetInstaller(application.assets, directories)
    val licenseRepository = LicenseRepository(application.assets)
    val contentResolver = application.contentResolver

    val projectTemplates: ProjectTemplates
        get() =
            ProjectTemplates(
                mainModule = strings.get(R.string.tpl_helloworld),
                unitModule = strings.get(R.string.tpl_module),
                gitIgnore = strings.get(R.string.tpl_gitignore)
            )

    private val buildMessages: BuildMessages
        get() =
            BuildMessages(
                manifestTemplate = strings.get(R.string.tpl_manifest),
                fileNotFound = strings.get(R.string.err_file_not_found),
                compilerExitTemplate = strings.get(R.string.err_compiler_exit),
                mainClassMissing = strings.get(R.string.err_main_class_missing),
                archiveCreationFailed = strings.get(R.string.err_failed_create_archive),
                buildSucceeded = strings.get(R.string.msg_build_successfully)
            )

    fun createBuildPipeline(
        project: Project,
        globalLibrariesDirectory: File
    ): ProjectBuildPipeline =
        ProjectBuildPipeline(
            messages = buildMessages,
            project = project,
            compilerExecutable = directories.compilerExecutable,
            runtimeLibraryDirectory = directories.runtimeLibraryDirectory,
            globalLibrariesDirectory = globalLibrariesDirectory
        )
}
