package com.ai.assistance.operit.core.tools.defaultTool.standard

import com.ai.assistance.operit.api.chat.enhance.ToolExecutionManager
import com.ai.assistance.operit.core.tools.StringResultData
import com.ai.assistance.operit.core.tools.ToolExecutor
import com.ai.assistance.operit.data.model.AITool
import com.ai.assistance.operit.data.model.PlanStepSummary
import com.ai.assistance.operit.data.model.ToolResult
import com.ai.assistance.operit.data.stats.PlanStepStore
import org.json.JSONObject

/**
 * Writes the session working plan, mirroring ZCode's TodoWrite contract:
 *
 * - the model always sends the **complete** list, which replaces the previous plan;
 * - the tool is session-scoped and never touches the file system;
 * - the result echoes the previous list, the new list and aggregate counters so the model can
 *   confirm what changed without a second read.
 *
 * Nested delegation is rejected: a subagent writing the parent's plan would desync the progress view
 * the parent is rendering.
 */
class UpdatePlanTool : ToolExecutor {

    override fun invoke(tool: AITool): ToolResult {
        val runtime = ToolExecutionManager.currentToolRuntimeContext()
        if (runtime?.isSubTask == true) {
            return failure(tool, "A subagent cannot write the parent session plan")
        }
        val chatKey = runtime?.callerChatId
        if (chatKey.isNullOrBlank()) {
            return failure(tool, "update_plan requires an active chat session")
        }

        val rawTodos = tool.parameters.firstOrNull { it.name == "todos" }?.value.orEmpty()
        val steps =
            try {
                PlanStepRequest.parse(rawTodos)
            } catch (error: IllegalArgumentException) {
                return failure(tool, error.message ?: "Invalid plan steps")
            }

        val previous = PlanStepStore.update(chatKey, steps)
        val summary = PlanStepSummary.of(steps)

        val payload =
            JSONObject()
                .put("old_todos", PlanStepRequest.toJson(previous))
                .put("todos", PlanStepRequest.toJson(steps))
                .put(
                    "summary",
                    JSONObject()
                        .put("total", summary.total)
                        .put("pending", summary.pending)
                        .put("in_progress", summary.inProgress)
                        .put("completed", summary.completed)
                )
                .toString()

        // Session state is observable; no Android logging is needed for this contract.
        return ToolResult(tool.name, true, StringResultData(payload))
    }

    override fun validateParameters(tool: AITool): com.ai.assistance.operit.data.model.ToolValidationResult {
        val rawTodos = tool.parameters.firstOrNull { it.name == "todos" }?.value.orEmpty()
        return try {
            PlanStepRequest.parse(rawTodos)
            com.ai.assistance.operit.data.model.ToolValidationResult(valid = true)
        } catch (error: IllegalArgumentException) {
            com.ai.assistance.operit.data.model.ToolValidationResult(
                valid = false,
                errorMessage = error.message ?: "Invalid plan steps"
            )
        }
    }

    private fun failure(tool: AITool, reason: String) =
        ToolResult(tool.name, false, StringResultData(""), reason)
}

/**
 * Reads back the session working plan, mirroring ZCode's TodoRead. Read-only and side-effect free,
 * so it stays available to subagents that need to know the parent's plan.
 */
class ReadPlanTool : ToolExecutor {

    override fun invoke(tool: AITool): ToolResult {
        val chatKey = ToolExecutionManager.currentToolRuntimeContext()?.callerChatId
        if (chatKey.isNullOrBlank()) {
            return ToolResult(
                tool.name,
                false,
                StringResultData(""),
                "read_plan requires an active chat session"
            )
        }
        val steps = PlanStepStore.read(chatKey)
        val payload = JSONObject().put("todos", PlanStepRequest.toJson(steps)).toString()
        return ToolResult(tool.name, true, StringResultData(payload))
    }
}