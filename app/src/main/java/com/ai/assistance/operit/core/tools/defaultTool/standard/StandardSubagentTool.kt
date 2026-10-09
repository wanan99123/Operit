package com.ai.assistance.operit.core.tools.defaultTool.standard

import android.content.Context
import com.ai.assistance.operit.R
import com.ai.assistance.operit.api.chat.EnhancedAIService
import com.ai.assistance.operit.api.chat.enhance.ToolExecutionManager
import com.ai.assistance.operit.core.config.SystemPromptConfig
import com.ai.assistance.operit.core.tools.StringResultData
import com.ai.assistance.operit.core.tools.ToolExecutor
import com.ai.assistance.operit.data.model.AITool
import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.ToolResult
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.ChatMarkupRegex
import com.ai.assistance.operit.util.ChatUtils
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/** One parent-call-scoped delegated agent. No detached job or persisted hidden conversation. */
class StandardSubagentTool(context: Context) : ToolExecutor {
    private val appContext = context.applicationContext

    override fun invoke(tool: AITool): ToolResult {
        // A synchronous/package bridge loses coroutine cancellation; fail instead of orphaning work.
        return failure(tool, "run_subagent requires the native streaming tool execution path")
    }

    override fun invokeAndStream(tool: AITool): Flow<ToolResult> = flow {
        val parent = ToolExecutionManager.currentToolRuntimeContext()
        if (parent == null || parent.isSubTask) {
            emit(failure(tool, "Only a parent agent can delegate a subtask; nested delegation is disabled"))
            return@flow
        }
        val request = try {
            SubagentRequest.parse(tool.parameters.map { it.name to it.value })
        } catch (error: IllegalArgumentException) {
            emit(failure(tool, error.message ?: "Invalid subagent parameters"))
            return@flow
        }
        val configId = parent.chatModelConfigId
        val modelIndex = parent.chatModelIndex
        if (configId.isNullOrBlank() || modelIndex == null) {
            emit(failure(tool, "Missing parent model identity"))
            return@flow
        }
        if (!slots.tryAcquire()) {
            emit(failure(tool, "Two subagents are already running. Wait for a result before delegating again."))
            return@flow
        }
        val id = UUID.randomUUID().toString()
        var service: EnhancedAIService? = null
        var toolCalls = 0
        try {
            val child = EnhancedAIService.createSubagentInstance(appContext)
            service = child
            emit(ToolResult(tool.name, true, StringResultData(
                JSONObject().put("id", id).put("status", "running")
                    .put("message", appContext.getString(R.string.chat_subagent_running)).toString()
            )))
            val result = withTimeout(request.timeoutSeconds * 1000L) {
                val config = child.getModelConfigForFunction(FunctionType.CHAT, configId, modelIndex)
                val maxContextTokens = (config.contextLength.toDouble() * 1024.0).toInt()
                require(maxContextTokens > 0) { "Parent model context length must be configured" }
                require(config.summaryTokenThreshold.toDouble() > 0.0 && config.summaryTokenThreshold.toDouble() <= 1.0) {
                    "Invalid parent context threshold"
                }
                val prompt = buildString {
                    appendLine("Complete this delegated task and return a concise result to the parent agent:")
                    appendLine(request.task)
                    if (request.contextText.isNotBlank()) { appendLine("Provided context:"); appendLine(request.contextText) }
                    appendLine("Do not delegate again. Keep tool calls within ${request.maxToolCalls}.")
                    appendLine("Do not ask the user questions. If blocked, state the blocker and stop.")
                    appendLine("Return findings and verification, not hidden reasoning or raw tool markup.")
                }
                child.sendMessage(EnhancedAIService.SendMessageOptions(
                    message = prompt,
                    maxTokens = maxContextTokens,
                    tokenUsageThreshold = config.summaryTokenThreshold.toDouble(),
                    chatId = "subagent:$id",
                    chatHistory = emptyList(),
                    workspacePath = parent.workspacePath,
                    workspaceEnv = parent.workspaceEnv,
                    functionType = FunctionType.CHAT,
                    isSubTask = true,
                    roleCardId = parent.callerCardId,
                    enableMemoryAutoUpdate = false,
                    notifyReplyOverride = false,
                    customSystemPromptTemplate = SystemPromptConfig.SUBTASK_AGENT_PROMPT_TEMPLATE,
                    chatModelConfigIdOverride = configId,
                    chatModelIndexOverride = modelIndex,
                    memorySpaceIdOverride = parent.memorySpaceId,
                    maxToolCalls = request.maxToolCalls,
                    onToolInvocation = { toolCalls += 1 },
                )).collect { currentCoroutineContext().ensureActive() }
                currentCoroutineContext().ensureActive()
                child.getSubagentFailure()?.let { throw IllegalStateException(it) }
                val reply = child.getSubagentFinalReply()
                    ?: throw IllegalStateException("Subagent did not produce a final answer")
                val summary = ChatUtils.removeThinkingContent(reply)
                    .replace(ChatMarkupRegex.statusTag, "")
                    .replace(ChatMarkupRegex.statusSelfClosingTag, "")
                    .trim().take(16_000)
                require(summary.isNotBlank()) { "Subagent returned no usable summary" }
                JSONObject().put("id", id).put("status", "completed").put("summary", summary)
                    .put("tool_calls", toolCalls)
                    .put("input_tokens", child.getCurrentInputTokenCount())
                    .put("output_tokens", child.getCurrentOutputTokenCount())
                    .put("usage_source", "provider_or_stream_estimate")
                    .put("usage_note", "Child turn usage; included in provider statistics, not parent chat totals")
            }
            emit(ToolResult(tool.name, true, StringResultData(result.toString())))
        } catch (error: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            emit(failure(tool, "Subagent timed out after ${request.timeoutSeconds}s"))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLogger.e(TAG, "Delegated task failed: $id", error)
            emit(failure(tool, error.message ?: error.javaClass.simpleName))
        } finally {
            try {
                withContext(NonCancellable) { service?.closeSubagentInstance() }
            } finally {
                slots.release()
            }
        }
    }

    private fun failure(tool: AITool, reason: String) =
        ToolResult(tool.name, false, StringResultData(""), reason)

    companion object {
        private const val TAG = "StandardSubagentTool"
        private val slots = Semaphore(2)
    }
}