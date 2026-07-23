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
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.core.Paths
import com.github.helltar.anpaside.ui.BackButton
import com.github.helltar.anpaside.ui.ConfirmDialog
import com.github.helltar.anpaside.ui.IdeViewModel
import com.github.helltar.anpaside.ui.NewProjectDialogs
import com.github.helltar.anpaside.ui.shareFile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: IdeViewModel,
    onProjectOpened: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val noShareAppMessage = stringResource(R.string.err_no_share_app)

    var showNewProject by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshProjects() }

    fun share(filename: String) {
        if (!shareFile(context, filename)) {
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
            if (viewModel.projects.isEmpty()) {
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
                    items(viewModel.projects) { name ->
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = if (viewModel.isOpenProject(name)) {
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
                                        onClick = { viewModel.exportProjectZip(name, Paths.exportDir, ::share) }
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
                                viewModel.openProject(name)
                                onProjectOpened()
                            }
                        )

                        HorizontalDivider()
                    }
                }
            }

            Text(
                text = viewModel.projectsDir,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // keep the path clear of the floating action button
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 88.dp, bottom = 16.dp)
            )
        }
    }

    NewProjectDialogs(
        viewModel = viewModel,
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
                viewModel.deleteProject(name)
                projectToDelete = null
            },
            onDismiss = { projectToDelete = null }
        )
    }
}
