package com.github.helltar.anpaside.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.project.ApkOrientation
import com.github.helltar.anpaside.project.ApkSettings
import com.github.helltar.anpaside.project.HexColor
import com.github.helltar.anpaside.project.MidletMetadata
import com.github.helltar.anpaside.project.Project
import com.github.helltar.anpaside.project.ProjectNames

// tiles a pixel sprite reads well on: the grey every export used to share, near black, four
// muted colours. anything else is typed into the field by hand
private val iconBackgroundPresets =
    listOf(
        ApkSettings.DEFAULT_ICON_BACKGROUND,
        "#1F2933",
        "#2E5E4E",
        "#7A3B2E",
        "#3B3A6B",
        "#E8E4D9"
    )

@Composable
fun ProjectMetadataDialog(
    metadata: MidletMetadata,
    apkSettings: ApkSettings,
    onSave: (MidletMetadata, ApkSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(metadata.name) }
    var vendor by rememberSaveable { mutableStateOf(metadata.vendor) }
    var version by rememberSaveable { mutableStateOf(metadata.version) }
    var packageId by rememberSaveable { mutableStateOf(apkSettings.packageName) }
    var label by rememberSaveable { mutableStateOf(apkSettings.label) }
    // kept as text, because an emptied field means "follow the MIDlet version" and not zero
    var versionCode by rememberSaveable {
        mutableStateOf(apkSettings.versionCode?.toString().orEmpty())
    }
    var iconBackground by rememberSaveable { mutableStateOf(apkSettings.iconBackground) }
    var showApkKeyboard by rememberSaveable { mutableStateOf(apkSettings.keyboardEnabled) }
    var selectedApkOrientation by rememberSaveable { mutableStateOf(apkSettings.orientation) }

    val packageValid = ProjectNames.isValidPackageName(packageId.trim())
    val parsedVersionCode = versionCode.trim().toIntOrNull()
    val versionCodeValid =
        versionCode.isBlank() || (parsedVersionCode ?: 0) >= Project.MIN_VERSION_CODE
    val iconBackgroundValid = HexColor.isValid(iconBackground.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dlg_title_project_config)) },
        text = {
            // landscape leaves the dialog short, so fields scroll instead of clipping
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MetadataField(name, { name = it }, "MIDlet-Name")
                MetadataField(vendor, { vendor = it }, "MIDlet-Vendor", Modifier.padding(top = 8.dp))
                MetadataField(version, { version = it }, "MIDlet-Version", Modifier.padding(top = 8.dp))

                SectionTitle(stringResource(R.string.dlg_section_apk))

                MetadataField(
                    value = packageId,
                    onValueChange = { packageId = it },
                    label = stringResource(R.string.dlg_hint_package),
                    isError = !packageValid,
                    supportingText = stringResource(R.string.err_invalid_package_name)
                        .takeIf { !packageValid }
                )

                MetadataField(
                    value = label,
                    onValueChange = { label = it },
                    label = stringResource(R.string.dlg_hint_app_label),
                    modifier = Modifier.padding(top = 8.dp),
                    supportingText = stringResource(R.string.dlg_hint_app_label_empty)
                        .takeIf { label.isBlank() }
                )

                MetadataField(
                    value = versionCode,
                    onValueChange = { versionCode = it },
                    label = stringResource(R.string.dlg_hint_version_code),
                    modifier = Modifier.padding(top = 8.dp),
                    keyboardType = KeyboardType.Number,
                    isError = !versionCodeValid,
                    supportingText =
                        when {
                            !versionCodeValid -> stringResource(R.string.err_invalid_version_code)

                            versionCode.isBlank() ->
                                stringResource(R.string.dlg_hint_version_code_empty)

                            else -> null
                        }
                )

                MetadataField(
                    value = iconBackground,
                    onValueChange = { iconBackground = it },
                    label = stringResource(R.string.dlg_hint_icon_background),
                    modifier = Modifier.padding(top = 8.dp),
                    isError = !iconBackgroundValid,
                    supportingText = stringResource(R.string.err_invalid_icon_background)
                        .takeIf { !iconBackgroundValid }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (preset in iconBackgroundPresets) {
                        ColorSwatch(
                            color = preset,
                            selected = preset.equals(iconBackground.trim(), ignoreCase = true),
                            onClick = { iconBackground = preset },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

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
                        ApkSettings(
                            packageName = packageId.trim(),
                            label = label.trim(),
                            versionCode = parsedVersionCode,
                            iconBackground = iconBackground.trim(),
                            orientation = selectedApkOrientation,
                            keyboardEnabled = showApkKeyboard
                        )
                    )
                },
                enabled =
                    name.isNotBlank() && packageValid && versionCodeValid && iconBackgroundValid
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun ColorSwatch(
    color: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val parsed = HexColor.parse(color) ?: return

    Box(
        modifier
            .aspectRatio(1f)
            .background(Color(parsed), CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun MetadataField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = supportingText?.let { text -> { Text(text) } },
        modifier = modifier.fillMaxWidth()
    )
}
