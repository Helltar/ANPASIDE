package com.github.helltar.anpaside.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.project.ApkOrientation
import com.github.helltar.anpaside.project.MidletMetadata
import com.github.helltar.anpaside.project.ProjectNames

@Composable
fun ProjectMetadataDialog(
    metadata: MidletMetadata,
    packageName: String,
    apkKeyboardEnabled: Boolean,
    apkOrientation: ApkOrientation,
    onSave: (MidletMetadata, String, ApkOrientation, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(metadata.name) }
    var vendor by rememberSaveable { mutableStateOf(metadata.vendor) }
    var version by rememberSaveable { mutableStateOf(metadata.version) }
    var packageId by rememberSaveable { mutableStateOf(packageName) }
    var showApkKeyboard by rememberSaveable { mutableStateOf(apkKeyboardEnabled) }
    var selectedApkOrientation by rememberSaveable { mutableStateOf(apkOrientation) }

    val packageValid = ProjectNames.isValidPackageName(packageId.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dlg_title_project_config)) },
        text = {
            // landscape leaves the dialog short, so fields scroll instead of clipping
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MetadataField(name, { name = it }, "MIDlet-Name")
                MetadataField(vendor, { vendor = it }, "MIDlet-Vendor", Modifier.padding(top = 8.dp))
                MetadataField(version, { version = it }, "MIDlet-Version", Modifier.padding(top = 8.dp))

                MetadataField(
                    value = packageId,
                    onValueChange = { packageId = it },
                    label = stringResource(R.string.dlg_hint_package),
                    modifier = Modifier.padding(top = 8.dp),
                    isError = !packageValid,
                    supportingText = stringResource(R.string.err_invalid_package_name)
                        .takeIf { !packageValid }
                )

                Text(
                    text = stringResource(R.string.dlg_option_apk_orientation),
                    modifier = Modifier.padding(top = 12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedApkOrientation == ApkOrientation.PORTRAIT,
                        onClick = { selectedApkOrientation = ApkOrientation.PORTRAIT },
                        label = { Text(stringResource(R.string.lbl_orientation_portrait)) }
                    )
                    FilterChip(
                        selected = selectedApkOrientation == ApkOrientation.LANDSCAPE,
                        onClick = { selectedApkOrientation = ApkOrientation.LANDSCAPE },
                        label = { Text(stringResource(R.string.lbl_orientation_landscape)) }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dlg_option_apk_keyboard),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = showApkKeyboard,
                        onCheckedChange = { showApkKeyboard = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        MidletMetadata(name.trim(), vendor.trim(), version.trim()),
                        packageId.trim(),
                        selectedApkOrientation,
                        showApkKeyboard
                    )
                },
                enabled = name.isNotBlank() && packageValid
            ) {
                Text(stringResource(R.string.menu_file_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dlg_btn_cancel))
            }
        }
    )
}

@Composable
private fun MetadataField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { text -> { Text(text) } },
        modifier = modifier.fillMaxWidth()
    )
}
