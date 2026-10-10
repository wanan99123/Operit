package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.stats.RequestPerformanceStore
import java.util.Locale

/** Header usage overview only; context diagnostic storage is unaffected. */
@Composable
fun ContextDiagnosticsDialog(
    chatId: String?,
    currentWindowSize: Long,
    inputTokens: Long,
    outputTokens: Long,
    onDismiss: () -> Unit,
) {
    val sessions by RequestPerformanceStore.sessions.collectAsState()
    val performance = chatId?.let { sessions[it] }
    val unrecorded = stringResource(R.string.context_diag_unknown_count)
    val speed = performance?.tokensPerSecond?.let { String.format(Locale.getDefault(), "%.1f", it) }
    val latency = performance?.firstTokenLatencyMs?.let { String.format(Locale.getDefault(), "%.2f", it / 1000.0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.context_diag_usage_overview)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.context_diag_close)) }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.context_window, currentWindowSize))
                Text(stringResource(R.string.input_tokens, inputTokens))
                Text(stringResource(R.string.output_tokens, outputTokens))
                Text(
                    text = stringResource(R.string.total_tokens, inputTokens + outputTokens),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.usage_token_rate, speed ?: unrecorded))
                Text(stringResource(R.string.usage_first_token_latency, latency ?: unrecorded))
                Text(
                    text = stringResource(R.string.usage_performance_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
