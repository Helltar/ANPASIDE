package com.github.helltar.anpaside.ui.editor

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.ui.AboutDialog
import com.github.helltar.anpaside.ui.ConfirmDialog
import com.github.helltar.anpaside.ui.CreateResult
import com.github.helltar.anpaside.ui.IdeViewModel
import com.github.helltar.anpaside.ui.MessageDialog
import com.github.helltar.anpaside.ui.ProjectConfigDialog
import com.github.helltar.anpaside.ui.TextInputDialog
import com.github.helltar.anpaside.ui.projects.ProjectDrawer
import com.github.helltar.anpaside.ui.runJar
import com.github.helltar.anpaside.ui.runJarExternally
import com.github.helltar.anpaside.ui.shareFile
import kotlinx.coroutines.launch

private const val DOCS_URL = "https://helltar.com/midletpascal"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: IdeViewModel,
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // read before the editor column consumes the ime insets
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val savedMessage = stringResource(R.string.msg_saved)
    val noShareAppMessage = stringResource(R.string.err_no_share_app)
    val moduleTooShortMessage = pluralStringResource(
        R.plurals.err_module_name_least_chars,
        IdeViewModel.MIN_NAME_LENGTH,
        IdeViewModel.MIN_NAME_LENGTH
    )

    var logVisible by rememberSaveable { mutableStateOf(false) }
    var seenErrors by rememberSaveable { mutableIntStateOf(viewModel.errorCount) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showNewModule by remember { mutableStateOf(false) }
    var showProjectConfig by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showNoJarApp by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var overwriteModuleName by remember { mutableStateOf<String?>(null) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var importDir by remember { mutableStateOf("") }

    // a failed build is the one moment the log has to be on screen without being asked for
    LaunchedEffect(viewModel.errorCount) {
        if (viewModel.errorCount > seenErrors) {
            logVisible = true
        }

        seenErrors = viewModel.errorCount
    }

    // the editor cursor handle is a popup, so it would otherwise draw over the drawer
    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) {
            focusManager.clearFocus()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && importDir.isNotEmpty()) {
            viewModel.importFileTo(uri, importDir)
        }
    }

    fun closeDrawer() = scope.launch { drawerState.close() }

    fun importInto(dir: String) {
        importDir = dir
        importLauncher.launch(arrayOf("*/*"))
    }

    fun share(filename: String) {
        if (!shareFile(context, filename)) {
            scope.launch { snackbarHostState.showSnackbar(noShareAppMessage) }
        }
    }

    fun save() {
        if (viewModel.saveAll()) {
            scope.launch { snackbarHostState.showSnackbar(savedMessage) }
        }
    }

    fun createModule(name: String, overwrite: Boolean) {
        when (viewModel.createModule(name, overwrite)) {
            CreateResult.NAME_TOO_SHORT -> alertMessage = moduleTooShortMessage

            CreateResult.ALREADY_EXISTS -> {
                showNewModule = false
                overwriteModuleName = name
            }

            else -> {
                showNewModule = false
                overwriteModuleName = null
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProjectDrawer(
                viewModel = viewModel,
                onOpenProjects = {
                    closeDrawer()
                    onOpenProjects()
                },
                onNewModule = {
                    closeDrawer()
                    showNewModule = true
                },
                onProjectConfig = {
                    closeDrawer()
                    showProjectConfig = true
                },
                onImportInto = { dir ->
                    closeDrawer()
                    importInto(dir)
                },
                onShare = ::share,
                onClose = { closeDrawer() }
            )
        }
    ) {
        Scaffold(
            modifier = modifier,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        actionColor = MaterialTheme.colorScheme.primary,
                        dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu),
                                contentDescription = stringResource(R.string.lbl_project_files)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = viewModel.currentFile?.name ?: stringResource(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        if (viewModel.isBuilding) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.padding(horizontal = 12.dp).size(24.dp)
                            )
                        } else {
                            IconButton(
                                enabled = viewModel.isProjectOpen,
                                onClick = {
                                    viewModel.buildAndRun { jarFilename ->
                                        val started =
                                            if (viewModel.embeddedEmulatorEnabled) {
                                                runJar(
                                                    context = context,
                                                    filename = jarFilename,
                                                    projectName = viewModel.openProjectName,
                                                    screenWidth = viewModel.midletScreenWidth,
                                                    screenHeight = viewModel.midletScreenHeight,
                                                    showKeyboard = viewModel.midletKeyboardEnabled
                                                )
                                            } else {
                                                runJarExternally(context, jarFilename)
                                            }

                                        if (!started) {
                                            showNoJarApp = true
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play_arrow),
                                    contentDescription = stringResource(R.string.menu_run)
                                )
                            }
                        }

                        IconButton(onClick = ::save, enabled = viewModel.hasModifiedFiles) {
                            Icon(
                                painter = painterResource(R.drawable.ic_save),
                                contentDescription = stringResource(R.string.menu_file_save)
                            )
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert),
                                    contentDescription = null
                                )
                            }

                            EditorMenu(
                                expanded = menuExpanded,
                                onDismiss = { menuExpanded = false },
                                onToggleLog = { logVisible = !logVisible },
                                onOpenSettings = onOpenSettings,
                                onDocumentation = { uriHandler.openUri(DOCS_URL) },
                                onAbout = { showAbout = true },
                                onExit = {
                                    if (viewModel.hasModifiedFiles) {
                                        showExitConfirm = true
                                    } else {
                                        activity?.finish()
                                    }
                                }
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .fillMaxSize()
                    .imePadding()
            ) {
                if (viewModel.openFiles.isNotEmpty()) {
                    FileTabs(viewModel)
                }

                val file = viewModel.currentFile

                if (file != null) {
                    CodeEditor(
                        file = file,
                        fontSize = viewModel.fontSize,
                        highlighterEnabled = viewModel.highlighterEnabled,
                        lineNumbersEnabled = viewModel.lineNumbersEnabled,
                        wordWrapEnabled = viewModel.wordWrapEnabled,
                        onSaveShortcut = ::save,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        if (viewModel.isProjectOpen) {
                            Text(
                                text = stringResource(R.string.lbl_open_file_to_edit),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.lbl_open_project_to_start),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )

                                Button(
                                    onClick = onOpenProjects,
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text(stringResource(R.string.lbl_projects))
                                }
                            }
                        }
                    }
                }

                if (file != null && keyboardVisible) {
                    HorizontalDivider()
                    SymbolBar(onInsert = file::insert)
                }

                if (logVisible) {
                    LogPanel(
                        messages = viewModel.log,
                        onClear = viewModel::clearLog,
                        onHide = { logVisible = false },
                        onErrorClick = viewModel::openCompilerError
                    )
                }
            }
        }
    }

    if (showNewModule) {
        TextInputDialog(
            title = stringResource(R.string.dlg_title_new_module),
            label = stringResource(R.string.dlg_hint_module_name),
            confirmText = stringResource(R.string.dlg_btn_create),
            onConfirm = { createModule(it, overwrite = false) },
            onDismiss = { showNewModule = false }
        )
    }

    overwriteModuleName?.let { name ->
        ConfirmDialog(
            text = stringResource(R.string.err_module_exists),
            confirmText = stringResource(R.string.dlg_btn_rewrite),
            onConfirm = { createModule(name, overwrite = true) },
            onDismiss = { overwriteModuleName = null }
        )
    }

    if (showProjectConfig) {
        ProjectConfigDialog(
            config = viewModel.projectConfig(),
            onSave = {
                viewModel.saveProjectConfig(it)
                showProjectConfig = false
            },
            onDismiss = { showProjectConfig = false }
        )
    }

    if (showAbout) {
        AboutDialog(
            onOpenLicenses = {
                showAbout = false
                onOpenLicenses()
            },
            onDismiss = { showAbout = false }
        )
    }

    if (showNoJarApp) {
        MessageDialog(
            title = stringResource(R.string.menu_run),
            text = stringResource(R.string.err_no_jar_app),
            onDismiss = { showNoJarApp = false }
        )
    }

    if (showExitConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.menu_exit),
            text = stringResource(R.string.dlg_msg_save_modified_files),
            confirmText = stringResource(R.string.dlg_btn_yes),
            dismissText = stringResource(R.string.dlg_btn_no),
            onConfirm = {
                viewModel.saveAll()
                activity?.finish()
            },
            onDismiss = {
                showExitConfirm = false
                activity?.finish()
            }
        )
    }

    alertMessage?.let { message ->
        MessageDialog(
            title = stringResource(R.string.dlg_title_invalid_value),
            text = message,
            onDismiss = { alertMessage = null }
        )
    }
}

