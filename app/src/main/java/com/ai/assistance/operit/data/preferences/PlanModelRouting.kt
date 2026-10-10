package com.ai.assistance.operit.data.preferences

import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.PlanModelStage

/** Generation and review share the planner binding. Unset bindings use the existing default config. */
internal object PlanModelRouting {
    fun resolve(
        stage: PlanModelStage,
        mappings: Map<FunctionType, FunctionConfigMapping>,
    ): FunctionConfigMapping = mappings[stage.functionType] ?: FunctionConfigMapping()
}
