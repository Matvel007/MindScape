package com.mindscape.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeStateManagerTest {
    @Test
    fun keepsFileNodeIdsPrefixedDuringMigration() {
        val file = LocalFileLink(
            "window.xml",
            "1",
            "content://downloads/public_downloads/1",
            "text/xml",
            1024,
            42
        )
        val note = Note("Заметка", "Текст", "1")
        val connections = mutableListOf(Connection("folder:${file.nodeId()}", "note:${note.fullPath()}"))

        NodeStateManager.migrateLegacyConnectionsAndHiddenNodes(
            connections,
            mutableSetOf(),
            listOf(note),
            emptyList()
        )

        assertEquals(file.nodeId(), connections[0].source)
        assertEquals("note:${note.fullPath()}", connections[0].target)
        assertTrue(NodeStateManager.isNodeLinked(file, connections))
    }
}
