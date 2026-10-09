package com.ai.assistance.operit.core.tools.defaultTool.standard

import org.junit.Assert.*
import org.junit.Test

class SubagentRequestTest {
    private fun tool(vararg parameters: Pair<String, String>) =
        parameters.toList()

    @Test fun defaultsAreFiniteAndTaskIsRequired() {
        val request = SubagentRequest.parse(tool("task" to " Inspect one file "))
        assertEquals("Inspect one file", request.task)
        assertEquals(8, request.maxToolCalls)
        assertEquals(180, request.timeoutSeconds)
    }

    @Test fun acceptsBoundaries() {
        val request = SubagentRequest.parse(tool("task" to "test", "max_tool_calls" to "32", "timeout_seconds" to "600"))
        assertEquals(32, request.maxToolCalls)
        assertEquals(600, request.timeoutSeconds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankTask() { SubagentRequest.parse(tool("task" to " ")) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateFields() { SubagentRequest.parse(tool("task" to "one", "task" to "two")) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsRoleIdentitySuppliedByModel() { SubagentRequest.parse(tool("task" to "test", "role_card_id" to "other")) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedTask() { SubagentRequest.parse(tool("task" to "x".repeat(16_001))) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDecimalLimit() { SubagentRequest.parse(tool("task" to "test", "max_tool_calls" to "1.5")) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnlimitedRuntime() { SubagentRequest.parse(tool("task" to "test", "timeout_seconds" to "0")) }
}