package com.github.helltar.anpaside.ui.editor

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.github.helltar.anpaside.ui.theme.LocalSyntaxColors

// hosts the code field, a platform view (see CodeEditorView for why), and hands it the
// document and the settings. one view serves every tab, so it keeps its focus and the
// keyboard across a tab switch
@Composable
fun CodeEditor(
    file: EditorDocument,
    fontSize: Int,
    highlighterEnabled: Boolean,
    lineNumbersEnabled: Boolean,
    wordWrapEnabled: Boolean,
    onSaveShortcut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syntaxColors = LocalSyntaxColors.current
    val colorScheme = MaterialTheme.colorScheme
    val currentOnSaveShortcut by rememberUpdatedState(onSaveShortcut)

    val palette = remember(syntaxColors, colorScheme) {
        EditorPalette(
            text = colorScheme.onSurface.toArgb(),
            background = colorScheme.surface.toArgb(),
            accent = colorScheme.primary.toArgb(),
            keyword = syntaxColors.keyword.toArgb(),
            string = syntaxColors.string.toArgb(),
            number = syntaxColors.number.toArgb(),
            comment = syntaxColors.comment.toArgb(),
            lineNumber = syntaxColors.lineNumber.toArgb()
        )
    }

    AndroidView(
        factory = { context ->
            CodeEditorView(context).apply {
                this.onSaveShortcut = { currentOnSaveShortcut() }
            }
        },
        // everything bind() reads from the document is snapshot state, so this runs again
        // whenever the text, the folds or the caret request change
        update = { view ->
            view.configure(
                fontSize = fontSize,
                highlighterEnabled = highlighterEnabled,
                lineNumbersEnabled = lineNumbersEnabled,
                wordWrapEnabled = wordWrapEnabled,
                palette = palette
            )
            view.bind(file)
        },
        modifier = modifier.background(colorScheme.surface)
    )
}
