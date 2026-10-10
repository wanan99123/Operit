package com.ai.assistance.operit.data.preferences

import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.PlanModelStage

/** Re-evaluate at every model hop, not just at the initial composer send. */
internal object AutomaticPlanModelRouting {
    fun resolveFunction(stage: PlanModelStage?): FunctionType = stage?.functionType ?: FunctionType.CHAT

    fun resolve(
        stage: PlanModelStage?,
        mappings: Map<FunctionType, FunctionConfigMapping>,
        ordinaryTarget: FunctionConfigMapping,
        isSubTask: Boolean,
    ): FunctionConfigMapping = if (isSubTask || stage == null) ordinaryTarget
        else PlanModelRouting.resolve(stage, mappings)
}
