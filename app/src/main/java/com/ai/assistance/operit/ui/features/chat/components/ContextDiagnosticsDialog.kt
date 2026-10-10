package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.stats.ContextCompressionCoverage
import com.ai.assistance.operit.data.stats.ContextCompressionDiagnostics
import com.ai.assistance.operit.data.stats.ContextDiagnostics
import com.ai.assistance.operit.data.stats.ContextDiagnosticsStore
import com.ai.assistance.operit.data.stats.ContextRequestDiagnostics
import com.ai.assistance.operit.data.stats.ContextSectionAggregate
import com.ai.assistance.operit.data.stats.ContextSectionKind
import com.ai.assistance.operit.data.stats.ContextTokenCount
import com.ai.assistance.operit.data.stats.ContextTokenSource
import com.ai.assistance.operit.data.stats.aggregateByKind
import com.ai.assistance.operit.data.stats.coverageRange

/**
 * Replaces the old header dropdown with a full M3 dialog.
 *
 * Why aggregated rows instead of one line per section: a long chat can hold hundreds of
 * messages, and composing one Text per message froze the header. Rows are now rolled up by
 * [ContextSectionKind] (max one row per kind) and the compression coverage is a single span
 * that the user can expand on demand.
 *
 * Contract notes for this screen:
 * - unknown counts render as "not recorded", never as 0.
 * - ESTIMATE / LOCAL_TOKENIZER counts are labelled as client side values, only SERVER is exact.
 * - message references show a timestamp, which is not a unique message id.
 */
