package com.github.helltar.anpaside.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.helltar.anpaside.BuildConfig
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.core.AssetInstaller
import com.github.helltar.anpaside.core.IdeLog
import com.github.helltar.anpaside.core.LogLevel
import com.github.helltar.anpaside.core.LogMessage
import com.github.helltar.anpaside.core.Paths
import com.github.helltar.anpaside.core.Paths.EXT_PAS
import com.github.helltar.anpaside.core.copyInto
import com.github.helltar.anpaside.core.ensureDirectory
import com.github.helltar.anpaside.core.importContent
import com.github.helltar.anpaside.core.prefs.EditorPrefs
import com.github.helltar.anpaside.core.prefs.IdePrefs
import com.github.helltar.anpaside.core.project.Project
import com.github.helltar.anpaside.core.project.ProjectBuilder
import com.github.helltar.anpaside.core.project.Projects
import com.github.helltar.anpaside.core.project.TreeNode
import com.github.helltar.anpaside.core.project.buildProjectTree
import com.github.helltar.anpaside.core.project.exportProject
import com.github.helltar.anpaside.ui.editor.OpenFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

class IdeViewModel(application: Application) : AndroidViewModel(application) {

    private val editorPrefs = EditorPrefs(application)
    private val idePrefs = IdePrefs(application)

    // null when no project is open; every path the ide works with comes off it
    private var project: Project? = null

    val openFiles = mutableStateListOf<OpenFile>()
    val log = mutableStateListOf<LogMessage>()

    // files whose content is still being read, they have no tab yet
    private val openingPaths = mutableSetOf<String>()

    // directories the file drawer keeps open, by absolute path
    private val expandedDirs = mutableSetOf<String>()

    var currentFileIndex by mutableIntStateOf(-1)
        private set

    var projects by mutableStateOf(emptyList<String>())
        private set

    // midlet name of the open project, empty when nothing is open
    var openProjectName by mutableStateOf("")
        private set

    // files of the open project, flattened for the drawer
    var projectTree by mutableStateOf(emptyList<TreeNode>())
        private set

    var isBuilding by mutableStateOf(false)
        private set

    // bumped on every logged error, the editor opens the log panel on it
    var errorCount by mutableIntStateOf(0)
        private set

    var fontSize by pref(editorPrefs::fontSize) {
        it.coerceIn(EditorPrefs.MIN_FONT_SIZE, EditorPrefs.MAX_FONT_SIZE)
    }

    var highlighterEnabled by pref(editorPrefs::highlighterEnabled)
    var lineNumbersEnabled by pref(editorPrefs::lineNumbersEnabled)
    var wordWrapEnabled by pref(editorPrefs::wordWrapEnabled)
    var globalLibsDir by pref(idePrefs::globalLibsDir)
    var embeddedEmulatorEnabled by pref(idePrefs::embeddedEmulatorEnabled)
    var midletScreenWidth by pref(idePrefs::midletScreenWidth)
    var midletScreenHeight by pref(idePrefs::midletScreenHeight)
    var midletKeyboardEnabled by pref(idePrefs::midletKeyboardEnabled)

    val currentFile: OpenFile? get() = openFiles.getOrNull(currentFileIndex)
    val isProjectOpen: Boolean get() = openProjectName.isNotEmpty()
    val hasModifiedFiles: Boolean get() = openFiles.any { it.isModified }
    val projectsDir: String get() = Paths.projectsDir.path

    init {
        viewModelScope.launch {
            IdeLog.messages.collect { message ->
                if (log.size >= MAX_LOG_MESSAGES) {
                    log.removeAt(0)
                }

                log.add(message)

                if (message.level == LogLevel.ERROR) {
                    errorCount++
                }
            }
        }

        IdeLog.add(getStr(R.string.app_name) + " " + BuildConfig.VERSION_NAME)

        viewModelScope.launch {
            withContext(Dispatchers.IO) { installAssets() }
            restoreSession()
            refreshProjects()
        }
    }

    fun refreshProjects() {
        viewModelScope.launch {
            projects = withContext(Dispatchers.IO) { Projects.names() }
        }
    }

    fun openProject(name: String) = openProjectFile(Projects.configFile(name))

    // the midlet name is editable, so a project is identified by its directory
    fun isOpenProject(name: String) = project?.dir == Projects.dir(name)

    // line is 1-based, 0 leaves the caret where it was
    fun openFile(path: String, line: Int = 0) {
        val index = openFiles.indexOfFirst { it.path == path }

        if (index >= 0) {
            currentFileIndex = index

            if (line > 0) {
                openFiles[index].moveCaretToLine(line)
            }

            return
        }

        // reading is async, so a second request for the same file would open a duplicate tab
        if (!openingPaths.add(path)) {
            return
        }

        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    try {
                        File(path).readText()
                    } catch (e: IOException) {
                        IdeLog.error(e)
                        null
                    }
                } ?: return@launch

                val file = OpenFile(path, text)

                openFiles.add(file)
                currentFileIndex = openFiles.lastIndex
                saveRecentFiles()

