package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskPulseItem
import com.example.ui.components.TaskItemCard
import com.example.ui.viewmodel.TaskPulseViewModel
import java.util.Calendar

@Composable
fun ScheduleScreen(
    viewModel: TaskPulseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allItems by viewModel.rawItems.collectAsState()
    val calendarPrefs by viewModel.calendarPrefs.collectAsState()
    var filterMode by remember { mutableStateOf(0) } // 0: All Scheduled, 1: Pending Only, 2: Bills Only

    // Filter items with dates
    val scheduledItems = remember(allItems, filterMode) {
        allItems.filter { item ->
            val hasDate = item.dueDate != null || item.reminderTime != null
            if (!hasDate) return@filter false
            when (filterMode) {
                1 -> !item.isCompleted && !item.isPaid
                2 -> item.isBill
                else -> true
            }
        }.sortedBy { it.dueDate ?: it.reminderTime ?: Long.MAX_VALUE }
    }

    // Group items into Timeline buckets
    val now = System.currentTimeMillis()
    val todayCal = Calendar.getInstance()
    val todayDay = todayCal.get(Calendar.DAY_OF_YEAR)
    val todayYear = todayCal.get(Calendar.YEAR)

    val overdueItems = mutableListOf<TaskPulseItem>()
    val todayItems = mutableListOf<TaskPulseItem>()
    val tomorrowItems = mutableListOf<TaskPulseItem>()
    val thisWeekItems = mutableListOf<TaskPulseItem>()
    val laterItems = mutableListOf<TaskPulseItem>()

    scheduledItems.forEach { item ->
        val target = item.dueDate ?: item.reminderTime ?: return@forEach
        val isDone = item.isCompleted || (item.isBill && item.isPaid)

        if (target < now && !isDone) {
            overdueItems.add(item)
        } else {
            val itemCal = Calendar.getInstance().apply { timeInMillis = target }
            val itemYear = itemCal.get(Calendar.YEAR)
            val itemDay = itemCal.get(Calendar.DAY_OF_YEAR)

            if (itemYear == todayYear && itemDay == todayDay) {
                todayItems.add(item)
            } else if (itemYear == todayYear && itemDay == todayDay + 1) {
                tomorrowItems.add(item)
            } else if (target < now + (7 * 24 * 3600 * 1000L)) {
                thisWeekItems.add(item)
            } else {
                laterItems.add(item)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Schedule & Deadlines",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track approaching due dates & scheduled reminders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                listOf(
                    0 to "All Scheduled",
                    1 to "Pending",
                    2 to "Bills Due"
                ).forEach { (mode, label) ->
                    val isSelected = filterMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { filterMode = mode },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("schedule_filter_$mode")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Google Calendar Sync Quick Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("schedule_google_calendar_banner"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (calendarPrefs.isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Google Calendar",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (calendarPrefs.isConnected) "Auto-syncing to ${calendarPrefs.calendarEmail}" else "Connect to auto-sync reminders",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (calendarPrefs.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (calendarPrefs.isConnected) {
                        Button(
                            onClick = { viewModel.syncAllToGoogleCalendar() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("btn_quick_sync_calendar")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sync All",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.connectGoogleCalendar() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("btn_quick_connect_calendar")
                        ) {
                            Text(
                                text = "Connect",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ================= OVERDUE SECTION =================
        if (overdueItems.isNotEmpty()) {
            item {
                TimelineSectionHeader(
                    title = "Overdue & Urgent Attention",
                    count = overdueItems.size,
                    isAlert = true
                )
            }
            items(overdueItems, key = { "overdue_${it.id}" }) { item ->
                TaskItemCard(
                    item = item,
                    onToggleStatus = { toggled ->
                        if (toggled.isBill) viewModel.togglePaid(toggled) else viewModel.toggleCompleted(toggled)
                    },
                    onEdit = { viewModel.openAddEdit(it) },
                    onDelete = { viewModel.deleteItem(it) },
                    onSnooze = { itm, min -> viewModel.snoozeItem(itm, min) }
                )
            }
        }

        // ================= TODAY SECTION =================
        if (todayItems.isNotEmpty()) {
            item {
                TimelineSectionHeader(
                    title = "Due Today",
                    count = todayItems.size
                )
            }
            items(todayItems, key = { "today_${it.id}" }) { item ->
                TaskItemCard(
                    item = item,
                    onToggleStatus = { toggled ->
                        if (toggled.isBill) viewModel.togglePaid(toggled) else viewModel.toggleCompleted(toggled)
                    },
                    onEdit = { viewModel.openAddEdit(it) },
                    onDelete = { viewModel.deleteItem(it) },
                    onSnooze = { itm, min -> viewModel.snoozeItem(itm, min) }
                )
            }
        }

        // ================= TOMORROW SECTION =================
        if (tomorrowItems.isNotEmpty()) {
            item {
                TimelineSectionHeader(
                    title = "Due Tomorrow",
                    count = tomorrowItems.size
                )
            }
            items(tomorrowItems, key = { "tomorrow_${it.id}" }) { item ->
                TaskItemCard(
                    item = item,
                    onToggleStatus = { toggled ->
                        if (toggled.isBill) viewModel.togglePaid(toggled) else viewModel.toggleCompleted(toggled)
                    },
                    onEdit = { viewModel.openAddEdit(it) },
                    onDelete = { viewModel.deleteItem(it) },
                    onSnooze = { itm, min -> viewModel.snoozeItem(itm, min) }
                )
            }
        }

        // ================= THIS WEEK SECTION =================
        if (thisWeekItems.isNotEmpty()) {
            item {
                TimelineSectionHeader(
                    title = "Upcoming This Week",
                    count = thisWeekItems.size
                )
            }
            items(thisWeekItems, key = { "week_${it.id}" }) { item ->
                TaskItemCard(
                    item = item,
                    onToggleStatus = { toggled ->
                        if (toggled.isBill) viewModel.togglePaid(toggled) else viewModel.toggleCompleted(toggled)
                    },
                    onEdit = { viewModel.openAddEdit(it) },
                    onDelete = { viewModel.deleteItem(it) },
                    onSnooze = { itm, min -> viewModel.snoozeItem(itm, min) }
                )
            }
        }

        // ================= LATER SECTION =================
        if (laterItems.isNotEmpty()) {
            item {
                TimelineSectionHeader(
                    title = "Later & Future",
                    count = laterItems.size
                )
            }
            items(laterItems, key = { "later_${it.id}" }) { item ->
                TaskItemCard(
                    item = item,
                    onToggleStatus = { toggled ->
                        if (toggled.isBill) viewModel.togglePaid(toggled) else viewModel.toggleCompleted(toggled)
                    },
                    onEdit = { viewModel.openAddEdit(it) },
                    onDelete = { viewModel.deleteItem(it) },
                    onSnooze = { itm, min -> viewModel.snoozeItem(itm, min) }
                )
            }
        }

        if (scheduledItems.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Scheduled Deadlines",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add tasks or bills with due dates to see your timeline here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineSectionHeader(
    title: String,
    count: Int,
    isAlert: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isAlert) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            )
        }

        Surface(
            shape = CircleShape,
            color = if (isAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "$count",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
            )
        }
    }
}
