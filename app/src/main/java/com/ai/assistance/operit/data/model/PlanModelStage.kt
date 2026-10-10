package com.ai.assistance.operit.data.model

/** Persisted workflow metadata. These stages do not select a request model. */
enum class PlanModelStage {
    GENERATION, IMPLEMENTATION, REVIEW;
}