                if (line > 0) {
                    file.moveCaretToLine(line)
                }
            } finally {
                openingPaths.remove(path)
            }
        }
    }

    fun selectFile(index: Int) {
        if (index in openFiles.indices) {
            currentFileIndex = index
        }
    }

    fun closeFile(index: Int) {
        val file = openFiles.getOrNull(index) ?: return

        if (file.isModified) {
            file.save()
        }

        openFiles.removeAt(index)
        currentFileIndex = currentFileIndex.coerceAtMost(openFiles.lastIndex)
        saveRecentFiles()
    }

    fun saveAll(): Boolean {
        if (openFiles.isEmpty()) {
            return false
        }

        return openFiles.map { it.save() }.all { it }
    }

    // the compiler reports the source file by its bare name, units live in src/
    fun openCompilerError(name: String, line: Int) {
        val file = project?.srcDir?.resolve(name) ?: return

        if (file.exists()) {
            openFile(file.path, line)
        }
    }

    fun refreshProjectTree() {
        val root = project?.dir

        if (root == null) {
            projectTree = emptyList()
            return
        }

        viewModelScope.launch {
            projectTree = withContext(Dispatchers.IO) { buildProjectTree(root, expandedDirs) }
        }
    }

    fun toggleDirectory(path: String) {
        if (!expandedDirs.add(path)) {
            expandedDirs.remove(path)
        }

        refreshProjectTree()
    }

    fun deleteProjectFile(path: String) {
        if (!File(path).deleteRecursively()) {
            IdeLog.error(getStr(R.string.err_delete_file) + ": " + path)
            return
        }

        closeTabsUnder(path)
        saveRecentFiles()
        refreshProjectTree()
    }

    fun renameProjectFile(path: String, newName: String): Boolean {
        if (newName.isBlank()) {
            return false
        }

        val source = File(path)
        val target = File(source.parentFile, newName)

        if (target.exists()) {
            IdeLog.error(getStr(R.string.err_file_exists) + ": " + newName)
            return false
        }

        if (!source.renameTo(target)) {
            IdeLog.error(getStr(R.string.err_rename_file) + ": " + path)
            return false
        }

        // a tab is identified by its path: the renamed file reopens under the new name,
        // files inside a renamed directory are just closed
        val wasOpen = openFiles.any { it.path == path }
        closeTabsUnder(path)

        if (wasOpen) {
            openFile(target.path)
        } else {
            saveRecentFiles()
        }

        refreshProjectTree()

        return true
    }

    fun importFileTo(uri: Uri, destDir: String) {
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) {
                try {
                    importContent(getApplication<Application>().contentResolver, uri, File(destDir))
                } catch (e: IOException) {
                    IdeLog.error(e)
                    null
                }
            } ?: return@launch

            IdeLog.info(getStr(R.string.msg_imported) + ": " + imported.name)
            refreshProjectTree()
        }
    }

    fun exportProjectZip(name: String, destDir: File, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val archive = withContext(Dispatchers.IO) {
                try {
                    // a throwaway Project so any listed project can be exported, not only the open one
                    exportProject(Project.open(Projects.configFile(name)), destDir)
                } catch (e: Exception) {
                    IdeLog.error(getStr(R.string.err_failed_create_archive) + ": " + name + " (" + e.message + ")")
                    null
                }
            } ?: return@launch

            onReady(archive.path)
        }
    }

    fun clearLog() = log.clear()

    fun projectConfig(): ProjectConfig =
        project?.let { ProjectConfig(it.midletName, it.midletVendor, it.midletVersion) }
            ?: ProjectConfig("", "", "")

    fun saveProjectConfig(config: ProjectConfig) {
        val project = project ?: return

        project.midletName = config.name
        project.midletVendor = config.vendor
        project.midletVersion = config.version

        try {
            project.save()
            openProjectName = project.midletName
        } catch (e: IOException) {
            IdeLog.error(e)
        }
    }

    fun createProject(name: String, overwrite: Boolean = false): CreateResult {
        if (name.length < MIN_NAME_LENGTH) {
            return CreateResult.NAME_TOO_SHORT
        }

        val dir = Projects.dir(name)

        if (dir.exists()) {
            if (!overwrite) {
                return CreateResult.ALREADY_EXISTS
            }

            dir.deleteRecursively()
        }

        return try {
            val created = createProjectFiles(name, dir)
            openProjectFile(created.configFile)
            refreshProjects()
            CreateResult.CREATED
        } catch (e: Exception) {
            IdeLog.error(e)
            CreateResult.FAILED
        }
    }

    fun createModule(name: String, overwrite: Boolean = false): CreateResult {
        if (name.length < MIN_NAME_LENGTH) {
            return CreateResult.NAME_TOO_SHORT
        }

        val project = project ?: return CreateResult.FAILED
        val file = project.srcDir.resolve(name + EXT_PAS)

        if (file.exists()) {
            if (!overwrite) {
                return CreateResult.ALREADY_EXISTS
            }

            if (!file.delete()) {
                IdeLog.error(getStr(R.string.err_del_old_module) + ": " + file.path)
                return CreateResult.FAILED
            }
        }

        return try {
            file.writeText(getStr(R.string.tpl_module).format(file.nameWithoutExtension))
            openFile(file.path)
            refreshProjectTree()
            CreateResult.CREATED
        } catch (e: IOException) {
            IdeLog.error(e)
            CreateResult.FAILED
        }
    }

    fun deleteProject(name: String) {
        val dir = Projects.dir(name)

        if (!dir.deleteRecursively()) {
            IdeLog.error(getStr(R.string.err_del_project) + ": " + name)
            return
        }

        val prefix = dir.path + File.separator
        openFiles.removeAll { it.path.startsWith(prefix) }
        currentFileIndex = currentFileIndex.coerceAtMost(openFiles.lastIndex)
        saveRecentFiles()

        if (editorPrefs.lastProject.startsWith(prefix)) {
            editorPrefs.lastProject = ""
        }

        if (project?.dir == dir) {
            project = null
            openProjectName = ""
            refreshProjectTree()
        }

        refreshProjects()
    }

    fun buildAndRun(onJarReady: (String) -> Unit) {
        val project = project

        if (project == null) {
            IdeLog.error(getStr(R.string.msg_no_open_project))
            return
        }

        if (isBuilding) {
            return
        }

        saveAll()
        isBuilding = true
        IdeLog.add(getStr(R.string.msg_building))

        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                ProjectBuilder(
                    getApplication<Application>(),
                    project,
                    Paths.compilerBinary,
                    Paths.rtlDir,
                    File(idePrefs.globalLibsDir)
                ).build()
            }

            isBuilding = false
            refreshProjectTree()

            if (success) {
                onJarReady(project.jarFile.path)
            }
        }
    }

    // creates the directory tree and the initial source files of a brand-new project
    private fun createProjectFiles(name: String, dir: File): Project {
        val project = Project.create(dir.resolve(name + Paths.EXT_PROJ), name)

        project.createDirectories()
        project.save()
        project.mainModuleFile.writeText(getStr(R.string.tpl_helloworld).format(project.mainModuleName))
        dir.resolve(".gitignore").writeText(getStr(R.string.tpl_gitignore))
        Paths.templateIcon.copyInto(project.resDir)
        Paths.globalLibsDir.ensureDirectory()

        return project
    }

    private fun openProjectFile(configFile: File) {
        val opened = try {
            Project.open(configFile)
        } catch (e: Exception) {
            IdeLog.error(e)
            return
        }

        project = opened
        editorPrefs.lastProject = configFile.path
        openProjectName = opened.midletName

        // sources are what the drawer is opened for, the other directories start folded
        expandedDirs.clear()
        expandedDirs.add(opened.srcDir.path)
        refreshProjectTree()

        openFile(opened.mainModuleFile.path)
    }

    private suspend fun restoreSession() {
        val recentFiles = withContext(Dispatchers.IO) {
            editorPrefs.recentFiles.distinct().filter { File(it).exists() }
        }

        recentFiles.forEach { openFile(it) }

        val lastProject = editorPrefs.lastProject

        if (lastProject.isNotEmpty() && File(lastProject).exists()) {
            openProjectFile(File(lastProject))
        }
    }

    private fun installAssets() {
        val installer = AssetInstaller(getApplication<Application>().assets)

        if (!idePrefs.assetsInstalled) {
            IdeLog.add(getStr(R.string.msg_install_start))

            if (installer.install()) {
                idePrefs.assetsInstalled = true
                idePrefs.assetsVersion = AssetInstaller.ASSETS_VERSION
                IdeLog.info(getStr(R.string.msg_install_ok))
            }

            return
        }

        // bundled assets changed since the last install, refresh the installed copies
        if (idePrefs.assetsVersion != AssetInstaller.ASSETS_VERSION && installer.install()) {
            idePrefs.assetsVersion = AssetInstaller.ASSETS_VERSION
        }
    }

    // a deleted or renamed directory takes the tabs of every file beneath it
    private fun closeTabsUnder(path: String) {
        openFiles.removeAll { it.path == path || it.path.startsWith("$path/") }
        currentFileIndex = currentFileIndex.coerceAtMost(openFiles.lastIndex)
    }

    private fun saveRecentFiles() {
        editorPrefs.recentFiles = openFiles.map { it.path }
    }

    private fun getStr(resId: Int) = getApplication<Application>().getString(resId)

    // mirrors a persisted preference as compose state: reads recompose, writes persist
    private fun <T> pref(backing: KMutableProperty0<T>, sanitize: (T) -> T = { it }) =
        object : ReadWriteProperty<Any?, T> {
            private val state = mutableStateOf(backing.get())

            override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                val clean = sanitize(value)
                state.value = clean
                backing.set(clean)
            }
        }

    companion object {
        const val MIN_NAME_LENGTH = 3
        private const val MAX_LOG_MESSAGES = 200
    }
}
