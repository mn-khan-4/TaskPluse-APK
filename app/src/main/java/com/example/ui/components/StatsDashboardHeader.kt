package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SyncLock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskCategory
import com.example.data.model.UserAccount
import com.example.data.remote.SyncState
import com.example.ui.theme.CategoryUrgent
import com.example.ui.theme.CategoryUrgentBg
import com.example.ui.theme.CategoryUrgentBorder
import com.example.ui.theme.CategoryWorkBg
import com.example.ui.viewmodel.TaskStats
import java.util.Calendar

@Composable
fun StatsDashboardHeader(
    stats: TaskStats,
    currentUser: UserAccount = UserAccount.DEFAULT_DEMO_USERS.first(),
    syncState: SyncState = SyncState.Synced,
    onTapToSpeak: () -> Unit,
    onCategoryClick: (TaskCategory) -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour in 5..11 -> "Good Morning"
        currentHour in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val firstName = currentUser.displayName.split(" ").firstOrNull() ?: "User"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar: Assistant label & Greeting + User Switch Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TASKPULSE AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Cloud / Scoped Sync Indicator Chip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (syncState) {
                            is SyncState.Synced -> Color(0xFF10B981).copy(alpha = 0.18f)
                            is SyncState.Syncing -> Color(0xFF38BDF8).copy(alpha = 0.18f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (syncState) {
                                            is SyncState.Synced -> Color(0xFF10B981)
                                            is SyncState.Syncing -> Color(0xFF38BDF8)
                                            else -> Color(0xFFF59E0B)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (syncState) {
                                    is SyncState.Synced -> "Cloud"
                                    is SyncState.Syncing -> "Syncing"
                                    else -> "Private DB"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (syncState) {
                                    is SyncState.Synced -> Color(0xFF10B981)
                                    is SyncState.Syncing -> Color(0xFF38BDF8)
                                    else -> Color(0xFFF59E0B)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$greeting, $firstName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // User Profile Avatar & Switch Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onProfileClick() }
                    .testTag("header_avatar_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser.avatarEmoji,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = firstName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Switch ▾",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Prominent Tap-to-Speak Hero Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { onTapToSpeak() }
                .testTag("tap_to_speak_hero_card"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mic Floating Glowing Circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Tap to Speak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "\"Pay electric bill $85 next Monday at 5pm\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Realtime Metric Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Due Today
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = "⏰",
                value = "${stats.dueTodayCount}",
                label = "Due Today",
                highlightColor = MaterialTheme.colorScheme.primary,
                onClick = { onCategoryClick(TaskCategory.PERSONAL) },
                testTag = "metric_due_today"
            )

            // Card 2: Urgent
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = "🔥",
                value = "${stats.urgentCount}",
                label = "Urgent",
                highlightColor = CategoryUrgent,
                containerBg = CategoryUrgentBg,
                borderStroke = BorderStroke(1.dp, CategoryUrgentBorder),
                onClick = { onCategoryClick(TaskCategory.URGENT) },
                testTag = "metric_urgent"
            )

            // Card 3: Unpaid Bills
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = "💰",
                value = if (stats.unpaidBillsCount > 0) "$${String.format("%.0f", stats.unpaidBillsTotal)}" else "$0",
                label = "${stats.unpaidBillsCount} Unpaid",
                highlightColor = MaterialTheme.colorScheme.secondary,
                containerBg = CategoryWorkBg,
                onClick = { onCategoryClick(TaskCategory.BILLS) },
                testTag = "metric_bills"
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String,
    highlightColor: Color,
    containerBg: Color = MaterialTheme.colorScheme.surface,
    borderStroke: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = containerBg,
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = icon, fontSize = 14.sp)
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = highlightColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
