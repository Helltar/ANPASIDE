package com.github.helltar.anpaside.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

// what a phone keyboard hides behind its symbol page, with the most-used entries first
private val SYMBOLS = listOf(
    ";", ":=", "(", ")", ",", "'", ".", ":", "+", "-", "=", "*", "/", "<", ">", "_", "[", "]"
)

// shown above the keyboard while a file is open: every entry is inserted at the caret
@Composable
fun SymbolBar(onInsert: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .horizontalScroll(rememberScrollState())
    ) {
        SYMBOLS.forEach { symbol ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable { onInsert(symbol) }
                    .defaultMinSize(minWidth = 44.dp)
                    .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = symbol,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
