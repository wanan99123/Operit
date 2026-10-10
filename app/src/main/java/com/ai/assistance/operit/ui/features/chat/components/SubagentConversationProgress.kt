package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.stats.SubagentProgress

/** Current-round projection in the conversation, independent of raw child/tool-result markup. */
@Composable
internal fun SubagentConversationProgress(
    agents: List<SubagentProgress>,
    modifier: Modifier = Modifier,
) {
    if (agents.isEmpty()) return
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        agents.forEach { agent ->
            key(agent.agentId) {
                val presentation = SubagentConversationStatus.fromStatus(agent.status)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubagentStatusCircle(presentation)
                    Column(Modifier.weight(1f)) {
                        Text(agent.description, style = MaterialTheme.typography.bodySmall)
                        val status = stringResource(when (presentation) {
                            SubagentConversationStatus.COMPLETED -> R.string.plan_agent_completed
                            SubagentConversationStatus.RUNNING -> R.string.plan_agent_running
                            SubagentConversationStatus.RETRYING -> R.string.plan_agent_retrying
                            SubagentConversationStatus.FAILED -> R.string.plan_agent_failed
                            SubagentConversationStatus.TIMED_OUT -> R.string.plan_agent_timed_out
                            SubagentConversationStatus.CANCELLED -> R.string.plan_agent_cancelled
                        })
                        Text(text = status + if (agent.attempt > 1L || presentation == SubagentConversationStatus.RETRYING)
                            " · " + stringResource(R.string.plan_agent_attempt, agent.attempt) else "",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (presentation == SubagentConversationStatus.RUNNING) {
                            agent.tools.lastOrNull { !it.isTerminal }?.let {
                                Text(it.name, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Thin, background-free status ring shared by the conversation and input details. */
@Composable
internal fun SubagentStatusCircle(
    status: SubagentConversationStatus,
    modifier: Modifier = Modifier,
) {
    val color = when (status) {
        SubagentConversationStatus.COMPLETED -> Color(0xFF34C759)
        SubagentConversationStatus.RUNNING, SubagentConversationStatus.RETRYING -> MaterialTheme.colorScheme.onSurface
        SubagentConversationStatus.FAILED, SubagentConversationStatus.TIMED_OUT -> MaterialTheme.colorScheme.error
        SubagentConversationStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(modifier.size(12.dp).then(
        if (status == SubagentConversationStatus.COMPLETED) {
            Modifier.background(color, CircleShape)
        } else {
            Modifier.border(1.5.dp, color, CircleShape)
        }
    ))
}
