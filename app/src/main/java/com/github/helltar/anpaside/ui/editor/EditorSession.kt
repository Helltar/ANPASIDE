package com.github.helltar.anpaside.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.github.helltar.anpaside.foundation.IdeLogger
import com.github.helltar.anpaside.foundation.TextFileStore
import com.github.helltar.anpaside.preferences.EditorSessionPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Owns the open editor documents and their persisted session.
 *
 * Disk work is suspendable, while every Compose state mutation happens after control returns to
 * the caller's main dispatcher.
 */
class EditorSession(
    private val files: TextFileStore,
    private val preferences: EditorSessionPreferences,
    private val logger: IdeLogger,
    private val ioDispatcher: CoroutineDispatcher
) {

    private val openingPaths = mutableSetOf<String>()

    val documents = mutableStateListOf<EditorDocument>()

    var selectedIndex by mutableIntStateOf(-1)
        private set

    val selectedDocument: EditorDocument?
        get() = documents.getOrNull(selectedIndex)

    val hasModifiedDocuments: Boolean
        get() = documents.any(EditorDocument::isModified)

    suspend fun restoreRecentDocuments() {
        val paths =
            withContext(ioDispatcher) {
                preferences.recentFiles.distinct().filter { File(it).isFile }
            }

        paths.forEach { open(it) }
    }

    suspend fun open(path: String, line: Int = 0): Boolean {
        val existingIndex = documents.indexOfFirst { it.path == path }

        if (existingIndex >= 0) {
            selectedIndex = existingIndex
            documents[existingIndex].moveCaretToLineIfRequested(line)
            return true
        }

        if (!openingPaths.add(path)) {
            return false
        }

        val text =
            try {
                runCatching {
                    withContext(ioDispatcher) { files.read(File(path)) }
                }.onFailure(logger::error)
                    .getOrNull()
                    ?: return false
            } finally {
                openingPaths.remove(path)
            }

        val document = EditorDocument(path, text)
        documents.add(document)
        selectedIndex = documents.lastIndex
        persist()
        document.moveCaretToLineIfRequested(line)
        return true
    }

    fun select(index: Int) {
        if (index in documents.indices) {
            selectedIndex = index
        }
    }

    suspend fun close(index: Int): Boolean {
        val document = documents.getOrNull(index) ?: return false

        if (document.isModified) {
            val snapshot = document.snapshot()

            if (!saveSnapshots(listOf(snapshot))) {
                return false
            }

            document.markSaved(snapshot)

            // edits made while the write was in progress must not be discarded
            if (document.isModified) {
                return false
            }
        }

        remove(document)
        return true
    }

    suspend fun saveAll(): Boolean {
        if (documents.isEmpty()) {
            return false
        }

        return saveModifiedDocuments()
    }

    suspend fun saveModifiedDocuments(): Boolean {
        repeat(MAX_SAVE_ATTEMPTS) {
            val snapshots = modifiedSnapshots()

            if (snapshots.isEmpty()) {
                return true
            }

            if (!saveSnapshots(snapshots)) {
                return false
            }

            markSaved(snapshots)
        }

        return !hasModifiedDocuments
    }

    fun modifiedSnapshots(): List<DocumentSnapshot> =
        documents.filter(EditorDocument::isModified).map(EditorDocument::snapshot)

    fun modifiedSnapshotsUnder(path: String): List<DocumentSnapshot> =
        documents
            .filter { it.path == path || it.path.startsWith("$path/") }
            .filter(EditorDocument::isModified)
            .map(EditorDocument::snapshot)

    suspend fun saveSnapshots(snapshots: List<DocumentSnapshot>): Boolean {
        if (snapshots.isEmpty()) {
            return true
        }

        return runCatching {
            withContext(ioDispatcher) {
                snapshots.forEach { snapshot ->
                    files.write(File(snapshot.path), snapshot.text)
                }
            }
        }.onFailure(logger::error)
            .isSuccess
    }

    fun markSaved(snapshots: List<DocumentSnapshot>) {
        snapshots.forEach { snapshot ->
            documents.firstOrNull { it.path == snapshot.path }?.markSaved(snapshot)
        }
    }

    fun closeUnder(path: String) {
        val removedIndices =
            documents.indices.filter { index ->
                documents[index].path == path || documents[index].path.startsWith("$path/")
            }

        removedIndices.asReversed().forEach { index ->
            documents.removeAt(index)

            if (index < selectedIndex) {
                selectedIndex--
            }
        }

        selectedIndex = selectedIndex.coerceAtMost(documents.lastIndex)
        persist()
    }

    fun relocate(oldPath: String, newPath: String) {
        documents.forEach { document ->
            when {
                document.path == oldPath -> document.relocate(newPath)
                document.path.startsWith("$oldPath/") ->
                    document.relocate(newPath + document.path.removePrefix(oldPath))
            }
        }

        persist()
    }

    private fun remove(document: EditorDocument) {
        val index = documents.indexOf(document)

        if (index < 0) {
            return
        }

        documents.removeAt(index)
        selectedIndex =
            when {
                documents.isEmpty() -> -1
                index < selectedIndex -> selectedIndex - 1
                index == selectedIndex -> index.coerceAtMost(documents.lastIndex)
                else -> selectedIndex
            }

        persist()
    }

    private fun persist() {
        preferences.recentFiles = documents.map(EditorDocument::path)
    }

    private fun EditorDocument.moveCaretToLineIfRequested(line: Int) {
        if (line > 0) {
            moveCaretToLine(line)
        }
    }

    private companion object {
        const val MAX_SAVE_ATTEMPTS = 3
    }
}
