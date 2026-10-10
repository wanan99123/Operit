package com.ai.assistance.operit.data.stats

import com.ai.assistance.operit.data.model.PlanModelStage
import com.ai.assistance.operit.data.model.PlanStep
import com.ai.assistance.operit.data.model.PlanStepStatus
import java.util.concurrent.ConcurrentHashMap

/** Session-scoped phase; never share a parent's phase with a delegated execution. */
object PlanModelStageStore {
    private val stages = ConcurrentHashMap<String, PlanModelStage>()
    fun read(chatId: String): PlanModelStage? = stages[chatId]
    fun update(chatId: String, stage: PlanModelStage?) {
        require(chatId.isNotBlank()) { "A chat id is required" }
        if (stage == null) stages.remove(chatId) else stages[chatId] = stage
    }
    fun clear(chatId: String) { stages.remove(chatId) }

    fun resolve(rawStage: String?, steps: List<PlanStep>): PlanModelStage? {
        if (rawStage != null) return when (rawStage) {
            "generation" -> PlanModelStage.GENERATION
            "implementation" -> PlanModelStage.IMPLEMENTATION
            "review" -> PlanModelStage.REVIEW
            "chat" -> null
            else -> throw IllegalArgumentException("model_stage must be generation, implementation, review or chat")
        }
        return when {
            steps.isEmpty() -> null
            steps.all { it.status == PlanStepStatus.COMPLETED } -> PlanModelStage.REVIEW
            steps.any { it.status == PlanStepStatus.IN_PROGRESS } -> PlanModelStage.IMPLEMENTATION
            else -> PlanModelStage.GENERATION
        }
    }
}
