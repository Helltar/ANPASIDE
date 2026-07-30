package com.github.helltar.anpaside.ui.editor

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.helltar.anpaside.BuildConfig
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.apk.ApkApplication
import com.github.helltar.anpaside.apk.ApkExportRequest
import com.github.helltar.anpaside.apk.ApkExporter
import com.github.helltar.anpaside.apk.ApkVersions
import com.github.helltar.anpaside.assets.AssetInstaller
import com.github.helltar.anpaside.compiler.ProjectBuildPipeline
import com.github.helltar.anpaside.foundation.IdeLogger
import com.github.helltar.anpaside.foundation.LogEntry
import com.github.helltar.anpaside.foundation.LogSeverity
import com.github.helltar.anpaside.foundation.ProjectLayout
import com.github.helltar.anpaside.foundation.StringResources
import com.github.helltar.anpaside.foundation.TextFileStore
import com.github.helltar.anpaside.preferences.AppPreferences
import com.github.helltar.anpaside.preferences.EditorPreferences
import com.github.helltar.anpaside.project.ApkOrientation
import com.github.helltar.anpaside.project.ApkSettings
import com.github.helltar.anpaside.project.CreationResult
import com.github.helltar.anpaside.project.MidletMetadata
import com.github.helltar.anpaside.project.Project
import com.github.helltar.anpaside.project.ProjectFileManager
import com.github.helltar.anpaside.project.ProjectNames
import com.github.helltar.anpaside.project.ProjectRepository
import com.github.helltar.anpaside.project.ProjectTemplates
import com.github.helltar.anpaside.project.ProjectTreeEntry
import com.github.helltar.anpaside.project.buildProjectTree
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.FileAlreadyExistsException

data class BuiltMidlet(
    val jarPath: String,
    val projectName: String
)

/**
 * Converts a built jar into the files an exported apk carries. It runs through the embedded
 * emulator's dexer, which needs a context, so the screen passes it in the same way it passes
 * the launcher.
 */
fun interface MidletConverter {
    fun convert(
        jarPath: String,
        projectName: String,
        showKeyboard: Boolean,
        orientation: ApkOrientation
    ): Map<String, File>
}

