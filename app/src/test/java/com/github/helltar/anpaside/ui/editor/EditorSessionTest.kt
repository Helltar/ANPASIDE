package com.github.helltar.anpaside.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import com.github.helltar.anpaside.foundation.IdeLogger
import com.github.helltar.anpaside.foundation.TextFileStore
import com.github.helltar.anpaside.preferences.EditorSessionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EditorSessionTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun savesDocumentsAndPersistsTheirOrder() = runBlocking {
        val first = temporaryFolder.newFile("first.pas").apply { writeText("one") }
        val second = temporaryFolder.newFile("second.pas").apply { writeText("two") }
        val preferences = FakePreferences()
        val session = session(preferences)

        session.open(first.path)
        session.open(second.path)
        session.documents[0].onValueChange(TextFieldValue("changed"))

        assertTrue(session.saveAll())
        assertEquals("changed", first.readText())
        assertFalse(session.documents[0].isModified)
        assertEquals(listOf(first.path, second.path), preferences.recentFiles)
    }

    @Test
    fun closingTabBeforeSelectionKeepsSameDocumentSelected() = runBlocking {
        val first = temporaryFolder.newFile("first.pas")
        val second = temporaryFolder.newFile("second.pas")
        val third = temporaryFolder.newFile("third.pas")
        val session = session(FakePreferences())

        session.open(first.path)
        session.open(second.path)
        session.open(third.path)
        assertEquals(third.path, session.selectedDocument?.path)

        session.close(0)

        assertEquals(third.path, session.selectedDocument?.path)
        assertEquals(1, session.selectedIndex)
    }

    @Test
    fun relocateUpdatesEveryOpenDescendant() = runBlocking {
        val root = temporaryFolder.newFolder("project")
        val source = root.resolve("src").apply { mkdir() }
        val first = source.resolve("first.pas").apply { writeText("") }
        val nestedDirectory = source.resolve("nested").apply { mkdir() }
        val second = nestedDirectory.resolve("second.pas").apply { writeText("") }
        val session = session(FakePreferences())

        session.open(first.path)
        session.open(second.path)
        session.relocate(source.path, root.resolve("source").path)

        assertEquals(
            listOf(
                root.resolve("source/first.pas").path,
                root.resolve("source/nested/second.pas").path
            ),
            session.documents.map(EditorDocument::path)
        )
    }

    @Test
    fun restoresFoldedBlocksAfterSessionRestart() = runBlocking {
        val source = "begin\n    value := 1;\nend."
        val file = temporaryFolder.newFile("main.pas").apply { writeText(source) }
        val preferences = FakePreferences()
        val firstSession = session(preferences)
        firstSession.open(file.path)
        val block = PascalFolding.findBlocks(source).single()

        firstSession.selectedDocument?.toggleFold(block)

        assertEquals(setOf(block.startOffset), preferences.foldedBlockOffsets[file.path])

        val restoredSession = session(preferences)
        restoredSession.restoreRecentDocuments()

        assertEquals(setOf(block.startOffset), restoredSession.selectedDocument?.collapsedFoldStarts)
    }

    @Test
    fun closeRemovesPersistedFoldState() = runBlocking {
        val source = "begin\n    value := 1;\nend."
        val file = temporaryFolder.newFile("main.pas").apply { writeText(source) }
        val preferences = FakePreferences()
        val session = session(preferences)
        session.open(file.path)
        session.selectedDocument?.toggleFold(PascalFolding.findBlocks(source).single())

        session.close(0)

        assertTrue(preferences.foldedBlockOffsets.isEmpty())
    }

    @Test
    fun relocateMovesPersistedFoldState() = runBlocking {
        val source = "begin\n    value := 1;\nend."
        val file = temporaryFolder.newFile("main.pas").apply { writeText(source) }
        val preferences = FakePreferences()
        val session = session(preferences)
        session.open(file.path)
        val block = PascalFolding.findBlocks(source).single()
        session.selectedDocument?.toggleFold(block)
        val target = temporaryFolder.root.resolve("renamed.pas")

        session.relocate(file.path, target.path)

        assertEquals(setOf(block.startOffset), preferences.foldedBlockOffsets[target.path])
        assertFalse(preferences.foldedBlockOffsets.containsKey(file.path))
    }

    private fun session(preferences: FakePreferences) =
        EditorSession(
            files = TextFileStore(),
            preferences = preferences,
            logger = IdeLogger(),
            ioDispatcher = Dispatchers.Unconfined
        )

    private class FakePreferences : EditorSessionPreferences {
        override var recentFiles: List<String> = emptyList()
        override var foldedBlockOffsets: Map<String, Set<Int>> = emptyMap()
        override var lastProject: String = ""
    }
}
