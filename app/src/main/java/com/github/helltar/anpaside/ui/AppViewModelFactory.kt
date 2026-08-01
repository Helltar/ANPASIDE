package com.github.helltar.anpaside.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.helltar.anpaside.AppContainer
import com.github.helltar.anpaside.ui.about.LicensesViewModel
import com.github.helltar.anpaside.ui.editor.WorkspaceViewModel
import com.github.helltar.anpaside.ui.projects.ProjectsViewModel
import com.github.helltar.anpaside.ui.settings.SettingsViewModel

class AppViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when {
            modelClass == WorkspaceViewModel::class.java ->
                WorkspaceViewModel(
                    projects = container.projectRepository,
                    projectFiles = container.projectFileManager,
                    textFiles = container.textFileStore,
                    projectTemplates = container.projectTemplates,
                    editorPreferences = container.editorPreferences,
                    appPreferences = container.appPreferences,
                    assetInstaller = container.assetInstaller,
                    contentResolver = container.contentResolver,
                    strings = container.strings,
                    logger = container.logger,
                    buildPipeline = container::createBuildPipeline,
                    apkExporter = container.apkExporter
                )

            modelClass == ProjectsViewModel::class.java ->
                ProjectsViewModel(
                    projects = container.projectRepository,
                    templates = container.projectTemplates,
                    strings = container.strings,
                    logger = container.logger
                )

            modelClass == SettingsViewModel::class.java ->
                SettingsViewModel(
                    editorPreferences = container.editorPreferences,
                    appPreferences = container.appPreferences
                )

            modelClass == LicensesViewModel::class.java ->
                LicensesViewModel(
                    repository = container.licenseRepository,
                    logger = container.logger
                )

            else -> error("Unknown ViewModel class: ${modelClass.name}")
        } as T
}
