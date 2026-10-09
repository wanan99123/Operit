package com.ai.assistance.operit.core.tools.defaultTool.standard

/** Public delegated-tool arguments. Context and identity come from the caller, never the model. */
data class SubagentRequest(
    val task: String,
    val contextText: String,
    val maxToolCalls: Int,
    val timeoutSeconds: Int,
) {
    companion object {
        fun parse(parameters: List<Pair<String, String>>): SubagentRequest {
            val allowed = setOf("task", "context_text", "max_tool_calls", "timeout_seconds")
            require(parameters.all { it.first in allowed }) { "Unsupported subagent parameter" }
            require(parameters.map { it.first }.distinct().size == parameters.size) { "Duplicate subagent parameter" }
            val params = parameters.toMap()
            val task = params["task"]?.trim().orEmpty()
            require(task.isNotEmpty() && task.length <= 16_000) { "task must contain 1..16000 characters" }
            val context = params["context_text"].orEmpty()
            require(context.length <= 32_000) { "context_text exceeds 32000 characters" }
            fun bounded(name: String, default: Int, range: IntRange): Int {
                val raw = params[name] ?: return default
                val value = raw.toIntOrNull() ?: throw IllegalArgumentException("$name must be an integer")
                require(value in range) { "$name must be in $range" }
                return value
            }
            return SubagentRequest(task, context,
                bounded("max_tool_calls", 8, 1..32), bounded("timeout_seconds", 180, 10..600))
        }
    }
}