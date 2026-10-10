package com.ai.assistance.operit.data.preferences
import com.ai.assistance.operit.data.model.FunctionType
import org.junit.Assert.assertEquals
import org.junit.Test
class SelectedRequestModelTest {
    private val mappings = mapOf(
        FunctionType.CHAT to FunctionConfigMapping("chat", 2),
        FunctionType.SUBAGENT to FunctionConfigMapping("child", 3),
        FunctionType.PLAN_GENERATION to FunctionConfigMapping("planner", 4),
        FunctionType.PLAN_EXECUTION to FunctionConfigMapping("implementer", 5),
    )
    @Test fun selectedChatModelSurvivesToolContinuations() {
        repeat(4) {
            assertEquals(FunctionConfigMapping("selected", 7), SelectedRequestModel.resolve(FunctionType.CHAT, mappings, "selected", 7, false))
        }
        assertEquals(FunctionConfigMapping("chat", 2), SelectedRequestModel.resolve(FunctionType.CHAT, mappings, null, null, false))
    }
    @Test fun childUsesIndependentBindingNotParentSelection() {
        assertEquals(FunctionConfigMapping("child", 3), SelectedRequestModel.resolve(FunctionType.CHAT, mappings, "selected", 7, true))
    }
}
