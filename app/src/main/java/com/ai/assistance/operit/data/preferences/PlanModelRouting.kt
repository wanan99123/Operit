package com.ai.assistance.operit.data.preferences

import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.PlanModelStage

/** Each stage has its own binding. Unset stages use the existing default config, never a sibling. */
internal object PlanModelRouting {
    fun resolve(
        stage: PlanModelStage,
        mappings: Map<FunctionType, FunctionConfigMapping>,
    ): FunctionConfigMapping = mappings[stage.functionType] ?: FunctionConfigMapping()
}
