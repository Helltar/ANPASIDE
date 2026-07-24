package com.github.helltar.anpaside.ui.about

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.helltar.anpaside.assets.LicenseDocuments
import com.github.helltar.anpaside.assets.LicenseRepository
import com.github.helltar.anpaside.foundation.IdeLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LicensesUiState(
    val documents: LicenseDocuments? = null,
    val isLoading: Boolean = true,
    val failed: Boolean = false
)

class LicensesViewModel(
    private val repository: LicenseRepository,
    private val logger: IdeLogger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    var state by mutableStateOf(LicensesUiState())
        private set

    init {
        viewModelScope.launch {
            val documents =
                runCatching {
                    withContext(ioDispatcher) { repository.load() }
                }.onFailure(logger::error)
                    .getOrNull()

            state =
                LicensesUiState(
                    documents = documents,
                    isLoading = false,
                    failed = documents == null
                )
        }
    }
}
