package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.stats.TokenStatsDisplayUnit
import com.ai.assistance.operit.data.stats.formatTokenCount
import com.ai.assistance.operit.ui.features.chat.viewmodel.ChatViewModel
import java.util.Locale

/** Shared by Agent and Classic input layouts; no provider calls or credentials in the view. */
@Composable
fun ChatTokenMeter(viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    val window by viewModel.currentWindowSize.collectAsState()
    val windowInK by viewModel.maxWindowSizeInK.collectAsState()
    val inputTotal by viewModel.inputTokenCount.collectAsState()
    val outputTotal by viewModel.outputTokenCount.collectAsState()
    val request by viewModel.requestTokenUsage.collectAsState()
    var showDetails by remember { mutableStateOf(false) }
    val limit = (windowInK.toDouble() * 1024.0).toLong().coerceAtLeast(0L)
    val progress = if (limit > 0L) (window.toDouble() / limit).coerceIn(0.0, 1.0).toFloat() else 0f
    val unknown = stringResource(R.string.chat_meter_unknown)
    val inputText = request?.let { usage ->
        usage.confirmedInputTokens?.let(::compactTokenCount)
            ?: stringResource(R.string.chat_meter_estimate, compactTokenCount(usage.estimatedInputTokens))
    } ?: unknown
    val outputText = request?.let { usage ->
        usage.confirmedOutputTokens?.let(::compactTokenCount)
            ?: stringResource(R.string.chat_meter_estimate, compactTokenCount(usage.estimatedOutputTokens))
    } ?: unknown
    val cacheText = request?.cacheHitPercent?.let { String.format(Locale.getDefault(), "%.0f%%", it) } ?: unknown
    val contextText = stringResource(
        R.string.chat_meter_context,
        compactTokenCount(window.coerceAtLeast(0L)),
        if (limit > 0L) compactTokenCount(limit) else unknown,
    )

    Surface(
        modifier = modifier.fillMaxWidth().clickable { showDetails = true },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = if (progress >= 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(contextText, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Text(stringResource(R.string.chat_meter_input, inputText), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Text(stringResource(R.string.chat_meter_output, outputText), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Text(stringResource(R.string.chat_meter_cache, cacheText), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text(stringResource(R.string.chat_meter_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(contextText)
                    Text(stringResource(R.string.chat_meter_input, inputText))
                    Text(stringResource(R.string.chat_meter_output, outputText))
                    Text(stringResource(R.string.chat_meter_cache, cacheText))
                    Text(stringResource(R.string.input_tokens, inputTotal))
                    Text(stringResource(R.string.output_tokens, outputTotal))
                    Text(stringResource(R.string.chat_meter_explanation), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) { Text(stringResource(R.string.chat_meter_close)) }
            },
        )
    }
}

private fun compactTokenCount(value: Long): String = formatTokenCount(value, TokenStatsDisplayUnit.MILLIONS)
