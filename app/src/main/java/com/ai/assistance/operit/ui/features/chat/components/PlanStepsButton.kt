package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.model.PlanStep
import com.ai.assistance.operit.data.model.PlanStepStatus

/**
 * Plan-steps affordance for the chat header, placed immediately left of the token gauge.
 *
 * Collapsed it shows a checklist icon and a completed/total counter, mirroring the collapsed row of
 * ZCode's todo block. Expanded it lists every step. The in-progress step uses a static arrow rather
 * than an animated indicator so "currently working on" never reads as a loading spinner.
 */
@Composable
fun PlanStepsButton(steps: List<PlanStep>, sessionId: String?, modifier: Modifier = Modifier) {
    var expanded by remember(sessionId) { mutableStateOf(false) }
    val completedCount = steps.count { it.status == PlanStepStatus.COMPLETED }
    val activeStep =
        steps.firstOrNull { it.status == PlanStepStatus.IN_PROGRESS }
            ?: steps.firstOrNull { it.status != PlanStepStatus.COMPLETED }

    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .heightIn(min = 32.dp)
                    .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Checklist,
                contentDescription = stringResource(R.string.plan_steps_title),
                tint =
                    when {
                        steps.isEmpty() ->
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        activeStep != null -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                modifier = Modifier.size(18.dp)
            )
            if (steps.isNotEmpty()) {
                Text(
                    text = "$completedCount/${steps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 240.dp, max = 320.dp)
                .heightIn(max = 360.dp).background(MaterialTheme.colorScheme.surface)
        ) {
            if (steps.isEmpty()) {
                Text(
                    text = stringResource(R.string.plan_steps_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else {
                val firstActive =
                    steps.firstOrNull { it.status == PlanStepStatus.IN_PROGRESS }
                        ?: steps.firstOrNull { it.status != PlanStepStatus.COMPLETED }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text =
                            stringResource(
                                R.string.plan_steps_progress,
                                completedCount,
                                steps.size
                            ),
                        style =
                            MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (firstActive != null) {
                        Text(
                            text =
                                stringResource(
                                    R.string.plan_steps_current,
                                    firstActive.content
                                ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                HorizontalDivider()
                Column {
                    steps.forEach { PlanStepRow(it) }
                }
            }
        }
    }
}

@Composable
private fun PlanStepRow(step: PlanStep) {
    val icon =
        when (step.status) {
            PlanStepStatus.COMPLETED -> Icons.Default.CheckCircle
            PlanStepStatus.IN_PROGRESS -> Icons.AutoMirrored.Filled.ArrowRight
            PlanStepStatus.PENDING -> Icons.Default.RadioButtonUnchecked
        }
    val tint =
        when (step.status) {
            PlanStepStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
            PlanStepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
            PlanStepStatus.PENDING ->
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }
    val textColor =
        when (step.status) {
            PlanStepStatus.COMPLETED ->
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            PlanStepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onSurface
            PlanStepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(
            text = step.content,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    textDecoration =
                        if (step.status == PlanStepStatus.COMPLETED) TextDecoration.LineThrough
                        else null
                ),
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}