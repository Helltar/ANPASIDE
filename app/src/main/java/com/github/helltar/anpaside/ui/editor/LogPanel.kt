package com.github.helltar.anpaside.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.core.LogLevel
import com.github.helltar.anpaside.core.LogMessage
import com.github.helltar.anpaside.ui.theme.LocalSyntaxColors

private val PANEL_HEIGHT = 132.dp

// error.c prints the offending place as "unit.pas(12)", with the bare file name
private val ERROR_LOCATION = Regex("""([^\s\\/]+\.pas)\((\d+)\)""", RegexOption.IGNORE_CASE)

// build output, hidden until asked for or until something fails
@Composable
fun LogPanel(
    messages: List<LogMessage>,
    onClear: () -> Unit,
    onHide: () -> Unit,
    onErrorClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.weight(1f))

            IconButton(onClick = onClear) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.lbl_log_clear),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onHide) {
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = stringResource(R.string.lbl_log_hide)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(PANEL_HEIGHT)
                .padding(horizontal = 16.dp)
        ) {
            items(messages) { message ->
                val location = message.location()

                Text(
                    text = message.text,
                    color = message.color(),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = if (location != null) TextDecoration.Underline else null,
                    modifier = Modifier
                        .let {
                            if (location == null) {
                                it
                            } else {
                                it.clickable { onErrorClick(location.first, location.second) }
                            }
                        }
                        .padding(bottom = 2.dp)
                )
            }
        }
    }
}

private fun LogMessage.location(): Pair<String, Int>? {
    if (level != LogLevel.ERROR) {
        return null
    }

    val match = ERROR_LOCATION.find(text) ?: return null
    val line = match.groupValues[2].toIntOrNull() ?: return null

    return match.groupValues[1] to line
}

@Composable
private fun LogMessage.color() = when (level) {
    LogLevel.TEXT -> MaterialTheme.colorScheme.onSurfaceVariant
    LogLevel.INFO -> LocalSyntaxColors.current.logInfo
    LogLevel.ERROR -> MaterialTheme.colorScheme.error
}
