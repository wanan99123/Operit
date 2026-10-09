package com.ai.assistance.operit.data.stats

import com.ai.assistance.operit.data.model.PlanStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live, process-local plans. Each UI derives its own selection, including floating windows. */
object PlanStepStore {
    private val state = MutableStateFlow<Map<String, List<PlanStep>>>(emptyMap())
    val plans: StateFlow<Map<String, List<PlanStep>>> = state.asStateFlow()

    @Synchronized
    fun update(key: String, steps: List<PlanStep>): List<PlanStep> {
        require(key.isNotBlank()) { "A chat id is required" }
        val previous = state.value[key].orEmpty()
        state.value = if (steps.isEmpty()) state.value - key else state.value + (key to steps.toList())
        return previous
    }

    fun read(key: String): List<PlanStep> = state.value[key].orEmpty().toList()

    fun clear(key: String) {
        update(key, emptyList())
    }
}