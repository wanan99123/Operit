package com.ai.assistance.operit.data.stats

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SubagentToolProgress(val id: String, val name: String, val status: String) {
    val isTerminal: Boolean
        get() = status in setOf("result", "error", "failed", "timed_out", "cancelled")
}
data class SubagentProgress(
    val agentId: String,
    val agentType: String,
    val description: String,
    val status: String = "running",
    val tools: List<SubagentToolProgress> = emptyList(),
)

/** Parent-session projection only. Never stores child reasoning or raw tool output. */
object SubagentProgressStore {
    private val state = MutableStateFlow<Map<String, List<SubagentProgress>>>(emptyMap())
    val sessions: StateFlow<Map<String, List<SubagentProgress>>> = state.asStateFlow()

    private val rounds = mutableMapOf<String, String>()

    /** One batch is one round. Start it before launching children, never from each batch child. */
    @Synchronized
    fun beginRound(chatId: String): String {
        require(chatId.isNotBlank()) { "A chat id is required" }
        val roundId = java.util.UUID.randomUUID().toString()
        rounds[chatId] = roundId
        state.value = state.value + (chatId to emptyList())
        return roundId
    }

    @Synchronized
    fun start(chatId: String, agent: SubagentProgress, roundId: String? = null) {
        // A superseded round can still finish cleanup; it must not repopulate the new panel.
        if (roundId != null && rounds[chatId] != roundId) return
        val old = state.value[chatId].orEmpty()
        state.value = state.value + (chatId to (old + agent))
    }

    @Synchronized
    fun tool(chatId: String, agentId: String, event: SubagentToolProgress) {
        val agents = state.value[chatId] ?: return
        state.value = state.value + (chatId to agents.map { agent ->
            if (agent.agentId != agentId || agent.status != "running") agent else {
                val existing = agent.tools.indexOfFirst { it.id == event.id }
                val tools = if (existing < 0) agent.tools + event else
                    agent.tools.map { if (it.id == event.id) event else it }
                agent.copy(tools = tools.takeLast(64))
            }
        })
    }

    @Synchronized
    fun finish(chatId: String, agentId: String, status: String) {
        require(status in setOf("completed", "failed", "timed_out", "cancelled"))
        val agents = state.value[chatId] ?: return
        state.value = state.value + (chatId to agents.map {
            if (it.agentId == agentId) it.copy(
                status = status,
                tools = it.tools.map { tool ->
                    if (tool.status == "scheduled" || tool.status == "started") {
                        tool.copy(status = if (status == "completed") "error" else status)
                    } else tool
                }
            ) else it
        })
    }

    @Synchronized
    fun clear(chatId: String) {
        rounds.remove(chatId)
        state.value = state.value - chatId
    }
}