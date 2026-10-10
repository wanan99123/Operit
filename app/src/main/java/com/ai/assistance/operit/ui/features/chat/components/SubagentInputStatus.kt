package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.stats.SubagentProgressStore

/** Input-only task projection; completed tasks remain available for inspection. */
@Composable
internal fun SubagentInputStatus(chatId: String?, modifier: Modifier = Modifier) {
    val sessions by SubagentProgressStore.sessions.collectAsState()
    var expanded by remember(chatId) { mutableStateOf(false) }
    val agents = sessions[chatId] ?: return
    if (agents.isEmpty()) return

    Row(
        modifier = modifier
            .clickable(role = Role.Button) { expanded = true }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        agents.take(3).forEach { agent ->
            SubagentStatusCircle(SubagentConversationStatus.fromStatus(agent.status))
        }
        Text(
            text = stringResource(R.string.chat_subagent_details_count, agents.size),
            style = MaterialTheme.typography.labelMedium,
        )
    }

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(stringResource(R.string.plan_subagents_title)) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(agents, key = { it.agentId }) { agent ->
                        val status = SubagentConversationStatus.fromStatus(agent.status)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.chat_subagent_task_name, agent.description),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SubagentStatusCircle(status)
                                Text(
                                    text = stringResource(when (status) {
                                        SubagentConversationStatus.RUNNING -> R.string.plan_agent_running
                                        SubagentConversationStatus.RETRYING -> R.string.plan_agent_retrying
                                        SubagentConversationStatus.COMPLETED -> R.string.plan_agent_completed
                                        SubagentConversationStatus.FAILED -> R.string.plan_agent_failed
                                        SubagentConversationStatus.TIMED_OUT -> R.string.plan_agent_timed_out
                                        SubagentConversationStatus.CANCELLED -> R.string.plan_agent_cancelled
                                    }),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                text = stringResource(R.string.chat_subagent_retry_count, agent.attempt - 1L),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (status == SubagentConversationStatus.RUNNING) {
                                agent.tools.lastOrNull { !it.isTerminal }?.let { tool ->
                                    Text(
                                        text = stringResource(R.string.chat_subagent_current_tool, tool.name),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}
