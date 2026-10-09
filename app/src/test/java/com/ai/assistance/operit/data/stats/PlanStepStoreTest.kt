package com.ai.assistance.operit.data.stats

import com.ai.assistance.operit.data.model.PlanStep
import com.ai.assistance.operit.data.model.PlanStepStatus
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class PlanStepStoreTest {
    @Test fun isolatesChatsAndReplacesAtomically() {
        val a = UUID.randomUUID().toString()
        val b = UUID.randomUUID().toString()
        val pending = listOf(PlanStep("Inspect", PlanStepStatus.PENDING))
        val done = listOf(PlanStep("Inspect", PlanStepStatus.COMPLETED))
        try {
            assertTrue(PlanStepStore.update(a, pending).isEmpty())
            PlanStepStore.update(b, done)
            assertEquals(pending, PlanStepStore.read(a))
            assertEquals(done, PlanStepStore.read(b))
            assertEquals(pending, PlanStepStore.update(a, done))
            PlanStepStore.clear(a)
            assertTrue(PlanStepStore.read(a).isEmpty())
            assertEquals(done, PlanStepStore.read(b))
        } finally {
            PlanStepStore.clear(a)
            PlanStepStore.clear(b)
        }
    }

    @Test fun copiesCallerList() {
        val id = UUID.randomUUID().toString()
        val list = mutableListOf(PlanStep("Inspect", PlanStepStatus.PENDING))
        try {
            PlanStepStore.update(id, list)
            list.clear()
            assertEquals(1, PlanStepStore.read(id).size)
        } finally { PlanStepStore.clear(id) }
    }
}
