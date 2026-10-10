package com.ai.assistance.operit.data.preferences

import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.PlanModelStage
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanModelRoutingTest {
    @Test
    fun eachStageSelectsOnlyItsOwnConfigAndModelIndex() {
        val mappings = mapOf(
            FunctionType.CHAT to FunctionConfigMapping("chat", 9),
            FunctionType.PLAN_GENERATION to FunctionConfigMapping("planner", 1),
            FunctionType.PLAN_EXECUTION to FunctionConfigMapping("implementer", 2),
            FunctionType.SUBAGENT to FunctionConfigMapping("child", 3),
        )
        assertEquals(FunctionConfigMapping("planner", 1), PlanModelRouting.resolve(PlanModelStage.GENERATION, mappings))
        assertEquals(FunctionConfigMapping("implementer", 2), PlanModelRouting.resolve(PlanModelStage.IMPLEMENTATION, mappings))
        assertEquals(FunctionConfigMapping("child", 3), mappings[FunctionType.SUBAGENT])
    }

    @Test
    fun unsetStagesUseDefaultWithoutInheritingSiblingOrChatBindings() {
        val mappings = mapOf(
            FunctionType.CHAT to FunctionConfigMapping("chat", 9),
            FunctionType.SUBAGENT to FunctionConfigMapping("child", 3),
            FunctionType.PLAN_GENERATION to FunctionConfigMapping("planner", 1),
        )
        assertEquals(FunctionConfigMapping(), PlanModelRouting.resolve(PlanModelStage.IMPLEMENTATION, mappings))
        assertEquals(FunctionConfigMapping(), PlanModelRouting.resolve(PlanModelStage.GENERATION, emptyMap()))
    }

    @Test
    fun reviewSharesTheExactPlannerMappingAndReflectsPlannerChanges() {
        val mappings = mutableMapOf(
            FunctionType.PLAN_GENERATION to FunctionConfigMapping("planner", 2),
            FunctionType.PLAN_EXECUTION to FunctionConfigMapping("implementer", 3),
            FunctionType.SUBAGENT to FunctionConfigMapping("child", 4),
        )
        assertEquals(FunctionType.PLAN_GENERATION, PlanModelStage.REVIEW.functionType)
        assertEquals(
            PlanModelRouting.resolve(PlanModelStage.GENERATION, mappings),
            PlanModelRouting.resolve(PlanModelStage.REVIEW, mappings),
        )
        mappings[FunctionType.PLAN_GENERATION] = FunctionConfigMapping("new-planner", 5)
        assertEquals(FunctionConfigMapping("new-planner", 5), PlanModelRouting.resolve(PlanModelStage.REVIEW, mappings))
        assertEquals(FunctionConfigMapping("implementer", 3), PlanModelRouting.resolve(PlanModelStage.IMPLEMENTATION, mappings))
        mappings.remove(FunctionType.PLAN_GENERATION)
        assertEquals(FunctionConfigMapping(), PlanModelRouting.resolve(PlanModelStage.REVIEW, mappings))
        assertEquals(FunctionConfigMapping(), PlanModelRouting.resolve(PlanModelStage.GENERATION, mappings))
    }

    @Test
    fun explicitlySelectedDefaultConfigKeepsItsChosenModelIndex() {
        val mappings = mapOf(FunctionType.PLAN_EXECUTION to FunctionConfigMapping("default", 2))
        assertEquals(FunctionConfigMapping("default", 2), PlanModelRouting.resolve(PlanModelStage.IMPLEMENTATION, mappings))
    }
}
