package com.github.helltar.anpaside.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.BuildConfig
import com.github.helltar.anpaside.R

private const val SITE_URL = "https://helltar.com"
private const val GITHUB_URL = "https://github.com/helltar/anpaside"
private const val PRIVACY_URL = "https://helltar.com/projects/anpaside/privacy-policy.html"

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    supportingText: String? = null,
    initialValue: String = ""
) {
    var value by rememberSaveable { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_btn_cancel)) }
        }
    )
}

@Composable
fun ConfirmDialog(
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
    dismissText: String = stringResource(R.string.dlg_btn_cancel)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title?.let { { Text(it) } },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } }
    )
}

@Composable
fun MessageDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_btn_ok)) } }
    )
}

@Composable
fun ProjectConfigDialog(
    config: ProjectConfig,
    onSave: (ProjectConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(config.name) }
    var vendor by rememberSaveable { mutableStateOf(config.vendor) }
    var version by rememberSaveable { mutableStateOf(config.version) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manifest_mf)) },
        text = {
            // landscape leaves the dialog short, so let the fields scroll rather than clip
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("MIDlet-Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("MIDlet-Vendor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("MIDlet-Version") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(ProjectConfig(name.trim(), vendor.trim(), version.trim())) }) {
                Text(stringResource(R.string.menu_file_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_btn_cancel)) }
        }
    )
}

@Composable
fun AboutDialog(onOpenLicenses: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colorResource(R.color.ic_launcher_background)),
                    contentAlignment = Alignment.Center
                ) {
                    // reuse the launcher artwork; scaled 108/72 so the triangle fills the badge
                    // the way the adaptive icon's mask crops to its safe zone
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(84.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(
                            R.string.about_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE.toString()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column {
                AboutLink(stringResource(R.string.about_link_website), SITE_URL)
                AboutLink(stringResource(R.string.about_link_source), GITHUB_URL)
                AboutLink(stringResource(R.string.about_link_privacy), PRIVACY_URL)
                AboutRow(stringResource(R.string.about_licenses), onOpenLicenses)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dlg_btn_ok)) } }
    )
}

// the new project flow is reachable from both the editor and the projects screen
@Composable
fun NewProjectDialogs(
    viewModel: IdeViewModel,
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreated: () -> Unit = {}
) {
    var overwriteName by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val nameTooShortMessage = pluralStringResource(
        R.plurals.err_project_name_least_chars,
        IdeViewModel.MIN_NAME_LENGTH,
        IdeViewModel.MIN_NAME_LENGTH
    )

    fun create(name: String, overwrite: Boolean) {
        when (viewModel.createProject(name, overwrite)) {
            CreateResult.NAME_TOO_SHORT -> errorMessage = nameTooShortMessage

            CreateResult.ALREADY_EXISTS -> {
                overwriteName = name
                onDismiss()
            }

            CreateResult.CREATED -> {
                overwriteName = null
                onDismiss()
                onCreated()
            }

            CreateResult.FAILED -> {
                overwriteName = null
                onDismiss()
            }
        }
    }

    if (visible) {
        TextInputDialog(
            title = stringResource(R.string.dlg_title_new_project),
            label = stringResource(R.string.dlg_hint_project_name),
            confirmText = stringResource(R.string.dlg_btn_create),
            supportingText = viewModel.projectsDir,
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

@Composable
private fun AboutLink(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    AboutRow(label) { uriHandler.openUri(url) }
}

@Composable
private fun AboutRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    )
}
