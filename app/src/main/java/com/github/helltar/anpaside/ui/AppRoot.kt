package com.github.helltar.anpaside.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.helltar.anpaside.AnpasideApplication
import com.github.helltar.anpaside.ui.about.LicensesScreen
import com.github.helltar.anpaside.ui.about.LicensesViewModel
import com.github.helltar.anpaside.ui.editor.EditorScreen
import com.github.helltar.anpaside.ui.editor.WorkspaceViewModel
import com.github.helltar.anpaside.ui.projects.ProjectsScreen
import com.github.helltar.anpaside.ui.projects.ProjectsViewModel
import com.github.helltar.anpaside.ui.settings.SettingsScreen
import com.github.helltar.anpaside.ui.settings.SettingsViewModel

// the editor is the app: the other screens are opened from it and lead back to it
private enum class AppScreen { EDITOR, PROJECTS, SETTINGS, LICENSES }

@Composable
fun AppRoot() {
    val application =
        LocalContext.current.applicationContext as AnpasideApplication
    val factory = remember(application) { AppViewModelFactory(application.container) }
    val workspaceViewModel: WorkspaceViewModel = viewModel(factory = factory)
    val projectsViewModel: ProjectsViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    var screen by rememberSaveable { mutableStateOf(AppScreen.EDITOR) }

    BackHandler(enabled = screen != AppScreen.EDITOR) {
        screen = AppScreen.EDITOR
    }

    when (screen) {
        AppScreen.EDITOR -> EditorScreen(
            workspaceViewModel = workspaceViewModel,
            settingsViewModel = settingsViewModel,
            onOpenProjects = { screen = AppScreen.PROJECTS },
            onOpenSettings = { screen = AppScreen.SETTINGS },
            onOpenLicenses = { screen = AppScreen.LICENSES }
        )

        AppScreen.PROJECTS -> ProjectsScreen(
            projectsViewModel = projectsViewModel,
            workspaceViewModel = workspaceViewModel,
            onProjectOpened = { screen = AppScreen.EDITOR },
            onBack = { screen = AppScreen.EDITOR }
        )

        AppScreen.SETTINGS -> SettingsScreen(
            viewModel = settingsViewModel,
            onBack = { screen = AppScreen.EDITOR }
        )

        AppScreen.LICENSES -> {
            val licensesViewModel: LicensesViewModel = viewModel(factory = factory)

            LicensesScreen(
                viewModel = licensesViewModel,
                onBack = { screen = AppScreen.EDITOR }
            )
        }
    }
}