class WorkspaceViewModel(
    private val projects: ProjectRepository,
    private val projectFiles: ProjectFileManager,
    textFiles: TextFileStore,
    private val projectTemplates: ProjectTemplates,
    private val editorPreferences: EditorPreferences,
    private val appPreferences: AppPreferences,
    private val assetInstaller: AssetInstaller,
    private val contentResolver: ContentResolver,
    private val strings: StringResources,
    private val logger: IdeLogger,
    private val buildPipeline: (Project, File) -> ProjectBuildPipeline,
    private val apkExporter: ApkExporter,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var project by mutableStateOf<Project?>(null)
    private val expandedDirectories = mutableSetOf<String>()
    private var treeRequest = 0
    private var projectOpenRequest = 0
    private val editorSession =
        EditorSession(
            files = textFiles,
            preferences = editorPreferences,
            logger = logger,
            ioDispatcher = ioDispatcher
        )

    val logEntries = mutableStateListOf<LogEntry>()

    val documents: List<EditorDocument>
        get() = editorSession.documents

    val selectedDocumentIndex: Int
        get() = editorSession.selectedIndex

    var projectTree by mutableStateOf(emptyList<ProjectTreeEntry>())
        private set

    var isBuilding by mutableStateOf(false)
        private set

    var isExporting by mutableStateOf(false)
        private set

    var errorCount by mutableIntStateOf(0)
        private set

    val selectedDocument: EditorDocument?
        get() = editorSession.selectedDocument

    var projectTitle by mutableStateOf("")
        private set

    val isProjectOpen: Boolean
        get() = project != null

    val hasModifiedDocuments: Boolean
        get() = editorSession.hasModifiedDocuments

    init {
        viewModelScope.launch {
            logger.entries.collect(::appendLogEntry)
        }

        logger.plain(strings.get(R.string.app_name) + " " + BuildConfig.VERSION_NAME)

        viewModelScope.launch {
            installAssetsIfNeeded()
            restoreSession()
        }
    }

    fun isProjectOpen(name: String): Boolean =
        project?.rootDirectory?.name == name

    fun openProject(name: String, onOpened: (Boolean) -> Unit = {}) {
        val request = ++projectOpenRequest

        viewModelScope.launch {
            val opened =
                runCatching {
                    withContext(ioDispatcher) { projects.open(name) }
                }.onFailure(logger::error)
                    .getOrNull()

            if (opened == null || request != projectOpenRequest) {
                onOpened(false)
                return@launch
            }

            activateProject(opened)
            editorSession.open(opened.mainModule.path)
            onOpened(true)
        }
    }

    fun openDocument(path: String, line: Int = 0) {
        viewModelScope.launch {
            editorSession.open(path, line)
        }
    }

    fun selectDocument(index: Int) = editorSession.select(index)

    fun closeDocument(index: Int) {
        viewModelScope.launch {
            editorSession.close(index)
        }
    }

    fun saveAll(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onComplete(editorSession.saveAll())
        }
    }

    // mp3cc reports a bare source name, and pascal units live in src/
    fun openCompilerError(fileName: String, line: Int) {
        val source = project?.sourcesDirectory?.resolve(fileName) ?: return

        if (source.isFile) {
            openDocument(source.path, line)
        }
    }

    fun toggleDirectory(path: String) {
        if (!expandedDirectories.add(path)) {
            expandedDirectories.remove(path)
        }

        refreshProjectTree()
    }

    fun deleteProjectEntry(path: String) {
        val activeProject = project ?: return

        viewModelScope.launch {
            val deleted =
                runCatching {
                    withContext(ioDispatcher) { projectFiles.delete(activeProject, path) }
                }.onFailure {
                    logger.error(strings.get(R.string.err_delete_file) + ": " + path)
                }.isSuccess

            if (!deleted) {
                return@launch
            }

            editorSession.closeUnder(path)
            expandedDirectories.removeAll { it == path || it.startsWith("$path/") }
            refreshProjectTree()
        }
    }

    fun renameProjectEntry(path: String, newName: String, onComplete: (Boolean) -> Unit = {}) {
        val activeProject = project ?: return onComplete(false)

        if (!ProjectNames.isValidEntryName(newName)) {
            logger.error(strings.get(R.string.err_invalid_file_name))
            onComplete(false)
            return
        }

        val snapshots = editorSession.modifiedSnapshotsUnder(path)

        viewModelScope.launch {
            if (!editorSession.saveSnapshots(snapshots)) {
                onComplete(false)
                return@launch
            }

            val target =
                try {
                    withContext(ioDispatcher) {
                        projectFiles.rename(activeProject, path, newName)
                    }
                } catch (_: FileAlreadyExistsException) {
                    logger.error(strings.get(R.string.err_file_exists) + ": " + newName)
                    onComplete(false)
                    return@launch
                } catch (error: Exception) {
                    logger.error(strings.get(R.string.err_rename_file) + ": " + path)
                    onComplete(false)
                    return@launch
                }

            editorSession.markSaved(snapshots)
            editorSession.relocate(path, target.path)
            relocateExpandedDirectories(path, target.path)
            refreshProjectTree()
            onComplete(true)
        }
    }

    fun importDocument(uri: Uri, destinationDirectory: String) {
        val activeProject = project ?: return

        viewModelScope.launch {
            val imported =
                runCatching {
                    withContext(ioDispatcher) {
                        projectFiles.import(
                            resolver = contentResolver,
                            uri = uri,
                            project = activeProject,
                            destinationPath = destinationDirectory
                        )
                    }
                }.onFailure(logger::error)
                    .getOrNull()
                    ?: return@launch

            logger.info(strings.get(R.string.msg_imported) + ": " + imported.name)
            refreshProjectTree()
        }
    }

    fun currentProjectMetadata(): MidletMetadata =
        project?.metadata ?: MidletMetadata("", "", "")

    fun currentApkSettings(): ApkSettings = project?.apkSettings ?: DEFAULT_APK_SETTINGS

    fun saveProjectMetadata(
        metadata: MidletMetadata,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val activeProject = project ?: return onComplete(false)

        if (!ProjectNames.isValidMetadata(metadata)) {
            logger.error(strings.get(R.string.err_invalid_midlet_metadata))
            onComplete(false)
            return
        }

        val previous = activeProject.metadata

        viewModelScope.launch {
            val saved =
                runCatching {
                    withContext(ioDispatcher) {
                        activeProject.updateMetadata(metadata)
                        activeProject.save()
                    }
                }.onFailure { error ->
                    activeProject.updateMetadata(previous)
                    logger.error(error)
                }.isSuccess

            if (saved) {
                projectTitle = metadata.name
            }

            onComplete(saved)
        }
    }

    /**
     * Writes the apk settings of the open project, if they are complete enough to write.
     *
     * The apk settings screen applies every change as it is made, so this is called while fields
     * are being typed into and has to ignore what it cannot store - an unfinished package name is
     * not an error worth logging, it is a field the user is still in the middle of.
     */
    fun saveApkSettings(settings: ApkSettings) {
        val activeProject = project ?: return

        if (settings == activeProject.apkSettings || !ProjectNames.isValidApkSettings(settings)) {
            return
        }

        val previous = activeProject.apkSettings

        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    activeProject.updateApkSettings(settings)
                    activeProject.save()
                }
            }.onFailure { error ->
                activeProject.updateApkSettings(previous)
                logger.error(error)
            }
        }
    }

    fun createModule(
        name: String,
        overwrite: Boolean = false,
        onResult: (CreationResult) -> Unit
    ) {
        if (name.length < ProjectNames.MIN_LENGTH) {
            onResult(CreationResult.NAME_TOO_SHORT)
            return
        }

        if (!ProjectNames.isValidModuleName(name)) {
            onResult(CreationResult.INVALID_NAME)
            return
        }

        val activeProject = project ?: return onResult(CreationResult.FAILED)

        viewModelScope.launch {
            val existing = activeProject.sourcesDirectory
                .resolve(name + ProjectLayout.PASCAL_EXTENSION)

            if (withContext(ioDispatcher) { existing.exists() } && !overwrite) {
                onResult(CreationResult.ALREADY_EXISTS)
                return@launch
            }

            val module =
                runCatching {
                    withContext(ioDispatcher) {
                        projectFiles.createModule(
                            project = activeProject,
                            name = name,
                            template = projectTemplates.unitModule,
                            overwrite = overwrite
                        )
                    }
                }.onFailure(logger::error)
                    .getOrNull()

            if (module == null) {
                onResult(CreationResult.FAILED)
                return@launch
            }

            openDocument(module.path)
            refreshProjectTree()
            onResult(CreationResult.CREATED)
        }
    }

    fun discardProjectSession(name: String) {
        projectOpenRequest++

        val directory = projects.projectDirectory(name)
        editorSession.closeUnder(directory.path)

        if (project?.rootDirectory == directory) {
            project = null
            projectTitle = ""
            projectTree = emptyList()
            editorPreferences.lastProject = ""
        }
    }

    fun clearLog() = logEntries.clear()

    fun reportError(error: Throwable) = logger.error(error)

    fun buildProject(onBuilt: (BuiltMidlet) -> Unit) {
        val activeProject = project

        if (activeProject == null) {
            logger.error(strings.get(R.string.msg_no_open_project))
            return
        }

        if (isBuilding) {
            return
        }

        isBuilding = true
        logger.plain(strings.get(R.string.msg_building))

        viewModelScope.launch {
            try {
                if (!editorSession.saveModifiedDocuments()) {
                    return@launch
                }

                val report =
                    withContext(ioDispatcher) {
                        buildPipeline(
                            activeProject,
                            File(appPreferences.globalLibrariesDirectory)
                        ).build()
                    }

                report.logEntries.forEach { entry ->
                    when (entry.severity) {
                        LogSeverity.PLAIN -> logger.plain(entry.text)
                        LogSeverity.INFO -> logger.info(entry.text)
                        LogSeverity.ERROR -> logger.error(entry.text)
                    }
                }

                refreshProjectTree()
                report.outputJar?.let { jar ->
                    onBuilt(BuiltMidlet(jar.path, activeProject.metadata.name))
                }
            } finally {
                isBuilding = false
            }
        }
    }

    /**
     * Packs an already built midlet into an installable apk next to its jar.
     *
     * The conversion is the same one the built in emulator does before running a midlet, so a
     * project that runs is a project that exports.
     */
    fun exportApk(
        builtMidlet: BuiltMidlet,
        converter: MidletConverter,
        onExported: (File) -> Unit
    ) {
        val activeProject = project

        if (activeProject == null || isExporting) {
            return
        }

        isExporting = true
        logger.plain(strings.get(R.string.msg_exporting_apk))

        viewModelScope.launch {
            try {
                val apk =
                    runCatching {
                        withContext(ioDispatcher) {
                            apkExporter.export(
                                exportRequest(
                                    activeProject,
                                    converter.convert(
                                        builtMidlet.jarPath,
                                        builtMidlet.projectName,
                                        activeProject.apkSettings.keyboardEnabled,
                                        activeProject.apkSettings.orientation
                                    )
                                )
                            )
                        }
                    }.onFailure(logger::error).getOrNull() ?: return@launch

                logger.info(
                    strings.get(R.string.msg_apk_exported) + "\n" +
                            "${ProjectLayout.BINARY_DIRECTORY}/${apk.name}\n" +
                            "${apk.length() / 1024} KB\n" +
                            activeProject.apkSettings.packageName
                )

                refreshProjectTree()
                onExported(apk)
            } finally {
                isExporting = false
            }
        }
    }

    private fun exportRequest(project: Project, midletFiles: Map<String, File>): ApkExportRequest {
        val settings = project.apkSettings
        val metadata = project.metadata

        return ApkExportRequest(
            application =
                ApkApplication(
                    packageName = settings.packageName,
                    label = settings.labelOr(metadata.name),
                    versionName = metadata.version,
                    // a project that never set one follows the MIDlet version, the way every
                    // export did before the field existed
                    versionCode = settings.versionCode ?: ApkVersions.codeOf(metadata.version)
                ),
            midletFiles = midletFiles,
            // the same file the midlet manifest points at, so the app and the midlet share it
            icon = project.resourcesDirectory.resolve(MIDLET_ICON).takeIf(File::isFile),
            iconBackground = settings.iconBackgroundColor,
            target = project.outputApk
        )
    }

    private suspend fun installAssetsIfNeeded() {
        val firstInstall = !appPreferences.assetsInstalled
        val needsUpdate =
            firstInstall || appPreferences.assetsVersion != AssetInstaller.ASSETS_VERSION

        if (!needsUpdate) {
            return
        }

        if (firstInstall) {
            logger.plain(strings.get(R.string.msg_install_start))
        }

        val result = withContext(ioDispatcher) { assetInstaller.install() }

        result.onSuccess {
            appPreferences.assetsInstalled = true
            appPreferences.assetsVersion = AssetInstaller.ASSETS_VERSION

            if (firstInstall) {
                logger.info(strings.get(R.string.msg_install_ok))
            }
        }.onFailure(logger::error)
    }

    private suspend fun restoreSession() {
        val request = projectOpenRequest
        editorSession.restoreRecentDocuments()

        val lastProject = editorPreferences.lastProject

        if (lastProject.isNotEmpty() && withContext(ioDispatcher) { File(lastProject).isFile }) {
            val restored =
                runCatching {
                    withContext(ioDispatcher) { projects.open(File(lastProject)) }
                }.onFailure(logger::error)
                    .getOrNull()

            if (restored != null && request == projectOpenRequest) {
                activateProject(restored)
                editorSession.open(restored.mainModule.path)
            }
        }
    }

    private fun activateProject(opened: Project) {
        project = opened
        projectTitle = opened.metadata.name
        editorPreferences.lastProject = opened.configFile.path
        expandedDirectories.clear()
        expandedDirectories.add(opened.sourcesDirectory.path)
        refreshProjectTree()
    }

    private fun refreshProjectTree() {
        val activeProject = project

        if (activeProject == null) {
            projectTree = emptyList()
            return
        }

        val request = ++treeRequest
        val expanded = expandedDirectories.toSet()

        viewModelScope.launch {
            val tree =
                withContext(ioDispatcher) {
                    buildProjectTree(activeProject, expanded)
                }

            if (request == treeRequest && project === activeProject) {
                projectTree = tree
            }
        }
    }

    private fun relocateExpandedDirectories(oldPath: String, newPath: String) {
        val relocated =
            expandedDirectories.map { path ->
                when {
                    path == oldPath -> newPath
                    path.startsWith("$oldPath/") -> newPath + path.removePrefix(oldPath)
                    else -> path
                }
            }

        expandedDirectories.clear()
        expandedDirectories.addAll(relocated)
    }

    private fun appendLogEntry(entry: LogEntry) {
        if (logEntries.size >= MAX_LOG_ENTRIES) {
            logEntries.removeAt(0)
        }

        logEntries.add(entry)

        if (entry.severity == LogSeverity.ERROR) {
            errorCount++
        }
    }

    private companion object {
        const val MAX_LOG_ENTRIES = 200
        const val MIDLET_ICON = "icon.png"

        // what the config dialog shows with no project open; it cannot be saved anywhere
        val DEFAULT_APK_SETTINGS =
            ApkSettings(
                packageName = "",
                label = "",
                versionCode = null,
                iconBackground = ApkSettings.DEFAULT_ICON_BACKGROUND,
                orientation = ApkOrientation.PORTRAIT,
                keyboardEnabled = false
            )
    }
}
