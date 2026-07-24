package com.github.helltar.anpaside.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.project.MidletMetadata

@Composable
fun ProjectMetadataDialog(
    metadata: MidletMetadata,
    onSave: (MidletMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(metadata.name) }
    var vendor by rememberSaveable { mutableStateOf(metadata.vendor) }
    var version by rememberSaveable { mutableStateOf(metadata.version) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manifest_mf)) },
        text = {
            // landscape leaves the dialog short, so fields scroll instead of clipping
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MetadataField(name, { name = it }, "MIDlet-Name")
                MetadataField(vendor, { vendor = it }, "MIDlet-Vendor", Modifier.padding(top = 8.dp))
                MetadataField(version, { version = it }, "MIDlet-Version", Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(MidletMetadata(name.trim(), vendor.trim(), version.trim()))
                },
                enabled = name.isNotBlank()
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
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

