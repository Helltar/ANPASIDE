package com.github.helltar.anpaside.ui.apk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.github.helltar.anpaside.project.Project
import com.github.helltar.anpaside.project.ProjectNames
import com.github.helltar.anpaside.ui.components.BackButton
import com.github.helltar.anpaside.ui.editor.WorkspaceViewModel

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

/**
 * Everything about the apk an export produces, for the open project.
 *
 * A screen rather than a dialog, because these outgrew the MIDlet manifest they used to be shown
 * with. Changes apply as they are made, the way the app settings do, so there is no save button -
 * but only a whole valid [ApkSettings] can be written, so nothing is persisted while a field is
 * half typed. The text of an invalid field lives here and is dropped on the way back; what was
 * last valid stays in the `.aproj`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkSettingsScreen(
    workspace: WorkspaceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = workspace.currentApkSettings()

    var packageName by rememberSaveable { mutableStateOf(settings.packageName) }
    var label by rememberSaveable { mutableStateOf(settings.label) }
    // kept as text, because an emptied field means "follow the MIDlet version" and not zero
    var versionCode by rememberSaveable {
        mutableStateOf(settings.versionCode?.toString().orEmpty())
    }
    var iconBackground by rememberSaveable { mutableStateOf(settings.iconBackground) }
    var orientation by rememberSaveable { mutableStateOf(settings.orientation) }
    var keyboardEnabled by rememberSaveable { mutableStateOf(settings.keyboardEnabled) }

    val packageValid = ProjectNames.isValidPackageName(packageName.trim())
    val parsedVersionCode = versionCode.trim().toIntOrNull()
    val versionCodeValid =
        versionCode.isBlank() || (parsedVersionCode ?: 0) >= Project.MIN_VERSION_CODE
    val iconBackgroundValid = HexColor.isValid(iconBackground.trim())

    // every value is read from the state here rather than from what composition computed: this
    // runs inside onValueChange, before the recomposition that would refresh a captured val
    fun apply() {
        workspace.saveApkSettings(
            ApkSettings(
                packageName = packageName.trim(),
                label = label.trim(),
                versionCode = versionCode.trim().toIntOrNull(),
                iconBackground = iconBackground.trim(),
                orientation = orientation,
                keyboardEnabled = keyboardEnabled
            )
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lbl_apk_settings)) },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
        ) {
            SettingsField(
                value = packageName,
                onValueChange = { packageName = it; apply() },
                label = stringResource(R.string.dlg_hint_package),
                modifier = Modifier.padding(top = 16.dp),
                isError = !packageValid,
                supportingText = stringResource(R.string.err_invalid_package_name)
                    .takeIf { !packageValid }
            )

            SettingsField(
                value = label,
                onValueChange = { label = it; apply() },
                label = stringResource(R.string.dlg_hint_app_label),
                modifier = Modifier.padding(top = 8.dp),
                supportingText = stringResource(R.string.dlg_hint_app_label_empty)
                    .takeIf { label.isBlank() }
            )

            SettingsField(
                value = versionCode,
                onValueChange = { versionCode = it; apply() },
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

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            SettingsField(
                value = iconBackground,
                onValueChange = { iconBackground = it; apply() },
                label = stringResource(R.string.dlg_hint_icon_background),
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
                        onClick = { iconBackground = preset; apply() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text(
                text = stringResource(R.string.dlg_option_apk_orientation),
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                FilterChip(
                    selected = orientation == ApkOrientation.PORTRAIT,
                    onClick = { orientation = ApkOrientation.PORTRAIT; apply() },
                    label = { Text(stringResource(R.string.lbl_orientation_portrait)) }
                )
                FilterChip(
                    selected = orientation == ApkOrientation.LANDSCAPE,
                    onClick = { orientation = ApkOrientation.LANDSCAPE; apply() },
                    label = { Text(stringResource(R.string.lbl_orientation_landscape)) }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.dlg_option_apk_keyboard),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = keyboardEnabled,
                    onCheckedChange = { keyboardEnabled = it; apply() }
                )
            }
        }
    }
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
private fun SettingsField(
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
