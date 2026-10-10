package com.ai.assistance.operit.data.stats

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Latest parent model request only: child requests and tool execution never enter this clock. */
data class RequestPerformance(
    val startedAtMs: Long,
    val firstContentAtMs: Long? = null,
    val sampledAtMs: Long = startedAtMs,
    val outputTokens: Long = 0L,
    val finished: Boolean = false,
) {
    val firstTokenLatencyMs: Long?
        get() = firstContentAtMs?.let { (it - startedAtMs).coerceAtLeast(0L) }

    val tokensPerSecond: Double?
        get() {
            val first = firstContentAtMs ?: return null
            val duration = sampledAtMs - first
            if (duration <= 0L || outputTokens <= 0L) return null
            return outputTokens.toDouble() * 1000.0 / duration.toDouble()
        }
}

/** Monotonic elapsed timestamps are supplied by the caller. No fabricated metrics for missing data. */
object RequestPerformanceStore {
    private val state = MutableStateFlow<Map<String, RequestPerformance>>(emptyMap())
    val sessions: StateFlow<Map<String, RequestPerformance>> = state.asStateFlow()

    @Synchronized
    fun begin(chatId: String, nowMs: Long) {
        state.value = state.value + (chatId to RequestPerformance(nowMs))
    }

    @Synchronized
    fun firstContent(chatId: String, nowMs: Long) {
        val current = state.value[chatId] ?: return
        if (current.firstContentAtMs != null || current.finished) return
        state.value = state.value + (chatId to current.copy(firstContentAtMs = nowMs, sampledAtMs = nowMs))
    }

    @Synchronized
    fun tokens(chatId: String, outputTokens: Long, nowMs: Long) {
        val current = state.value[chatId] ?: return
        if (current.finished) return
        state.value = state.value + (chatId to current.copy(outputTokens = outputTokens.coerceAtLeast(0L), sampledAtMs = nowMs))
    }

    @Synchronized
    fun complete(chatId: String, outputTokens: Long, nowMs: Long) {
        val current = state.value[chatId] ?: return
        state.value = state.value + (chatId to current.copy(outputTokens = outputTokens.coerceAtLeast(0L), sampledAtMs = nowMs, finished = true))
    }

    /** Cancellation/error closes the current sample without inventing a final token count. */
    @Synchronized
    fun stop(chatId: String) {
        val current = state.value[chatId] ?: return
        if (!current.finished) state.value = state.value + (chatId to current.copy(finished = true))
    }

    @Synchronized
    fun clear(chatId: String) {
        state.value = state.value - chatId
    }
}
