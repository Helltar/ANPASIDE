package com.github.helltar.anpaside.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.helltar.anpaside.foundation.AppDirectories
import com.github.helltar.anpaside.preferences.EditorPreferences
import com.github.helltar.anpaside.preferences.AppPreferences
import com.github.helltar.anpaside.preferences.ScreenSize

data class SettingsUiState(
    val fontSize: Int,
    val syntaxHighlighting: Boolean,
    val lineNumbers: Boolean,
    val wordWrap: Boolean,
    val builtInEmulator: Boolean,
    val screenSize: ScreenSize,
    val virtualKeyboard: Boolean,
    val globalLibrariesDirectory: String
)

class SettingsViewModel(
    private val editorPreferences: EditorPreferences,
    private val appPreferences: AppPreferences,
    directories: AppDirectories
) : ViewModel() {

    val workspaceDirectory: String = directories.workspaceDirectory.path
    val availableScreenSizes: List<ScreenSize> = AppPreferences.availableScreenSizes
    val fontSizeRange: IntRange = EditorPreferences.FONT_SIZE_RANGE

    var state by mutableStateOf(loadState())
        private set

    fun setFontSize(value: Int) {
        val fontSize = value.coerceIn(fontSizeRange)
        editorPreferences.fontSize = fontSize
        state = state.copy(fontSize = fontSize)
    }

    fun setSyntaxHighlighting(enabled: Boolean) {
        editorPreferences.highlighterEnabled = enabled
        state = state.copy(syntaxHighlighting = enabled)
    }

    fun setLineNumbers(enabled: Boolean) {
        editorPreferences.lineNumbersEnabled = enabled
        state = state.copy(lineNumbers = enabled)
    }

    fun setWordWrap(enabled: Boolean) {
        editorPreferences.wordWrapEnabled = enabled
        state = state.copy(wordWrap = enabled)
    }

    fun setBuiltInEmulator(enabled: Boolean) {
        appPreferences.builtInEmulatorEnabled = enabled
        state = state.copy(builtInEmulator = enabled)
    }

    fun setScreenSize(size: ScreenSize) {
        appPreferences.screenWidth = size.width
        appPreferences.screenHeight = size.height
        state = state.copy(screenSize = size)
    }

    fun setVirtualKeyboard(enabled: Boolean) {
        appPreferences.virtualKeyboardEnabled = enabled
        state = state.copy(virtualKeyboard = enabled)
    }

    fun setGlobalLibrariesDirectory(path: String) {
        appPreferences.globalLibrariesDirectory = path
        state = state.copy(globalLibrariesDirectory = path)
    }

    private fun loadState(): SettingsUiState =
        SettingsUiState(
            fontSize = editorPreferences.fontSize.coerceIn(EditorPreferences.FONT_SIZE_RANGE),
            syntaxHighlighting = editorPreferences.highlighterEnabled,
            lineNumbers = editorPreferences.lineNumbersEnabled,
            wordWrap = editorPreferences.wordWrapEnabled,
            builtInEmulator = appPreferences.builtInEmulatorEnabled,
            screenSize = ScreenSize(appPreferences.screenWidth, appPreferences.screenHeight),
            virtualKeyboard = appPreferences.virtualKeyboardEnabled,
            globalLibrariesDirectory = appPreferences.globalLibrariesDirectory
        )
}
