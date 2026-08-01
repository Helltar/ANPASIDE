package com.github.helltar.anpaside.ui.apk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    var pickerOpen by rememberSaveable { mutableStateOf(false) }

    val packageValid = ProjectNames.isValidPackageName(packageName.trim())
    val parsedVersionCode = versionCode.trim().toIntOrNull()
    val versionCodeValid =
        versionCode.isBlank() || (parsedVersionCode ?: 0) >= Project.MIN_VERSION_CODE
    val iconBackgroundValid = HexColor.isValid(iconBackground.trim())
    val packageSupportingText =
        stringResource(R.string.err_invalid_package_name).takeIf { !packageValid }
    val labelSupportingText =
        stringResource(R.string.text_apk_label_hint).takeIf { label.isBlank() }
    val versionCodeSupportingText =
        when {
            !versionCodeValid -> stringResource(R.string.err_invalid_version_code)
            versionCode.isBlank() -> stringResource(R.string.text_apk_version_code_hint)
            else -> null
        }

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
                // the ime inset covers the navigation bar the scaffold already padded for
                .consumeWindowInsets(innerPadding)
                .fillMaxSize()
                // shrink the scroll viewport instead of letting the keyboard cover it
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            SettingsField(
                value = packageName,
                onValueChange = { packageName = it; apply() },
                label = stringResource(R.string.text_apk_package),
                modifier = Modifier.padding(top = 16.dp),
                isError = !packageValid,
                supportingText = packageSupportingText
            )

            SettingsField(
                value = label,
                onValueChange = { label = it; apply() },
                label = stringResource(R.string.text_apk_label),
                modifier = Modifier.padding(
                    top = if (packageSupportingText == null) 8.dp else 16.dp
                ),
                supportingText = labelSupportingText
            )

            SettingsField(
                value = versionCode,
                onValueChange = { versionCode = it; apply() },
                label = stringResource(R.string.text_apk_version_code),
                modifier = Modifier.padding(
                    top = if (labelSupportingText == null) 8.dp else 16.dp
                ),
                keyboardType = KeyboardType.Number,
                isError = !versionCodeValid,
                supportingText = versionCodeSupportingText
            )

            SettingsField(
                value = iconBackground,
                onValueChange = { iconBackground = it; apply() },
                label = stringResource(R.string.text_apk_icon_background),
                modifier = Modifier.padding(
                    top = if (versionCodeSupportingText == null) 8.dp else 16.dp
                ),
                isError = !iconBackgroundValid,
                supportingText = stringResource(R.string.err_invalid_icon_background)
                    .takeIf { !iconBackgroundValid },
                // the swatch is the button: it shows the color the field spells out and opens
                // the picker, which is the only other way to change it
                trailingIcon = {
                    IconButton(onClick = { pickerOpen = true }) {
                        ColorSwatch(
                            color = HexColor.parse(iconBackground.trim()),
                            description = stringResource(R.string.text_apk_pick_color)
                        )
                    }
                }
            )

            Text(
                text = stringResource(R.string.text_apk_orientation),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 24.dp)
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
                    text = stringResource(R.string.text_apk_keyboard),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = keyboardEnabled,
                    onCheckedChange = { keyboardEnabled = it; apply() }
                )
            }
        }
    }

    if (pickerOpen) {
        ColorPickerDialog(
            // a half typed color in the field is no reason to open on black
            initialColor = HexColor.parse(iconBackground.trim())
                ?: ApkSettings.DEFAULT_ICON_BACKGROUND_COLOR,
            onPick = { picked ->
                iconBackground = HexColor.format(picked)
                pickerOpen = false
                apply()
            },
            onDismiss = { pickerOpen = false }
        )
    }
}

// an unparseable color leaves an empty ring rather than nothing to press
@Composable
private fun ColorSwatch(color: Int?, description: String) {
    Box(
        Modifier
            .size(24.dp)
            .semantics { contentDescription = description }
            .background(color?.let(::Color) ?: Color.Transparent, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
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
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = supportingText?.let { text -> { Text(text) } },
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth()
    )
}
