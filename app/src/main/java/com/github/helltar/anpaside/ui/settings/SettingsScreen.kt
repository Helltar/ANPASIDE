package com.github.helltar.anpaside.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.github.helltar.anpaside.core.Paths
import com.github.helltar.anpaside.core.prefs.EditorPrefs
import com.github.helltar.anpaside.core.prefs.IdePrefs
import com.github.helltar.anpaside.ui.BackButton
import com.github.helltar.anpaside.ui.IdeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: IdeViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.text_editor_font_size) + ": " + viewModel.fontSize,
                modifier = Modifier.padding(top = 16.dp)
            )

            Slider(
                value = viewModel.fontSize.toFloat(),
                onValueChange = { viewModel.fontSize = it.toInt() },
                valueRange = EditorPrefs.MIN_FONT_SIZE.toFloat()..EditorPrefs.MAX_FONT_SIZE.toFloat(),
                steps = EditorPrefs.MAX_FONT_SIZE - EditorPrefs.MIN_FONT_SIZE - 1
            )

            SwitchRow(
                text = stringResource(R.string.text_editor_highlighter),
                checked = viewModel.highlighterEnabled,
                onCheckedChange = { viewModel.highlighterEnabled = it }
            )

            SwitchRow(
                text = stringResource(R.string.text_editor_line_numbers),
                checked = viewModel.lineNumbersEnabled,
                onCheckedChange = { viewModel.lineNumbersEnabled = it }
            )

            SwitchRow(
                text = stringResource(R.string.text_editor_word_wrap),
                checked = viewModel.wordWrapEnabled,
                onCheckedChange = { viewModel.wordWrapEnabled = it }
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            SwitchRow(
                text = stringResource(R.string.text_emulator_builtin),
                checked = viewModel.embeddedEmulatorEnabled,
                onCheckedChange = { viewModel.embeddedEmulatorEnabled = it }
            )

            Text(
                text = stringResource(R.string.text_emulator_builtin_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.text_emulator_screen),
                style = MaterialTheme.typography.titleSmall,
                color = emulatorSettingsColor(viewModel.embeddedEmulatorEnabled),
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
                IdePrefs.SCREEN_SIZES.forEach { (width, height) ->
                    val selected = width == viewModel.midletScreenWidth
                            && height == viewModel.midletScreenHeight

                    FilterChip(
                        selected = selected,
                        enabled = viewModel.embeddedEmulatorEnabled,
                        onClick = {
                            viewModel.midletScreenWidth = width
                            viewModel.midletScreenHeight = height
                        },
                        label = {
                            Text(
                                if (width <= 0 || height <= 0) {
                                    stringResource(R.string.lbl_screen_fit_device)
                                } else {
                                    "$width × $height"
                                }
                            )
                        }
                    )
                }
            }

            SwitchRow(
                text = stringResource(R.string.text_emulator_keyboard),
                checked = viewModel.midletKeyboardEnabled,
                onCheckedChange = { viewModel.midletKeyboardEnabled = it },
                enabled = viewModel.embeddedEmulatorEnabled
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            OutlinedTextField(
                value = viewModel.globalLibsDir,
                onValueChange = { viewModel.globalLibsDir = it },
                label = { Text(stringResource(R.string.text_global_directory_libs)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.lbl_workdir) + ": " + Paths.workDir.path,
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
