package com.ai.assistance.operit.data.stats

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ChatRequestTokenUsageTest {
    @Test fun estimatesAreNeverAdvertisedAsConfirmedUsage() {
        val usage = ChatRequestTokenUsage().withEstimate(1234, 56)
        assertEquals(1234L, usage.estimatedInputTokens)
        assertNull(usage.confirmedInputTokens)
        assertNull(usage.confirmedOutputTokens)
        assertNull(usage.cacheHitPercent)
    }

    @Test fun cachedTokensAreIncludedInInputAndNotAddedTwice() {
        val snapshot = ProviderUsageNormalizer.openAiChatCompletions(JSONObject(
            """{"prompt_tokens":1000,"completion_tokens":40,"prompt_tokens_details":{"cached_tokens":970}}"""
        ))!!
        val usage = ChatRequestTokenUsage().withProviderUsage(snapshot, 1)
        assertEquals(1000L, usage.confirmedInputTokens)
        assertEquals(40L, usage.confirmedOutputTokens)
        assertEquals(97.0, usage.cacheHitPercent!!, 0.001)
    }

    @Test fun deepSeekTopLevelCacheHitFieldIsSupported() {
        val snapshot = ProviderUsageNormalizer.openAiChatCompletions(JSONObject(
            """{"prompt_tokens":1000,"completion_tokens":40,"prompt_cache_hit_tokens":970,"prompt_cache_miss_tokens":30}"""
        ))!!
        assertEquals(970L, snapshot.cachedInputTokens)
        assertEquals(30L, snapshot.uncachedInputTokens)
    }

    @Test fun absentCacheReportIsUnknownRatherThanZero() {
        val snapshot = ProviderUsageNormalizer.openAiChatCompletions(JSONObject(
            """{"prompt_tokens":1000,"completion_tokens":40}"""
        ))!!
        val usage = ChatRequestTokenUsage().withProviderUsage(snapshot, 1)
        assertEquals(1000L, usage.confirmedInputTokens)
        assertNull(usage.cacheHitPercent)
    }

    @Test fun partialUpdatesMergeButNewRetryDoesNotInheritCache() {
        val first = ChatRequestTokenUsage().withProviderUsage(ProviderUsageSnapshot(
            totalInputTokens = 200, cachedInputTokens = 100, source = "test"
        ), 1)
        val delta = first.withProviderUsage(ProviderUsageSnapshot(outputTokens = 7, source = "test"), 1)
        assertEquals(50.0, delta.cacheHitPercent!!, 0.001)
        assertEquals(7L, delta.confirmedOutputTokens)
        val retry = delta.withProviderUsage(ProviderUsageSnapshot(totalInputTokens = 300, source = "test"), 2)
        assertEquals(300L, retry.confirmedInputTokens)
        assertNull(retry.cacheHitPercent)
        assertNull(retry.confirmedOutputTokens)
    }

    @Test fun completeSnapshotCanRevokeEarlierKnownFields() {
        val first = ChatRequestTokenUsage().withProviderUsage(ProviderUsageSnapshot(
            totalInputTokens = 100, cachedInputTokens = 50, source = "test"
        ), 1)
        val final = first.withProviderUsage(ProviderUsageSnapshot(
            totalInputTokens = 100, completeSnapshot = true, source = "test"
        ), 1)
        assertNull(final.cacheHitPercent)
    }

    @Test fun zeroInputAndInvalidCacheHaveNoRatio() {
        listOf(0L to 0L, 10L to 11L, 10L to -1L).forEach { (input, cache) ->
            val usage = ChatRequestTokenUsage().withProviderUsage(ProviderUsageSnapshot(
                totalInputTokens = input, cachedInputTokens = cache, source = "test"
            ), 1)
            assertNull(usage.cacheHitPercent)
        }
    }

    @Test fun separateReasoningIsCountedOnceAndLongAdditionSaturates() {
        val usage = ChatRequestTokenUsage().withProviderUsage(ProviderUsageSnapshot(
            outputTokens = Long.MAX_VALUE - 1, reasoningTokens = 5,
            reasoningIncludedInOutput = false, source = "test"
        ), 1)
        assertEquals(Long.MAX_VALUE, usage.confirmedOutputTokens)
        val included = usage.withProviderUsage(ProviderUsageSnapshot(
            outputTokens = 10, reasoningTokens = 5, reasoningIncludedInOutput = true,
            completeSnapshot = true, source = "test"
        ), 1)
        assertEquals(10L, included.confirmedOutputTokens)
    }

    @Test fun anthropicPartialInputIncludesKnownCacheComponents() {
        val first = ChatRequestTokenUsage().withProviderUsage(ProviderUsageSnapshot(
            uncachedInputTokens = 20, cachedInputTokens = 30, cacheWriteTokens = 50,
            cacheWriteSeparateBilling = true, source = "test"
        ), 1)
        assertEquals(100L, first.confirmedInputTokens)
        assertEquals(30.0, first.cacheHitPercent!!, 0.001)
    }
}