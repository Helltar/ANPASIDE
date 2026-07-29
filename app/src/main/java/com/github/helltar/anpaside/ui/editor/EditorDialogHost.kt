package com.github.helltar.anpaside.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.project.CreationResult
import com.github.helltar.anpaside.project.ProjectNames
import com.github.helltar.anpaside.ui.about.AboutDialog
import com.github.helltar.anpaside.ui.components.ConfirmDialog
import com.github.helltar.anpaside.ui.components.MessageDialog
import com.github.helltar.anpaside.ui.components.TextInputDialog

sealed interface EditorDialog {
    data object NewModule : EditorDialog
    data class OverwriteModule(val name: String) : EditorDialog
    data object ProjectMetadata : EditorDialog
    data object About : EditorDialog
    data object NoJarHandler : EditorDialog
    data object Exit : EditorDialog
    data class Alert(val message: String) : EditorDialog
}

@Composable
fun EditorDialogHost(
    dialog: EditorDialog?,
    workspace: WorkspaceViewModel,
    onDialogChange: (EditorDialog?) -> Unit,
    onOpenLicenses: () -> Unit,
    onExit: () -> Unit
) {
    val moduleNameTooShort = pluralStringResource(
        R.plurals.err_module_name_least_chars,
        ProjectNames.MIN_LENGTH,
        ProjectNames.MIN_LENGTH
    )
    val invalidModuleName = stringResource(R.string.err_invalid_module_name)

    fun createModule(name: String, overwrite: Boolean) {
        workspace.createModule(name, overwrite) { result ->
            onDialogChange(
                when (result) {
                    CreationResult.NAME_TOO_SHORT -> EditorDialog.Alert(moduleNameTooShort)
                    CreationResult.INVALID_NAME -> EditorDialog.Alert(invalidModuleName)
                    CreationResult.ALREADY_EXISTS -> EditorDialog.OverwriteModule(name)
                    CreationResult.CREATED,
                    CreationResult.FAILED -> null
                }
            )
        }
    }

    when (dialog) {
        null -> Unit

        EditorDialog.NewModule ->
            TextInputDialog(
                title = stringResource(R.string.dlg_title_new_module),
                label = stringResource(R.string.dlg_hint_module_name),
                confirmText = stringResource(R.string.dlg_btn_create),
                onConfirm = { createModule(it, overwrite = false) },
                onDismiss = { onDialogChange(null) }
            )

        is EditorDialog.OverwriteModule ->
            ConfirmDialog(
                text = stringResource(R.string.err_module_exists),
                confirmText = stringResource(R.string.dlg_btn_rewrite),
                onConfirm = { createModule(dialog.name, overwrite = true) },
                onDismiss = { onDialogChange(null) }
            )

        EditorDialog.ProjectMetadata ->
            ProjectMetadataDialog(
                metadata = workspace.currentProjectMetadata(),
                packageName = workspace.currentPackageName(),
                onSave = { metadata, packageName ->
                    workspace.saveProjectMetadata(metadata, packageName) { saved ->
                        if (saved) {
                            onDialogChange(null)
                        }
                    }
                },
                onDismiss = { onDialogChange(null) }
            )

        EditorDialog.About ->
            AboutDialog(
                onOpenLicenses = {
                    onDialogChange(null)
                    onOpenLicenses()
                },
                onDismiss = { onDialogChange(null) }
            )

        EditorDialog.NoJarHandler ->
            MessageDialog(
                title = stringResource(R.string.menu_run),
                text = stringResource(R.string.err_no_jar_app),
                onDismiss = { onDialogChange(null) }
            )

        EditorDialog.Exit ->
            ConfirmDialog(
                title = stringResource(R.string.menu_exit),
                text = stringResource(R.string.dlg_msg_save_modified_files),
                confirmText = stringResource(R.string.dlg_btn_yes),
                dismissText = stringResource(R.string.dlg_btn_no),
                onConfirm = {
                    workspace.saveAll { saved ->
                        if (saved) {
                            onExit()
                        }
                    }
                },
                onDismiss = onExit,
                onDismissRequest = { onDialogChange(null) }
            )

        is EditorDialog.Alert ->
            MessageDialog(
                title = stringResource(R.string.dlg_title_invalid_value),
                text = dialog.message,
                onDismiss = { onDialogChange(null) }
            )
    }
}
