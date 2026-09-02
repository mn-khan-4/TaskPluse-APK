package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TaskPulseApp
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskPulseItem
import com.example.data.model.TaskType
import com.example.ui.theme.CategoryUrgent
import com.example.ui.theme.CategoryUrgentBg
import com.example.ui.theme.CategoryUrgentBorder
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskItemCard(
    item: TaskPulseItem,
    onToggleStatus: (TaskPulseItem) -> Unit,
    onEdit: (TaskPulseItem) -> Unit,
    onDelete: (TaskPulseItem) -> Unit,
    onSnooze: (TaskPulseItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isDone = item.isCompleted || (item.isBill && item.isPaid)
    val isOverdue = item.isOverdue()
    val categoryColor = getCategoryColor(item.taskCategory)

    // Animated transitions for interactive completion effect
    val cardAlpha by animateFloatAsState(
        targetValue = if (isDone) 0.6f else 1f,
        animationSpec = tween(durationMillis = 280),
        label = "card_alpha"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (isDone) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "check_scale"
    )

    val strikethroughProgress by animateFloatAsState(
        targetValue = if (isDone) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "strikethrough_progress"
    )

    val titleColor by animateColorAsState(
        targetValue = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 250),
        label = "title_color"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = if (isOverdue && !isDone) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        else if (isDone) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 250),
        label = "border_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .alpha(cardAlpha)
            .testTag("task_item_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = cardBorderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Accent Stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        if (isDone) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        else if (isOverdue) MaterialTheme.colorScheme.error
                        else categoryColor
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Top Row: Category Tag + Priority Badge + Overflow Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category pill with Icon
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = getCategoryBgColor(item.taskCategory),
                            border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(item.taskCategory),
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = item.taskCategory.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = categoryColor
                                )
                            }
                        }

                        // Priority Badge
                        PriorityBadge(
                            priority = item.taskPriority,
                            isCompleted = isDone
                        )

                        // Recurring Chip
                        if (item.isRecurring) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = (item.recurringInterval ?: "Recurring").lowercase().replaceFirstChar { it.uppercase() },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Google Calendar Synced Badge
                        if (item.isSyncedToCalendar) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = "Google Calendar Synced",
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Calendar",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Options Dropdown Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("task_menu_btn_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit(item)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Snooze 15 min") },
                                leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onSnooze(item, 15)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Snooze 1 hour") },
                                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onSnooze(item, 60)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sync to Google Calendar") },
                                leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    coroutineScope.launch {
                                        TaskPulseApp.instance.repository.syncSingleItemToGoogleCalendar(item)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = StatusDanger) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete(item)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Middle Row: Interactive Checkbox / Paid Action + Title & Description + Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox / Paid button with scale animation
                    Box(
                        modifier = Modifier
                            .scale(checkScale)
                            .testTag("checkbox_${item.id}")
                    ) {
                        if (item.isBill) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (item.isPaid) StatusSuccess else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (item.isPaid) StatusSuccess else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleStatus(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isPaid) Icons.Default.Check else Icons.Default.Paid,
                                        contentDescription = if (item.isPaid) "Paid" else "Mark Paid",
                                        tint = if (item.isPaid) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (item.isPaid) "PAID" else "PAY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isPaid) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { onToggleStatus(item) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                    uncheckedColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // Title with dynamic animated strikethrough transition effect
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
                            color = titleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.drawWithContent {
                                drawContent()
                                if (strikethroughProgress > 0f) {
                                    val strokeWidth = 2.dp.toPx()
                                    val y = size.height / 2f
                                    drawLine(
                                        color = titleColor.copy(alpha = 0.85f),
                                        start = Offset(0f, y),
                                        end = Offset(size.width * strikethroughProgress, y),
                                        strokeWidth = strokeWidth
                                    )
                                }
                            }
                        )

                        if (item.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = if (isDone) 0.5f else 1f
                                ),
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Bill Amount Display
                    if (item.isBill && item.amount != null && item.amount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = item.getFormattedAmount(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isPaid) StatusSuccess else MaterialTheme.colorScheme.primary
                            )
                            if (item.billPayee?.isNotBlank() == true) {
                                Text(
                                    text = item.billPayee,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Badges: Due Date & Reminder Time
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.getFormattedDueDate()?.let { dueDateStr ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOverdue && !isDone) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isOverdue && !isDone) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isOverdue && !isDone) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isOverdue && !isDone) "Overdue • $dueDateStr" else "Due $dueDateStr",
                                    fontSize = 11.sp,
                                    fontWeight = if (isOverdue && !isDone) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isOverdue && !isDone) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (item.reminderTime != null && item.reminderTime != item.dueDate) {
                        item.getFormattedReminderTime()?.let { reminderStr ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Alert: $reminderStr",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual Priority Badge for High, Medium, Low, and Urgent priorities.
 */
@Composable
fun PriorityBadge(
    priority: TaskPriority,
    isCompleted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (label, iconVector, textColor, bgColor, borderColor) = when (priority) {
        TaskPriority.URGENT -> PriorityIconStyle(
            label = "URGENT",
            icon = Icons.Default.Warning,
            textColor = MaterialTheme.colorScheme.error,
            bgColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        )
        TaskPriority.HIGH -> PriorityIconStyle(
            label = "HIGH",
            icon = Icons.Default.Bolt,
            textColor = Color(0xFFD97706),
            bgColor = Color(0xFFFEF3C7).copy(alpha = 0.7f),
            borderColor = Color(0xFFF59E0B).copy(alpha = 0.6f)
        )
        TaskPriority.MEDIUM -> PriorityIconStyle(
            label = "MID",
            icon = Icons.Default.Flag,
            textColor = Color(0xFF0284C7),
            bgColor = Color(0xFFE0F2FE).copy(alpha = 0.7f),
            borderColor = Color(0xFF38BDF8).copy(alpha = 0.5f)
        )
        TaskPriority.LOW -> PriorityIconStyle(
            label = "LOW",
            icon = Icons.Default.ArrowDownward,
            textColor = Color(0xFF059669),
            bgColor = Color(0xFFD1FAE5).copy(alpha = 0.7f),
            borderColor = Color(0xFF34D399).copy(alpha = 0.5f)
        )
    }

    Surface(
        modifier = modifier.testTag("priority_badge_${priority.name.lowercase()}"),
        shape = RoundedCornerShape(8.dp),
        color = if (isCompleted) bgColor.copy(alpha = 0.3f) else bgColor,
        border = BorderStroke(1.dp, if (isCompleted) borderColor.copy(alpha = 0.25f) else borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = if (isCompleted) textColor.copy(alpha = 0.5f) else textColor,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCompleted) textColor.copy(alpha = 0.5f) else textColor
            )
        }
    }
}

private data class PriorityIconStyle(
    val label: String,
    val icon: ImageVector,
    val textColor: Color,
    val bgColor: Color,
    val borderColor: Color
)
