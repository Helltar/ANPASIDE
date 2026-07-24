package com.github.helltar.anpaside.ui.editor

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.ui.platform.launchInBuiltInEmulator
import com.github.helltar.anpaside.ui.platform.launchInExternalEmulator
import com.github.helltar.anpaside.ui.platform.shareFile
import com.github.helltar.anpaside.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

private const val DOCS_URL = "https://helltar.com/midletpascal"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    workspaceViewModel: WorkspaceViewModel,
    settingsViewModel: SettingsViewModel,
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = settingsViewModel.state
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
    var logVisible by rememberSaveable { mutableStateOf(false) }
    var seenErrors by rememberSaveable { mutableIntStateOf(workspaceViewModel.errorCount) }
    var menuExpanded by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<EditorDialog?>(null) }
    var importDestination by remember { mutableStateOf("") }

    BackHandler(enabled = logVisible || workspaceViewModel.hasModifiedDocuments) {
        if (logVisible) {
            logVisible = false
        } else {
            dialog = EditorDialog.Exit
        }
    }

    // a failed build is the one moment the log has to be on screen without being asked for
    LaunchedEffect(workspaceViewModel.errorCount) {
        if (workspaceViewModel.errorCount > seenErrors) {
            logVisible = true
        }

        seenErrors = workspaceViewModel.errorCount
    }

    // the editor cursor handle is a popup, so it would otherwise draw over the drawer
    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) {
            focusManager.clearFocus()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && importDestination.isNotEmpty()) {
            workspaceViewModel.importDocument(uri, importDestination)
        }
    }

    fun closeDrawer() = scope.launch { drawerState.close() }

    fun importInto(directoryPath: String) {
        importDestination = directoryPath
        importLauncher.launch(arrayOf("*/*"))
    }

    fun share(filePath: String) {
        if (!shareFile(context, filePath, workspaceViewModel::reportError)) {
            scope.launch { snackbarHostState.showSnackbar(noShareAppMessage) }
        }
    }

    fun save() {
        workspaceViewModel.saveAll { saved ->
            if (saved) {
                scope.launch { snackbarHostState.showSnackbar(savedMessage) }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProjectFilesDrawer(
                workspace = workspaceViewModel,
                onOpenProjects = {
                    closeDrawer()
                    onOpenProjects()
                },
                onNewModule = {
                    closeDrawer()
                    dialog = EditorDialog.NewModule
                },
                onProjectConfig = {
                    closeDrawer()
                    dialog = EditorDialog.ProjectMetadata
                },
                onImportInto = { directoryPath ->
                    closeDrawer()
                    importInto(directoryPath)
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
                            text = workspaceViewModel.selectedDocument?.name
                                ?: stringResource(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        if (workspaceViewModel.isBuilding) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.padding(horizontal = 12.dp).size(24.dp)
                            )
                        } else {
                            IconButton(
                                enabled = workspaceViewModel.isProjectOpen,
                                onClick = {
                                    workspaceViewModel.buildProject { builtMidlet ->
                                        val started =
                                            if (settings.builtInEmulator) {
                                                launchInBuiltInEmulator(
                                                    context = context,
                                                    jarPath = builtMidlet.jarPath,
                                                    projectName = builtMidlet.projectName,
                                                    screenWidth = settings.screenSize.width,
                                                    screenHeight = settings.screenSize.height,
                                                    showKeyboard = settings.virtualKeyboard,
                                                    onError = workspaceViewModel::reportError
                                                )
                                            } else {
                                                launchInExternalEmulator(
                                                    context,
                                                    builtMidlet.jarPath,
                                                    workspaceViewModel::reportError
                                                )
                                            }

                                        if (!started) {
                                            dialog = EditorDialog.NoJarHandler
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

                        IconButton(
                            onClick = ::save,
                            enabled = workspaceViewModel.hasModifiedDocuments
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_save),
                                contentDescription = stringResource(R.string.menu_file_save)
                            )
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert),
                                    contentDescription = stringResource(R.string.menu_more_actions)
                                )
                            }

                            EditorOverflowMenu(
                                expanded = menuExpanded,
                                onDismiss = { menuExpanded = false },
                                onToggleLog = { logVisible = !logVisible },
                                onOpenSettings = onOpenSettings,
                                onDocumentation = { uriHandler.openUri(DOCS_URL) },
                                onAbout = { dialog = EditorDialog.About },
                                onExit = {
                                    if (workspaceViewModel.hasModifiedDocuments) {
                                        dialog = EditorDialog.Exit
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
                if (workspaceViewModel.documents.isNotEmpty()) {
                    EditorTabs(
                        documents = workspaceViewModel.documents,
                        selectedIndex = workspaceViewModel.selectedDocumentIndex,
                        onSelect = workspaceViewModel::selectDocument,
                        onClose = workspaceViewModel::closeDocument
                    )
                }

                val file = workspaceViewModel.selectedDocument

                if (file != null) {
                    CodeEditor(
                        file = file,
                        fontSize = settings.fontSize,
                        highlighterEnabled = settings.syntaxHighlighting,
                        lineNumbersEnabled = settings.lineNumbers,
                        wordWrapEnabled = settings.wordWrap,
                        onSaveShortcut = ::save,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        if (workspaceViewModel.isProjectOpen) {
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
                    SymbolBar(onInsert = file::insertText)
                }

                if (logVisible) {
                    LogPanel(
                        messages = workspaceViewModel.logEntries,
                        onClear = workspaceViewModel::clearLog,
                        onHide = { logVisible = false },
                        onErrorClick = workspaceViewModel::openCompilerError
                    )
                }
            }
        }
    }

    EditorDialogHost(
        dialog = dialog,
        workspace = workspaceViewModel,
        onDialogChange = { dialog = it },
        onOpenLicenses = onOpenLicenses,
        onExit = { activity?.finish() }
    )
}