@Composable
fun ContextDiagnosticsDialog(
    chatId: String?,
    currentWindowSize: Long,
    inputTokens: Long,
    outputTokens: Long,
    onDismiss: () -> Unit
) {
    val sessions by ContextDiagnosticsStore.sessions.collectAsState()
    val diagnostics: ContextDiagnostics? = chatId?.let { sessions[it] }
    var coverageExpanded by rememberSaveable(chatId) { mutableStateOf(false) }
    val request = diagnostics?.request
    val compression = diagnostics?.compression
    val aggregates = remember(request) { request?.aggregateByKind().orEmpty() }
    val coverage = remember(compression) { compression?.coverageRange() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.context_diag_title)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.context_diag_close)) }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewCard(currentWindowSize, inputTokens, outputTokens)
                RequestCard(request, aggregates)
                CompressionCard(compression, coverage, coverageExpanded) { coverageExpanded = !coverageExpanded }
                Text(
                    text = stringResource(R.string.context_diag_timestamp_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun OverviewCard(currentWindowSize: Long, inputTokens: Long, outputTokens: Long) {
    DiagnosticsCard(stringResource(R.string.context_diag_usage_overview)) {
        Text(stringResource(R.string.context_window, currentWindowSize), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.input_tokens, inputTokens), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.output_tokens, outputTokens), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = stringResource(R.string.total_tokens, inputTokens + outputTokens),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.context_diag_usage_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RequestCard(request: ContextRequestDiagnostics?, aggregates: List<ContextSectionAggregate>) {
    DiagnosticsCard(stringResource(R.string.context_diag_request_title)) {
        if (request == null) {
            Text(
                text = stringResource(R.string.context_diag_request_unknown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@DiagnosticsCard
        }
        LabeledValue(stringResource(R.string.context_diag_request_total), countLabel(request.total))
        LabeledValue(stringResource(R.string.context_diag_model_window), limitLabel(request.limits.modelWindowTokens))
        LabeledValue(
            stringResource(R.string.context_diag_compression_threshold),
            limitLabel(request.limits.compressionThresholdTokens)
        )
        LabeledValue(stringResource(R.string.context_diag_recorded_at), request.recordedAt.toString())
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (aggregates.isEmpty()) {
            Text(
                text = stringResource(R.string.context_diag_empty_sections),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val attributedTokens = aggregates.sumOf { it.recordedTokens }
            Text(stringResource(R.string.context_diag_attribution_hint), style = MaterialTheme.typography.bodySmall)
            aggregates.forEach { aggregate -> AggregateRow(aggregate, attributedTokens) }
        }
    }
}

@Composable
private fun AggregateRow(aggregate: ContextSectionAggregate, attributedTokens: Long) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.context_diag_section_aggregate, kindLabel(aggregate.kind), aggregateTokensLabel(aggregate), aggregate.sectionCount.toString()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Text(aggregateSourceLabel(aggregate), style = MaterialTheme.typography.labelSmall)
        if (attributedTokens > 0 && aggregate.isFullyRecorded) {
            val fraction = aggregate.recordedTokens.toFloat() / attributedTokens.toFloat()
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.context_diag_share, (fraction * 100).toInt()),
                style = MaterialTheme.typography.labelSmall)
        }
        Text(
            text = aggregateSpanLabel(aggregate),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompressionCard(
    compression: ContextCompressionDiagnostics?,
    coverage: ContextCompressionCoverage?,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    DiagnosticsCard(stringResource(R.string.context_diag_compression_title)) {
        if (compression == null) {
            Text(
                text = stringResource(R.string.context_diag_compression_unknown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@DiagnosticsCard
        }
        LabeledValue(stringResource(R.string.context_diag_compression_reason), compressionReasonLabel(compression.reason))
        Text(stringResource(R.string.context_diag_compression_hint), style = MaterialTheme.typography.bodySmall)
        LabeledValue(stringResource(R.string.context_diag_compression_before), countLabel(compression.before))
        LabeledValue(stringResource(R.string.context_diag_compression_after), countLabel(compression.after))
        LabeledValue(stringResource(R.string.context_diag_recorded_at), compression.recordedAt.toString())
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (coverage == null) {
            Text(
                text = stringResource(R.string.context_diag_no_coverage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(
                    R.string.context_diag_coverage_range,
                    coverage.messageCount.toString(),
                    coverage.firstIndex.toString(),
                    coverage.lastIndex.toString()
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            val resources = LocalContext.current.resources
            Text(
                text = coverage.roleCounts.entries.joinToString(
                    separator = ", ",
                    prefix = stringResource(R.string.context_diag_coverage_roles) + " "
                ) { entry -> resources.getString(R.string.context_diag_role_count, entry.key, entry.value) },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onToggleExpanded) {
                Text(
                    stringResource(
                        if (expanded) R.string.context_diag_coverage_collapse else R.string.context_diag_coverage_expand
                    )
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LabeledValue(stringResource(R.string.context_diag_coverage_first_time), coverage.firstTimestamp.toString())
                    LabeledValue(stringResource(R.string.context_diag_coverage_last_time), coverage.lastTimestamp.toString())
                    Text(
                        text = stringResource(R.string.context_diag_timestamp_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun countLabel(count: ContextTokenCount?): String = if (count == null) {
    stringResource(R.string.context_diag_unknown_count)
} else {
    stringResource(
        R.string.context_diag_count_with_source,
        count.tokens.toString(),
        tokenSourceLabel(count.source)
    )
}

@Composable
private fun limitLabel(tokens: Long?): String = if (tokens == null) {
    stringResource(R.string.context_diag_unknown_count)
} else {
    stringResource(
        R.string.context_diag_count_with_source,
        tokens.toString(),
        stringResource(R.string.context_diag_source_config)
    )
}

@Composable
private fun tokenSourceLabel(source: ContextTokenSource): String = stringResource(
    when (source) {
        ContextTokenSource.ESTIMATE -> R.string.context_diag_source_estimate
        ContextTokenSource.LOCAL_TOKENIZER -> R.string.context_diag_source_local_tokenizer
        ContextTokenSource.SERVER -> R.string.context_diag_source_server
    }
)

@Composable
private fun aggregateSourceLabel(aggregate: ContextSectionAggregate): String = when {
    aggregate.unknownSectionCount > 0 -> stringResource(
        R.string.context_diag_source_partial,
        (aggregate.sectionCount - aggregate.unknownSectionCount).toString(),
        aggregate.sectionCount.toString()
    )
    aggregate.sources.size > 1 -> stringResource(R.string.context_diag_source_mixed)
    aggregate.sources == setOf(ContextTokenSource.SERVER) -> stringResource(R.string.context_diag_source_server)
    ContextTokenSource.LOCAL_TOKENIZER in aggregate.sources ->
        stringResource(R.string.context_diag_source_local_tokenizer)
    else -> stringResource(R.string.context_diag_source_estimate)
}

@Composable
private fun aggregateTokensLabel(aggregate: ContextSectionAggregate): String =
    if (aggregate.unknownSectionCount == aggregate.sectionCount) {
        stringResource(R.string.context_diag_unknown_count)
    } else stringResource(R.string.context_diag_aggregate_tokens, aggregate.recordedTokens.toString())

@Composable
private fun aggregateSpanLabel(aggregate: ContextSectionAggregate): String {
    val firstIndex = aggregate.firstIndex
    val lastIndex = aggregate.lastIndex
    if (firstIndex == null || lastIndex == null) {
        return stringResource(R.string.context_diag_aggregate_no_index)
    }
    return stringResource(
        R.string.context_diag_aggregate_span,
        firstIndex.toString(),
        lastIndex.toString(),
        (lastIndex - firstIndex + 1).toString()
    )
}

@Composable
private fun compressionReasonLabel(reason: String): String = when (reason) {
    "manual" -> stringResource(R.string.context_diag_reason_manual)
    "tool_token_limit" -> stringResource(R.string.context_diag_reason_tool_limit)
    "send_threshold" -> stringResource(R.string.context_diag_reason_send)
    "automatic_threshold" -> stringResource(R.string.context_diag_reason_automatic)
    else -> reason
}

@Composable
private fun kindLabel(kind: ContextSectionKind): String = stringResource(
    when (kind) {
        ContextSectionKind.CHARACTER -> R.string.context_diag_kind_character
        ContextSectionKind.SYSTEM -> R.string.context_diag_kind_system
        ContextSectionKind.TOOL_DEFINITIONS -> R.string.context_diag_kind_tool_definitions
        ContextSectionKind.USER -> R.string.context_diag_kind_user
        ContextSectionKind.ASSISTANT -> R.string.context_diag_kind_assistant
        ContextSectionKind.TOOL_RESULTS -> R.string.context_diag_kind_tool_results
        ContextSectionKind.SUMMARY -> R.string.context_diag_kind_summary
        ContextSectionKind.OTHER -> R.string.context_diag_kind_other
    }
)
