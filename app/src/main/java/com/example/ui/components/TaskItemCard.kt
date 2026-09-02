package com.example.ui.components

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskPulseItem
import com.example.ui.theme.CategoryUrgent
import com.example.ui.theme.CategoryUrgentBg
import com.example.ui.theme.CategoryUrgentBorder
import com.example.ui.theme.ElegantBorder
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.ElegantPrimaryContainer
import com.example.ui.theme.ElegantSurface
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

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
    val context = LocalContext.current
    val isDone = item.isCompleted || (item.isBill && item.isPaid)
    val isOverdue = item.isOverdue()
    val categoryColor = getCategoryColor(item.taskCategory)

    // Animated transitions for completion effect
    val cardAlpha by animateFloatAsState(
        targetValue = if (isDone) 0.58f else 1f,
        animationSpec = tween(durationMillis = 300),
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
        animationSpec = tween(durationMillis = 280),
        label = "title_color"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = if (isOverdue && !isDone) StatusDanger.copy(alpha = 0.6f)
        else if (isDone) ElegantBorder.copy(alpha = 0.4f)
        else ElegantBorder,
        animationSpec = tween(durationMillis = 280),
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
            containerColor = ElegantSurface
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
                        if (isDone) ElegantBorder.copy(alpha = 0.5f)
                        else if (isOverdue) StatusDanger
                        else getPriorityAccentColor(item.taskPriority, categoryColor)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Top Row: Category badge, Visual Priority Badge (High, Med, Low, Urgent), Recurring, Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category Pill
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
                                Text(text = getCategoryEmoji(item.taskCategory), fontSize = 11.sp)
                                Text(
                                    text = item.taskCategory.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = categoryColor
                                )
                            }
                        }

                        // Priority Badge (High, Medium, Low, Urgent)
                        PriorityBadge(
                            priority = item.taskPriority,
                            isCompleted = isDone
                        )

                        // Recurring badge
                        if (item.isRecurring) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ElegantBorder.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = item.recurringInterval ?: "Repeat",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("task_menu_button_${item.id}")
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
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(ElegantSurface)
                        ) {
                            if (!isDone && (item.reminderTime != null || item.dueDate != null)) {
                                DropdownMenuItem(
                                    text = { Text("Snooze 15 minutes", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        menuExpanded = false
                                        onSnooze(item, 15)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null, tint = ElegantPrimary) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Snooze 1 hour", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        menuExpanded = false
                                        onSnooze(item, 60)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null, tint = ElegantPrimary) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Edit Details", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit(item)
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                            DropdownMenuItem(
                                text = { Text("Open in Calendar", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    menuExpanded = false
                                    try {
                                        val intent = com.example.TaskPulseApp.instance.googleCalendarSyncService.createViewCalendarIntent(item.dueDate ?: System.currentTimeMillis())
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = ElegantPrimary) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = StatusDanger) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete(item)
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = StatusDanger) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Middle Row: Checkbox or Pay button with pop animation + Title with animated strikethrough
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isBill) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                }
                                .clip(CircleShape)
                                .background(if (item.isPaid) StatusSuccess else ElegantBorder.copy(alpha = 0.5f))
                                .clickable { onToggleStatus(item) }
                                .testTag("toggle_bill_paid_${item.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.isPaid) Icons.Default.Check else Icons.Default.Paid,
                                contentDescription = if (item.isPaid) "Paid" else "Mark Paid",
                                tint = if (item.isPaid) Color(0xFF0F172A) else ElegantPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                scaleX = checkScale
                                scaleY = checkScale
                            }
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { onToggleStatus(item) },
                                modifier = Modifier.testTag("toggle_task_complete_${item.id}"),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ElegantPrimary,
                                    checkmarkColor = Color(0xFF1C1B1F),
                                    uncheckedColor = ElegantBorder
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
                                color = if (item.isPaid) StatusSuccess else ElegantPrimary
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
                            color = if (isOverdue && !isDone) CategoryUrgentBg else ElegantBorder.copy(alpha = 0.35f),
                            border = if (isOverdue && !isDone) BorderStroke(1.dp, CategoryUrgentBorder) else null
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
                                    tint = if (isOverdue && !isDone) CategoryUrgent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isOverdue && !isDone) "Overdue • $dueDateStr" else "Due $dueDateStr",
                                    fontSize = 11.sp,
                                    fontWeight = if (isOverdue && !isDone) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isOverdue && !isDone) CategoryUrgent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (item.reminderTime != null && item.reminderTime != item.dueDate) {
                        item.getFormattedReminderTime()?.let { reminderStr ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ElegantPrimaryContainer.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, ElegantPrimary.copy(alpha = 0.3f))
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
                                        tint = ElegantPrimary
                                    )
                                    Text(
                                        text = "Alert: $reminderStr",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ElegantPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Google Calendar Synced Badge
                    if (item.isSyncedToCalendar && (item.dueDate != null || item.reminderTime != null)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Google Calendar",
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

/**
 * Visual Priority Badge for High, Medium, Low, and Urgent priorities.
 */
@Composable
fun PriorityBadge(
    priority: TaskPriority,
    isCompleted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (label, iconEmoji, textColor, bgColor, borderColor) = when (priority) {
        TaskPriority.URGENT -> PriorityStyle(
            label = "URGENT",
            iconEmoji = "🔥",
            textColor = CategoryUrgent,
            bgColor = CategoryUrgentBg,
            borderColor = CategoryUrgentBorder
        )
        TaskPriority.HIGH -> PriorityStyle(
            label = "HIGH",
            iconEmoji = "⚡",
            textColor = Color(0xFFFBBF24),
            bgColor = Color(0xFF332308),
            borderColor = Color(0xFFF59E0B).copy(alpha = 0.6f)
        )
        TaskPriority.MEDIUM -> PriorityStyle(
            label = "MED",
            iconEmoji = "🔹",
            textColor = Color(0xFF38BDF8),
            bgColor = Color(0xFF0C243B),
            borderColor = Color(0xFF38BDF8).copy(alpha = 0.5f)
        )
        TaskPriority.LOW -> PriorityStyle(
            label = "LOW",
            iconEmoji = "🟢",
            textColor = Color(0xFF34D399),
            bgColor = Color(0xFF0D291E),
            borderColor = Color(0xFF34D399).copy(alpha = 0.5f)
        )
    }

    Surface(
        modifier = modifier.testTag("priority_badge_${priority.name.lowercase()}"),
        shape = RoundedCornerShape(8.dp),
        color = if (isCompleted) bgColor.copy(alpha = 0.4f) else bgColor,
        border = BorderStroke(1.dp, if (isCompleted) borderColor.copy(alpha = 0.3f) else borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = iconEmoji, fontSize = 9.sp)
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCompleted) textColor.copy(alpha = 0.5f) else textColor
            )
        }
    }
}

private data class PriorityStyle(
    val label: String,
    val iconEmoji: String,
    val textColor: Color,
    val bgColor: Color,
    val borderColor: Color
)

private fun getPriorityAccentColor(priority: TaskPriority, defaultColor: Color): Color {
    return when (priority) {
        TaskPriority.URGENT -> CategoryUrgent
        TaskPriority.HIGH -> Color(0xFFF59E0B)
        TaskPriority.MEDIUM -> Color(0xFF38BDF8)
        TaskPriority.LOW -> Color(0xFF34D399)
    }
}

