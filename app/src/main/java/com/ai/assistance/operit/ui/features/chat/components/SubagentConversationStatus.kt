package com.ai.assistance.operit.ui.features.chat.components

/** One explicit mapping for the conversation indicator, also exercised by JVM tests. */
internal enum class SubagentConversationStatus {
    RUNNING, RETRYING, COMPLETED, FAILED, TIMED_OUT, CANCELLED;

    val isActive: Boolean get() = this == RUNNING || this == RETRYING

    companion object {
        fun fromStatus(status: String): SubagentConversationStatus = when (status) {
            "running" -> RUNNING
            "retrying" -> RETRYING
            "completed" -> COMPLETED
            "failed" -> FAILED
            "timed_out" -> TIMED_OUT
            "cancelled" -> CANCELLED
            else -> error("Unknown subagent status: $status")
        }
    }
}
