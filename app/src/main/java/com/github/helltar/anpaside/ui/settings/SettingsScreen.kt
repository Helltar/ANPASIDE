package com.github.helltar.anpaside.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.ui.components.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_settings)) },
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
            Text(
                text = stringResource(R.string.text_editor_font_size) + ": " + state.fontSize,
                modifier = Modifier.padding(top = 16.dp)
            )

            Slider(
                value = state.fontSize.toFloat(),
                onValueChange = { viewModel.setFontSize(it.toInt()) },
                valueRange =
                    viewModel.fontSizeRange.first.toFloat()..viewModel.fontSizeRange.last.toFloat(),
                steps = viewModel.fontSizeRange.last - viewModel.fontSizeRange.first - 1
            )

            SwitchRow(
                text = stringResource(R.string.text_editor_highlighter),
                checked = state.syntaxHighlighting,
                onCheckedChange = viewModel::setSyntaxHighlighting
            )

            SwitchRow(
                text = stringResource(R.string.text_editor_line_numbers),
                checked = state.lineNumbers,
                onCheckedChange = viewModel::setLineNumbers
            )

            SwitchRow(
                text = stringResource(R.string.text_editor_word_wrap),
                checked = state.wordWrap,
                onCheckedChange = viewModel::setWordWrap
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            SwitchRow(
                text = stringResource(R.string.text_emulator_builtin),
                checked = state.builtInEmulator,
                onCheckedChange = viewModel::setBuiltInEmulator
            )

            Text(
                text = stringResource(R.string.text_emulator_builtin_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.text_emulator_screen),
                style = MaterialTheme.typography.titleSmall,
                color = emulatorSettingsColor(state.builtInEmulator),
                modifier = Modifier.padding(top = 20.dp)
            )

            Text(
                text = stringResource(R.string.text_emulator_screen_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                viewModel.availableScreenSizes.forEach { size ->
                    val selected = size == state.screenSize

                    FilterChip(
                        selected = selected,
                        enabled = state.builtInEmulator,
                        onClick = { viewModel.setScreenSize(size) },
                        label = {
                            Text(
                                when {
                                    size.usesNativeResolution ->
                                        stringResource(R.string.lbl_screen_device_resolution)
                                    size.width == 0 || size.height == 0 ->
                                        stringResource(R.string.lbl_screen_fit_device)
                                    else -> "${size.width} × ${size.height}"
                                }
                            )
                        }
                    )
                }
            }

            SwitchRow(
                text = stringResource(R.string.text_emulator_keyboard),
                checked = state.virtualKeyboard,
                onCheckedChange = viewModel::setVirtualKeyboard,
                enabled = state.builtInEmulator
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            OutlinedTextField(
                value = state.globalLibrariesDirectory,
                onValueChange = viewModel::setGlobalLibrariesDirectory,
                label = { Text(stringResource(R.string.text_global_directory_libs)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.lbl_workdir) + ": " + viewModel.workspaceDirectory,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }
    }
}

@Composable
private fun SwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(text, color = emulatorSettingsColor(enabled), modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

// the emulator options stay visible when the built in emulator is off, just muted
@Composable
private fun emulatorSettingsColor(enabled: Boolean) =
    if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
