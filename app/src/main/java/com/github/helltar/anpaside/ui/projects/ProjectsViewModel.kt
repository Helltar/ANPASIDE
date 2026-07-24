package com.github.helltar.anpaside.ui.projects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.helltar.anpaside.R
import com.github.helltar.anpaside.foundation.IdeLogger
import com.github.helltar.anpaside.foundation.StringResources
import com.github.helltar.anpaside.project.CreationResult
import com.github.helltar.anpaside.project.ProjectNames
import com.github.helltar.anpaside.project.ProjectRepository
import com.github.helltar.anpaside.project.ProjectTemplates
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProjectsUiState(
    val names: List<String> = emptyList(),
    val isLoading: Boolean = true
)

class ProjectsViewModel(
    private val projects: ProjectRepository,
    private val templates: ProjectTemplates,
    private val strings: StringResources,
    private val logger: IdeLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var refreshRequest = 0

    var state by mutableStateOf(ProjectsUiState())
        private set

    val projectsDirectory: String
        get() = projects.projectsDirectory.path

    init {
        refresh()
    }

    fun refresh() {
        val request = ++refreshRequest

        viewModelScope.launch {
            state = state.copy(isLoading = true)

            val names =
                runCatching {
                    withContext(ioDispatcher) { projects.listNames() }
                }.onFailure(logger::error)
                    .getOrDefault(emptyList())

            if (request == refreshRequest) {
                state = ProjectsUiState(names = names, isLoading = false)
            }
        }
    }

    fun create(
        name: String,
        overwrite: Boolean = false,
        onResult: (CreationResult) -> Unit
    ) {
        if (name.length < ProjectNames.MIN_LENGTH) {
            onResult(CreationResult.NAME_TOO_SHORT)
            return
        }

        if (!ProjectNames.isValidProjectName(name)) {
            onResult(CreationResult.INVALID_NAME)
            return
        }

        viewModelScope.launch {
            if (withContext(ioDispatcher) { projects.exists(name) } && !overwrite) {
                onResult(CreationResult.ALREADY_EXISTS)
                return@launch
            }

            val created =
                runCatching {
                    withContext(ioDispatcher) {
                        projects.create(name, templates, overwrite)
                    }
                }.onFailure(logger::error)
                    .isSuccess

            if (created) {
                refresh()
                onResult(CreationResult.CREATED)
            } else {
                onResult(CreationResult.FAILED)
            }
        }
    }

    fun delete(name: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val deleted =
                runCatching {
                    withContext(ioDispatcher) { projects.delete(name) }
                }.onFailure {
                    logger.error(strings.get(R.string.err_del_project) + ": " + name)
                }.isSuccess

            if (deleted) {
                refresh()
            }

            onComplete(deleted)
        }
    }

    fun export(name: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val archive =
                runCatching {
                    withContext(ioDispatcher) { projects.export(name) }
                }.onFailure { error ->
                    logger.error(
                        strings.get(R.string.err_failed_create_archive) +
                                ": " +
                                name +
                                " (" +
                                error.message +
                                ")"
                    )
                }.getOrNull()
                    ?: return@launch

            onReady(archive.path)
        }
    }
}
