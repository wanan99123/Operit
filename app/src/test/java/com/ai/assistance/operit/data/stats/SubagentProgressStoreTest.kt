package com.ai.assistance.operit.data.stats

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubagentProgressStoreTest {
    @Test
    fun isolatesSessionsAndUpdatesToolLifecycle() {
        val firstChat = UUID.randomUUID().toString()
        val secondChat = UUID.randomUUID().toString()
        val agent = SubagentProgress("agent-1", "Explore", "Inspect files")
        try {
            SubagentProgressStore.start(firstChat, agent)
            SubagentProgressStore.start(secondChat, agent.copy(agentId = "agent-2"))
            SubagentProgressStore.tool(
                firstChat,
                "agent-1",
                SubagentToolProgress("tool-1", "read_file", "started")
            )
            SubagentProgressStore.tool(
                firstChat,
                "agent-1",
                SubagentToolProgress("tool-1", "read_file", "result")
            )
            SubagentProgressStore.finish(firstChat, "agent-1", "completed")

            val first = SubagentProgressStore.sessions.value.getValue(firstChat).single()
            val second = SubagentProgressStore.sessions.value.getValue(secondChat).single()
            assertEquals("completed", first.status)
            assertEquals(listOf(SubagentToolProgress("tool-1", "read_file", "result")), first.tools)
            assertEquals("running", second.status)
            assertTrue(second.tools.isEmpty())
        } finally {
            SubagentProgressStore.clear(firstChat)
            SubagentProgressStore.clear(secondChat)
        }
    }

    @Test fun cancellationFinishesToolsAndLateEventsDoNotRestoreClearedSession() {
        val chatId = UUID.randomUUID().toString()
        try {
            SubagentProgressStore.start(chatId, SubagentProgress("agent", "Explore", "Inspect"))
            SubagentProgressStore.tool(chatId, "agent", SubagentToolProgress("tool", "read_file", "started"))
            SubagentProgressStore.finish(chatId, "agent", "cancelled")
            val agent = SubagentProgressStore.sessions.value.getValue(chatId).single()
            assertEquals("cancelled", agent.status)
            assertEquals("cancelled", agent.tools.single().status)
            SubagentProgressStore.clear(chatId)
            SubagentProgressStore.tool(chatId, "agent", SubagentToolProgress("tool", "read_file", "result"))
            SubagentProgressStore.finish(chatId, "agent", "completed")
            assertTrue(chatId !in SubagentProgressStore.sessions.value)
        } finally {
            SubagentProgressStore.clear(chatId)
        }
    }

    @Test
    fun finishClosesPendingToolsAndIgnoresLateToolEvents() {
        for (status in listOf("completed", "failed", "timed_out", "cancelled")) {
            val chatId = UUID.randomUUID().toString()
            try {
                SubagentProgressStore.start(chatId, SubagentProgress("agent", "Explore", "Inspect"))
                SubagentProgressStore.tool(chatId, "agent", SubagentToolProgress("tool", "read_file", "scheduled"))
                SubagentProgressStore.finish(chatId, "agent", status)
                val finished = SubagentProgressStore.sessions.value.getValue(chatId).single()
                assertEquals(status, finished.status)
                assertTrue(finished.tools.single().isTerminal)
                assertEquals(if (status == "completed") "error" else status, finished.tools.single().status)
                SubagentProgressStore.tool(chatId, "agent", SubagentToolProgress("tool", "read_file", "started"))
                assertEquals(finished, SubagentProgressStore.sessions.value.getValue(chatId).single())
            } finally {
                SubagentProgressStore.clear(chatId)
            }
        }
        assertTrue(!SubagentToolProgress("tool", "read_file", "started").isTerminal)
        assertTrue(!SubagentToolProgress("tool", "read_file", "scheduled").isTerminal)
    }

    @Test
    fun boundsToolHistory() {
        val chatId = UUID.randomUUID().toString()
        try {
            SubagentProgressStore.start(chatId, SubagentProgress("agent", "general-purpose", "Test"))
            repeat(70) { index ->
                SubagentProgressStore.tool(
                    chatId,
                    "agent",
                    SubagentToolProgress("tool-$index", "read_file", "result")
                )
            }
            assertEquals(64, SubagentProgressStore.sessions.value.getValue(chatId).single().tools.size)
            assertEquals("tool-69", SubagentProgressStore.sessions.value.getValue(chatId).single().tools.last().id)
        } finally {
            SubagentProgressStore.clear(chatId)
        }
    }
}
