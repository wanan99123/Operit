package com.ai.assistance.operit.data.preferences

import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.PlanModelStage

/** Only generation/review use the planner; implementation keeps the selected chat model. */
internal object SelectedRequestModel {
    fun resolve(
        functionType: FunctionType,
        mappings: Map<FunctionType, FunctionConfigMapping>,
        configIdOverride: String?,
        modelIndexOverride: Int?,
        isSubTask: Boolean,
        stage: PlanModelStage? = null,
    ): FunctionConfigMapping {
        if (isSubTask) return mappings[FunctionType.SUBAGENT] ?: FunctionConfigMapping()
        if (functionType == FunctionType.CHAT &&
            (stage == PlanModelStage.GENERATION || stage == PlanModelStage.REVIEW)) {
            return mappings[FunctionType.PLAN_GENERATION] ?: FunctionConfigMapping()
        }
        if (functionType == FunctionType.CHAT && !configIdOverride.isNullOrBlank()) {
            return FunctionConfigMapping(configIdOverride, (modelIndexOverride ?: 0).coerceAtLeast(0))
        }
        return mappings[functionType] ?: FunctionConfigMapping()
    }
}