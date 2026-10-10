package com.ai.assistance.operit.data.model

/** Explicit user-selected stages; progress updates alone never switch the conversation model. */
enum class PlanModelStage(val functionType: FunctionType) {
    GENERATION(FunctionType.PLAN_GENERATION),
    IMPLEMENTATION(FunctionType.PLAN_EXECUTION),
    // Review deliberately shares the planner binding; never add a separate review setting.
    REVIEW(FunctionType.PLAN_GENERATION),
}