package com.github.helltar.anpaside.ui.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.ui.components.BackButton
import com.github.helltar.anpaside.ui.components.ConfirmDialog
import com.github.helltar.anpaside.ui.editor.WorkspaceViewModel
import com.github.helltar.anpaside.ui.platform.shareFile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projectsViewModel: ProjectsViewModel,
    workspaceViewModel: WorkspaceViewModel,
    onProjectOpened: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = projectsViewModel.state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val noShareAppMessage = stringResource(R.string.err_no_share_app)

    var showNewProject by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { projectsViewModel.refresh() }

    fun share(filePath: String) {
        if (!shareFile(context, filePath, workspaceViewModel::reportError)) {
            scope.launch { snackbarHostState.showSnackbar(noShareAppMessage) }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lbl_projects)) },
                navigationIcon = { BackButton(onBack) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewProject = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.menu_create_project)
                )
            }
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (!state.isLoading && state.names.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.lbl_no_projects),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(state.names) { name ->
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = if (workspaceViewModel.isProjectOpen(name)) {
                                { Text(stringResource(R.string.lbl_project_open)) }
                            } else {
                                null
                            },
                            leadingContent = {
                                Icon(painterResource(R.drawable.ic_folder), contentDescription = null)
                            },
                            trailingContent = {
                                Row {
                                    IconButton(
                                        onClick = { projectsViewModel.export(name, ::share) }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_archive),
                                            contentDescription = stringResource(R.string.menu_export_project)
                                        )
                                    }

                                    IconButton(onClick = { projectToDelete = name }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_delete),
                                            contentDescription = stringResource(R.string.dlg_btn_delete)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.clickable {
                                workspaceViewModel.openProject(name) { opened ->
                                    if (opened) {
                                        onProjectOpened()
                                    }
                                }
                            }
                        )

                        HorizontalDivider()
                    }
                }
            }
        }
    }

    NewProjectDialogs(
        projectsViewModel = projectsViewModel,
        workspaceViewModel = workspaceViewModel,
        visible = showNewProject,
        onDismiss = { showNewProject = false },
        onCreated = onProjectOpened
    )

    projectToDelete?.let { name ->
        ConfirmDialog(
            title = stringResource(R.string.dlg_title_delete_project),
            text = stringResource(R.string.dlg_msg_delete_project, name),
            confirmText = stringResource(R.string.dlg_btn_delete),
            onConfirm = {
                projectsViewModel.delete(name) { deleted ->
                    if (deleted) {
                        workspaceViewModel.discardProjectSession(name)
                        projectToDelete = null
                    }
                }
            },
            onDismiss = { projectToDelete = null }
        )
    }
}
