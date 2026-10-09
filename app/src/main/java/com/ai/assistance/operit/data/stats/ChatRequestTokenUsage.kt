package com.ai.assistance.operit.data.stats

/** One HTTP request, not the sum of every request in the conversation. */
data class ChatRequestTokenUsage(
    val estimatedInputTokens: Long = 0L,
    val estimatedOutputTokens: Long = 0L,
    val providerUsage: ProviderUsageSnapshot? = null,
    val attempt: Int? = null,
) {
    val confirmedInputTokens: Long?
        get() {
            val usage = providerUsage ?: return null
            usage.totalInputTokens?.let { return it }
            val uncached = usage.uncachedInputTokens ?: return null
            val cached = usage.cachedInputTokens ?: return null
            val writes = if (usage.cacheWriteSeparateBilling) usage.cacheWriteTokens ?: return null else 0L
            return saturatedAdd(saturatedAdd(uncached, cached), writes)
        }

    val confirmedOutputTokens: Long?
        get() {
            val usage = providerUsage ?: return null
            val output = usage.outputTokens ?: return null
            return if (usage.reasoningIncludedInOutput == false) {
                usage.reasoningTokens?.let { saturatedAdd(output, it) }
            } else output
        }

    val cacheHitPercent: Double?
        get() {
            val input = confirmedInputTokens ?: return null
            val cached = providerUsage?.cachedInputTokens ?: return null
            if (input <= 0L || cached !in 0L..input) return null
            return cached.toDouble() / input.toDouble() * 100.0
        }

    fun withEstimate(input: Long, output: Long): ChatRequestTokenUsage = copy(
        estimatedInputTokens = input.coerceAtLeast(0L),
        estimatedOutputTokens = output.coerceAtLeast(0L),
    )

    fun withProviderUsage(update: ProviderUsageSnapshot, attempt: Int): ChatRequestTokenUsage {
        val key = attempt.coerceAtLeast(1)
        // Never carry a previous retry attempt's cache split into the current attempt.
        val previous = providerUsage.takeIf { this.attempt == key }
        val merged = if (previous == null || update.completeSnapshot) update else update.copy(
            uncachedInputTokens = update.uncachedInputTokens ?: previous.uncachedInputTokens,
            cachedInputTokens = update.cachedInputTokens ?: previous.cachedInputTokens,
            cacheWriteTokens = update.cacheWriteTokens ?: previous.cacheWriteTokens,
            totalInputTokens = update.totalInputTokens ?: previous.totalInputTokens,
            outputTokens = update.outputTokens ?: previous.outputTokens,
            reasoningTokens = update.reasoningTokens ?: previous.reasoningTokens,
        )
        return copy(providerUsage = merged, attempt = key)
    }

    /** A retry without a confirmed successful usage must not retain a failed attempt's report. */
    fun finalized(successfulAttempt: Int?): ChatRequestTokenUsage =
        if (successfulAttempt != null && successfulAttempt.coerceAtLeast(1) == attempt) this
        else copy(providerUsage = null, attempt = successfulAttempt)

    companion object {
        internal fun saturatedAdd(left: Long, right: Long): Long {
            val a = left.coerceAtLeast(0L)
            val b = right.coerceAtLeast(0L)
            return if (Long.MAX_VALUE - a < b) Long.MAX_VALUE else a + b
        }
    }
}
