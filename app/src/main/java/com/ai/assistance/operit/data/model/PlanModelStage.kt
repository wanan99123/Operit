package com.ai.assistance.operit.data.model

/** Session stages select their configured model at the next model-request boundary. */
enum class PlanModelStage(val functionType: FunctionType) {
    GENERATION(FunctionType.PLAN_GENERATION),
    IMPLEMENTATION(FunctionType.PLAN_EXECUTION),
    // Review deliberately shares the planner binding; never add a separate review setting.
    REVIEW(FunctionType.PLAN_GENERATION),
}
