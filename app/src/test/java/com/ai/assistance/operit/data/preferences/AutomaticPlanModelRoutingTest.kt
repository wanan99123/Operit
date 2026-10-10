package com.ai.assistance.operit.data.preferences

import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.PlanModelStage
import org.junit.Assert.*
import org.junit.Test

class AutomaticPlanModelRoutingTest {
    private val ordinary = FunctionConfigMapping("composer", 9)
    private val mappings = mapOf(
        FunctionType.CHAT to ordinary,
        FunctionType.PLAN_GENERATION to FunctionConfigMapping("planner", 1),
        FunctionType.PLAN_EXECUTION to FunctionConfigMapping("implementer", 2),
        FunctionType.SUBAGENT to FunctionConfigMapping("child", 3),
    )

    @Test fun phaseTransitionsSelectConfiguredTargetsAndReturnToOrdinaryChat() {
        val stages = listOf(null, PlanModelStage.GENERATION, PlanModelStage.IMPLEMENTATION, PlanModelStage.REVIEW, null)
        val targets = stages.map { AutomaticPlanModelRouting.resolve(it, mappings, ordinary, false) }
        assertEquals(listOf(ordinary, FunctionConfigMapping("planner", 1), FunctionConfigMapping("implementer", 2), FunctionConfigMapping("planner", 1), ordinary), targets)
    }

    @Test fun defaultInitialTargetStillSwitchesWithinTheSameExecution() {
        val first = FunctionConfigMapping()
        assertEquals(first, AutomaticPlanModelRouting.resolve(null, mappings, first, false))
        assertEquals(FunctionConfigMapping("planner", 1), AutomaticPlanModelRouting.resolve(PlanModelStage.GENERATION, mappings, first, false))
        assertEquals(FunctionConfigMapping("implementer", 2), AutomaticPlanModelRouting.resolve(PlanModelStage.IMPLEMENTATION, mappings, first, false))
    }

    @Test fun subagentNeverInheritsParentPhase() {
        val child = mappings.getValue(FunctionType.SUBAGENT)
        for (stage in PlanModelStage.values()) {
            assertEquals(child, AutomaticPlanModelRouting.resolve(stage, mappings, child, true))
        }
    }

    @Test fun unsetPhaseUsesUserRequestedDefaultAndSettingsUpdatesAreVisible() {
        assertEquals(FunctionConfigMapping(), AutomaticPlanModelRouting.resolve(PlanModelStage.IMPLEMENTATION, emptyMap(), ordinary, false))
        val updated = mappings + (FunctionType.PLAN_GENERATION to FunctionConfigMapping("new-planner", 5))
        assertEquals(FunctionConfigMapping("new-planner", 5), AutomaticPlanModelRouting.resolve(PlanModelStage.REVIEW, updated, ordinary, false))
    }

    @Test fun reviewAndGenerationShareOneFunction() {
        assertEquals(FunctionType.CHAT, AutomaticPlanModelRouting.resolveFunction(null))
        assertEquals(FunctionType.PLAN_GENERATION, AutomaticPlanModelRouting.resolveFunction(PlanModelStage.GENERATION))
        assertEquals(FunctionType.PLAN_GENERATION, AutomaticPlanModelRouting.resolveFunction(PlanModelStage.REVIEW))
        assertEquals(FunctionType.PLAN_EXECUTION, AutomaticPlanModelRouting.resolveFunction(PlanModelStage.IMPLEMENTATION))
    }
}