@Composable
private fun EditorMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onToggleLog: () -> Unit,
    onOpenSettings: () -> Unit,
    onDocumentation: () -> Unit,
    onAbout: () -> Unit,
    onExit: () -> Unit
) {
    @Composable
    fun item(@DrawableRes icon: Int, @StringRes text: Int, enabled: Boolean = true, onClick: () -> Unit) {
        DropdownMenuItem(
            text = { Text(stringResource(text)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            enabled = enabled,
            onClick = {
                onDismiss()
                onClick()
            }
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        item(R.drawable.ic_subject, R.string.lbl_log, onClick = onToggleLog)
        item(R.drawable.ic_settings, R.string.menu_settings, onClick = onOpenSettings)
        item(R.drawable.ic_help, R.string.menu_documentation, onClick = onDocumentation)
        item(R.drawable.ic_info, R.string.menu_about, onClick = onAbout)
        item(R.drawable.ic_logout, R.string.menu_exit, onClick = onExit)
    }
}

@Composable
private fun FileTabs(viewModel: IdeViewModel) {
    val selectedIndex = viewModel.currentFileIndex.coerceIn(0, viewModel.openFiles.lastIndex)

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 0.dp
    ) {
        viewModel.openFiles.forEachIndexed { index, file ->
            Tab(
                selected = index == selectedIndex,
                onClick = { viewModel.selectFile(index) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (file.isModified) file.name + " •" else file.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.pmenu_tab_close),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.closeFile(index) }
                        )
                    }
                }
            )
        }
    }
}
