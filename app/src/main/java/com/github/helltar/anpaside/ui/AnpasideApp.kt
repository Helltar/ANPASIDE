package com.github.helltar.anpaside.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.helltar.anpaside.ui.editor.EditorScreen
import com.github.helltar.anpaside.ui.projects.ProjectsScreen
import com.github.helltar.anpaside.ui.settings.SettingsScreen

// the editor is the app: the other screens are opened from it and lead back to it
enum class AppDestination { EDITOR, PROJECTS, SETTINGS, LICENSES }

@Composable
fun AnpasideApp(viewModel: IdeViewModel = viewModel()) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.EDITOR) }

    BackHandler(enabled = destination != AppDestination.EDITOR) {
        destination = AppDestination.EDITOR
    }

    when (destination) {
        AppDestination.EDITOR -> EditorScreen(
            viewModel = viewModel,
            onOpenProjects = { destination = AppDestination.PROJECTS },
            onOpenSettings = { destination = AppDestination.SETTINGS },
            onOpenLicenses = { destination = AppDestination.LICENSES }
        )

        AppDestination.PROJECTS -> ProjectsScreen(
            viewModel = viewModel,
            onProjectOpened = { destination = AppDestination.EDITOR },
            onBack = { destination = AppDestination.EDITOR }
        )

        AppDestination.SETTINGS -> SettingsScreen(
            viewModel = viewModel,
            onBack = { destination = AppDestination.EDITOR }
        )

        AppDestination.LICENSES -> LicensesScreen(
            onBack = { destination = AppDestination.EDITOR }
        )
    }
}
