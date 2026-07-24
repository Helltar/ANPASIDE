package com.github.helltar.anpaside.ui.projects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.project.CreationResult
import com.github.helltar.anpaside.project.ProjectNames
import com.github.helltar.anpaside.ui.components.ConfirmDialog
import com.github.helltar.anpaside.ui.components.MessageDialog
import com.github.helltar.anpaside.ui.components.TextInputDialog
import com.github.helltar.anpaside.ui.editor.WorkspaceViewModel

@Composable
fun NewProjectDialogs(
    projectsViewModel: ProjectsViewModel,
    workspaceViewModel: WorkspaceViewModel,
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreated: () -> Unit = {}
) {
    var overwriteName by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val nameTooShortMessage = pluralStringResource(
        R.plurals.err_project_name_least_chars,
        ProjectNames.MIN_LENGTH,
        ProjectNames.MIN_LENGTH
    )
    val invalidNameMessage = stringResource(R.string.err_invalid_project_name)

    fun create(name: String, overwrite: Boolean) {
        projectsViewModel.create(name, overwrite) { result ->
            when (result) {
                CreationResult.NAME_TOO_SHORT -> {
                    onDismiss()
                    errorMessage = nameTooShortMessage
                }

                CreationResult.INVALID_NAME -> {
                    onDismiss()
                    errorMessage = invalidNameMessage
                }

                CreationResult.ALREADY_EXISTS -> {
                    overwriteName = name
                    onDismiss()
                }

                CreationResult.CREATED -> {
                    overwriteName = null
                    onDismiss()

                    if (overwrite) {
                        workspaceViewModel.discardProjectSession(name)
                    }

                    workspaceViewModel.openProject(name) { opened ->
                        if (opened) {
                            onCreated()
                        }
                    }
                }

                CreationResult.FAILED -> {
                    overwriteName = null
                    onDismiss()
                }
            }
        }
    }

    if (visible) {
        TextInputDialog(
            title = stringResource(R.string.dlg_title_new_project),
            label = stringResource(R.string.dlg_hint_project_name),
            confirmText = stringResource(R.string.dlg_btn_create),
            supportingText = projectsViewModel.projectsDirectory,
            onConfirm = { create(it, overwrite = false) },
            onDismiss = onDismiss
        )
    }

    overwriteName?.let { name ->
        ConfirmDialog(
            text = stringResource(R.string.err_project_exists),
            confirmText = stringResource(R.string.dlg_btn_rewrite),
            onConfirm = { create(name, overwrite = true) },
            onDismiss = { overwriteName = null }
        )
    }

    errorMessage?.let { message ->
        MessageDialog(
            title = stringResource(R.string.dlg_title_invalid_value),
            text = message,
            onDismiss = { errorMessage = null }
        )
    }
}
