package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.StatusDanger
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
    onOpenAuth: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour in 5..11 -> "Good Morning"
        currentHour in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val firstName = currentUser.displayName.split(" ").firstOrNull() ?: "User"
    val avatarInitial = currentUser.displayName.firstOrNull()?.uppercase() ?: "U"
    var profileMenuExpanded by remember { mutableStateOf(false) }

    // Gentle pulse animation for the Hero Mic
    val infiniteTransition = rememberInfiniteTransition(label = "hero_mic_pulse")
    val heroPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar: Clean greeting on left & Minimal profile icon on top right with Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TASKPULSE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$greeting, $firstName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Minimalist Profile Avatar Button with Dropdown
            Box {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { profileMenuExpanded = true }
                        .testTag("header_avatar_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = avatarInitial,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Sync status indicator dot at bottom right corner of avatar
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(
                            when (syncState) {
                                is SyncState.Synced -> Color(0xFF10B981)
                                is SyncState.Syncing -> Color(0xFF38BDF8)
                                else -> Color(0xFFF59E0B)
                            }
                        )
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                )

                // Profile & Account Dropdown Menu
                DropdownMenu(
                    expanded = profileMenuExpanded,
                    onDismissRequest = { profileMenuExpanded = false },
                    modifier = Modifier.widthIn(min = 240.dp)
                ) {
                    // Header: User Profile Info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = avatarInitial,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = currentUser.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentUser.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sync Status Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (syncState) {
                                is SyncState.Synced -> Color(0xFF10B981).copy(alpha = 0.12f)
                                is SyncState.Syncing -> Color(0xFF38BDF8).copy(alpha = 0.12f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = when (syncState) {
                                        is SyncState.Synced -> Icons.Default.CloudDone
                                        is SyncState.Syncing -> Icons.Default.CloudSync
                                        else -> Icons.Default.CloudOff
                                    },
                                    contentDescription = null,
                                    tint = when (syncState) {
                                        is SyncState.Synced -> Color(0xFF10B981)
                                        is SyncState.Syncing -> Color(0xFF38BDF8)
                                        else -> Color(0xFFF59E0B)
                                    },
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = when (syncState) {
                                        is SyncState.Synced -> "Cloud Synced"
                                        is SyncState.Syncing -> "Syncing Changes..."
                                        else -> "Local Mode"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (syncState) {
                                        is SyncState.Synced -> Color(0xFF10B981)
                                        is SyncState.Syncing -> Color(0xFF38BDF8)
                                        else -> Color(0xFFF59E0B)
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Action: Switch Account
                    DropdownMenuItem(
                        text = { Text("Switch Account") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            profileMenuExpanded = false
                            onProfileClick()
                        }
                    )

                    // Action: Add / Sign In Account
                    DropdownMenuItem(
                        text = { Text("Add / Sign In Account") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            profileMenuExpanded = false
                            onOpenAuth()
                        }
                    )

                    HorizontalDivider()

                    // Action: Sign Out
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Sign Out",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            profileMenuExpanded = false
                            onSignOut()
                        }
                    )
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mic Floating Glowing Circle with scale animation
                Box(
                    modifier = Modifier
                        .scale(heroPulseScale)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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
                icon = Icons.Default.Today,
                value = "${stats.dueTodayCount}",
                label = "Due Today",
                highlightColor = MaterialTheme.colorScheme.primary,
                containerBg = MaterialTheme.colorScheme.surface,
                onClick = { onCategoryClick(TaskCategory.PERSONAL) },
                testTag = "metric_due_today"
            )

            // Card 2: Urgent
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Warning,
                value = "${stats.urgentCount}",
                label = "Urgent",
                highlightColor = MaterialTheme.colorScheme.error,
                containerBg = MaterialTheme.colorScheme.surface,
                borderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                onClick = { onCategoryClick(TaskCategory.URGENT) },
                testTag = "metric_urgent"
            )

            // Card 3: Unpaid Bills
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AccountBalanceWallet,
                value = if (stats.unpaidBillsCount > 0) "$${String.format("%.0f", stats.unpaidBillsTotal)}" else "$0",
                label = "${stats.unpaidBillsCount} Unpaid",
                highlightColor = MaterialTheme.colorScheme.tertiary,
                containerBg = MaterialTheme.colorScheme.surface,
                onClick = { onCategoryClick(TaskCategory.BILLS) },
                testTag = "metric_bills"
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    highlightColor: Color,
    containerBg: Color = MaterialTheme.colorScheme.surface,
    borderStroke: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = highlightColor,
                    modifier = Modifier.size(16.dp)
                )
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
