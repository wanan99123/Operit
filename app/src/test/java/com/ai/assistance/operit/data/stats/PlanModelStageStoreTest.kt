package com.ai.assistance.operit.data.stats

import com.ai.assistance.operit.data.model.PlanModelStage
import com.ai.assistance.operit.data.model.PlanStep
import com.ai.assistance.operit.data.model.PlanStepStatus
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class PlanModelStageStoreTest {
    @Test fun isolatesSessionsAndClearsWithPlan() {
        val a = UUID.randomUUID().toString()
        val b = UUID.randomUUID().toString()
        try {
            assertNull(PlanModelStageStore.read(a))
            PlanModelStageStore.update(a, PlanModelStage.GENERATION)
            PlanModelStageStore.update(b, PlanModelStage.IMPLEMENTATION)
            assertEquals(PlanModelStage.GENERATION, PlanModelStageStore.read(a))
            assertEquals(PlanModelStage.IMPLEMENTATION, PlanModelStageStore.read(b))
            PlanStepStore.clear(a)
            assertNull(PlanModelStageStore.read(a))
            assertEquals(PlanModelStage.IMPLEMENTATION, PlanModelStageStore.read(b))
            PlanModelStageStore.update(b, null)
            assertNull(PlanModelStageStore.read(b))
        } finally {
            PlanStepStore.clear(a)
            PlanStepStore.clear(b)
        }
    }

    @Test fun absentWirePhaseDerivesFromTheCompleteChecklist() {
        assertNull(PlanModelStageStore.resolve(null, emptyList()))
        assertEquals(PlanModelStage.GENERATION, PlanModelStageStore.resolve(null, listOf(step(PlanStepStatus.PENDING))))
        assertEquals(PlanModelStage.IMPLEMENTATION, PlanModelStageStore.resolve(null, listOf(step(PlanStepStatus.IN_PROGRESS), step(PlanStepStatus.PENDING))))
        assertEquals(PlanModelStage.REVIEW, PlanModelStageStore.resolve(null, listOf(step(PlanStepStatus.COMPLETED))))
    }

    @Test fun explicitWirePhaseOverridesProgressIncludingEmptyBootstrap() {
        assertEquals(PlanModelStage.GENERATION, PlanModelStageStore.resolve("generation", emptyList()))
        assertEquals(PlanModelStage.IMPLEMENTATION, PlanModelStageStore.resolve("implementation", listOf(step(PlanStepStatus.PENDING))))
        assertEquals(PlanModelStage.REVIEW, PlanModelStageStore.resolve("review", listOf(step(PlanStepStatus.IN_PROGRESS))))
        assertNull(PlanModelStageStore.resolve("chat", listOf(step(PlanStepStatus.COMPLETED))))
    }

    @Test fun invalidPhaseDoesNotMutateExistingState() {
        val id = UUID.randomUUID().toString()
        try {
            PlanModelStageStore.update(id, PlanModelStage.GENERATION)
            try {
                val invalid = PlanModelStageStore.resolve("invalid", emptyList())
                PlanModelStageStore.update(id, invalid)
                fail("Invalid phase was accepted")
            } catch (_: IllegalArgumentException) { }
            assertEquals(PlanModelStage.GENERATION, PlanModelStageStore.read(id))
        } finally { PlanStepStore.clear(id) }
    }

    private fun step(status: PlanStepStatus) = PlanStep("Work", status)
}
