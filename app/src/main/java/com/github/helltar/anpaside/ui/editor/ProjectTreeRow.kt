package com.github.helltar.anpaside.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.project.ProjectTreeEntry

private val IndentPerLevel = 16.dp

@Composable
fun ProjectTreeRow(
    entry: ProjectTreeEntry,
    isCurrent: Boolean,
    onOpen: () -> Unit,
    onImport: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            // a file the editor cannot show opens its action menu instead
            .clickable {
                if (entry.isDirectory || entry.isTextFile) {
                    onOpen()
                } else {
                    menuExpanded = true
                }
            }
            .padding(start = 12.dp + IndentPerLevel * entry.nestingLevel, end = 4.dp)
    ) {
        Icon(
            painter = painterResource(entry.icon()),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
        )

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(R.string.menu_more_actions),
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (entry.isDirectory) {
                    MenuItem(R.string.menu_import_file, R.drawable.ic_file_download) {
                        menuExpanded = false
                        onImport()
                    }
                } else {
                    MenuItem(R.string.menu_share, R.drawable.ic_share) {
                        menuExpanded = false
                        onShare()
                    }
                }

                if (entry.canRename) {
                    MenuItem(R.string.dlg_btn_rename, R.drawable.ic_edit) {
                        menuExpanded = false
                        onRename()
                    }
                }

                if (entry.canDelete) {
                    MenuItem(R.string.dlg_btn_delete, R.drawable.ic_delete) {
                        menuExpanded = false
                        onDelete()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(label: Int, icon: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        leadingIcon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        onClick = onClick
    )
}

private fun ProjectTreeEntry.icon() = when {
    isDirectory && isExpanded -> R.drawable.ic_expand_more
    isDirectory -> R.drawable.ic_chevron_right
    else -> R.drawable.ic_description
}
