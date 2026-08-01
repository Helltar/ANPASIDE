package com.github.helltar.anpaside.ui.editor

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R

@Composable
fun EditorOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpenProjects: () -> Unit,
    onToggleLog: () -> Unit,
    onOpenSettings: () -> Unit,
    onDocumentation: () -> Unit,
    onAbout: () -> Unit,
    onExit: () -> Unit
) {
    @Composable
    fun item(
        @DrawableRes icon: Int,
        @StringRes text: Int,
        onClick: () -> Unit
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(text)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = {
                onDismiss()
                onClick()
            }
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        item(R.drawable.ic_folder, R.string.lbl_projects, onOpenProjects)
        item(R.drawable.ic_subject, R.string.lbl_log, onToggleLog)
        item(R.drawable.ic_settings, R.string.menu_settings, onOpenSettings)
        item(R.drawable.ic_help, R.string.menu_documentation, onDocumentation)
        item(R.drawable.ic_info, R.string.menu_about, onAbout)
        item(R.drawable.ic_logout, R.string.menu_exit, onExit)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTabs(
    documents: List<EditorDocument>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit
) {
    if (documents.isEmpty()) {
        return
    }

    val safeSelectedIndex = selectedIndex.coerceIn(0, documents.lastIndex)

    PrimaryScrollableTabRow(
        selectedTabIndex = safeSelectedIndex,
        edgePadding = 0.dp
    ) {
        documents.forEachIndexed { index, document ->
            Tab(
                selected = index == safeSelectedIndex,
                onClick = { onSelect(index) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (document.isModified) {
                                document.name + " •"
                            } else {
                                document.name
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.pmenu_tab_close),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onClose(index) }
                        )
                    }
                }
            )
        }
    }
}
