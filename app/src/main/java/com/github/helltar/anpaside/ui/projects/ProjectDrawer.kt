package com.github.helltar.anpaside.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.core.project.TreeNode
import com.github.helltar.anpaside.ui.ConfirmDialog
import com.github.helltar.anpaside.ui.IdeViewModel
import com.github.helltar.anpaside.ui.TextInputDialog

private val INDENT_PER_LEVEL = 16.dp

// files of the open project: the only way back to a module whose tab was closed
@Composable
fun ProjectDrawer(
    viewModel: IdeViewModel,
    onOpenProjects: () -> Unit,
    onNewModule: () -> Unit,
    onProjectConfig: () -> Unit,
    onImportInto: (String) -> Unit,
    onShare: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nodeToDelete by remember { mutableStateOf<TreeNode?>(null) }
    var nodeToRename by remember { mutableStateOf<TreeNode?>(null) }

    ModalDrawerSheet(modifier) {
        Text(
            text = viewModel.openProjectName.ifEmpty { stringResource(R.string.lbl_no_open_project) },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyColumn(Modifier.weight(1f)) {
            items(viewModel.projectTree, key = { it.path }) { node ->
                TreeRow(
                    node = node,
                    isCurrent = node.path == viewModel.currentFile?.path,
                    onOpen = {
                        if (node.isDirectory) {
                            viewModel.toggleDirectory(node.path)
                        } else {
                            viewModel.openFile(node.path)
                            onClose()
                        }
                    },
                    onImport = { onImportInto(node.path) },
                    onShare = { onShare(node.path) },
                    onRename = { nodeToRename = node },
                    onDelete = { nodeToDelete = node }
                )
            }
        }

        HorizontalDivider()

        DrawerFooter(
            isProjectOpen = viewModel.isProjectOpen,
            onNewModule = onNewModule,
            onProjectConfig = onProjectConfig,
            onOpenProjects = onOpenProjects
        )
    }

    nodeToRename?.let { node ->
        TextInputDialog(
            title = stringResource(R.string.dlg_title_rename),
            label = stringResource(R.string.dlg_hint_new_name),
            initialValue = node.name,
            confirmText = stringResource(R.string.dlg_btn_rename),
            onConfirm = { newName ->
                if (viewModel.renameProjectFile(node.path, newName)) {
                    nodeToRename = null
                }
            },
            onDismiss = { nodeToRename = null }
        )
    }

    nodeToDelete?.let { node ->
        ConfirmDialog(
            title = stringResource(R.string.dlg_title_delete_file),
            text = stringResource(R.string.dlg_msg_delete_file, node.name),
            confirmText = stringResource(R.string.dlg_btn_delete),
            onConfirm = {
                viewModel.deleteProjectFile(node.path)
                nodeToDelete = null
            },
            onDismiss = { nodeToDelete = null }
        )
    }
}

private data class FooterTile(
    val icon: Int,
    val label: String,
    val onClick: () -> Unit
)

// footer: icon tiles laid out three to a row, project actions shown only while a project is open
@Composable
private fun DrawerFooter(
    isProjectOpen: Boolean,
    onNewModule: () -> Unit,
    onProjectConfig: () -> Unit,
    onOpenProjects: () -> Unit
) {
    val tiles = buildList {
        if (isProjectOpen) {
            add(FooterTile(R.drawable.ic_add, stringResource(R.string.menu_create_module), onNewModule))
            add(FooterTile(R.drawable.ic_description, stringResource(R.string.manifest_mf), onProjectConfig))
        }

        add(FooterTile(R.drawable.ic_folder, stringResource(R.string.lbl_projects), onOpenProjects))
    }

    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        tiles.chunked(3).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) {
                Spacer(Modifier.height(8.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { tile ->
                    DrawerTile(
                        icon = tile.icon,
                        label = tile.label,
                        onClick = tile.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                // pad a short last row so tiles keep the same width across rows
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MenuIcon(icon: Int) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun DrawerTile(icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun TreeRow(
    node: TreeNode,
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
            // a file the editor cannot show has nothing to open, so it goes straight to its menu
            .clickable { if (node.isDirectory || node.isText) onOpen() else menuExpanded = true }
            .padding(start = 12.dp + INDENT_PER_LEVEL * node.depth, end = 4.dp)
    ) {
        Icon(
            painter = painterResource(node.icon()),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = node.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isCurrent) {
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
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (node.isDirectory) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_import_file)) },
                        leadingIcon = { MenuIcon(R.drawable.ic_file_download) },
                        onClick = {
                            menuExpanded = false
                            onImport()
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_share)) },
                        leadingIcon = { MenuIcon(R.drawable.ic_share) },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dlg_btn_rename)) },
                    leadingIcon = { MenuIcon(R.drawable.ic_edit) },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dlg_btn_delete)) },
                    leadingIcon = { MenuIcon(R.drawable.ic_delete) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

private fun TreeNode.icon() = when {
    isDirectory && isExpanded -> R.drawable.ic_expand_more
    isDirectory -> R.drawable.ic_chevron_right
    else -> R.drawable.ic_description
}
