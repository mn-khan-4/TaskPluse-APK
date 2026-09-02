package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskPulseItem
import com.example.ui.components.AddEditTaskSheet
import com.example.ui.components.AuthDialog
import com.example.ui.components.CategoryFilterBar
import com.example.ui.components.StatsDashboardHeader
import com.example.ui.components.TaskItemCard
import com.example.ui.components.UserSwitchDialog
import com.example.ui.components.VoiceInputDialog
import com.example.ui.theme.CategoryUrgent
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.TaskPulseViewModel
import com.example.ui.viewmodel.TaskSortMode

@Composable
fun HomeScreen(
    viewModel: TaskPulseViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.filteredItems.collectAsState()
    val rawItems by viewModel.rawItems.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val knownUsers by viewModel.knownUsers.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val isAuthOpen by viewModel.isAuthDialogOpen.collectAsState()
    val isUserSwitchOpen by viewModel.isUserSwitchDialogOpen.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isVoiceDialogOpen by viewModel.isVoiceDialogOpen.collectAsState()
    val isAddEditSheetOpen by viewModel.isAddEditSheetOpen.collectAsState()
    val editingItem by viewModel.editingItem.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var activeNavTab by remember { mutableStateOf(0) } // 0: Home, 1: Schedule, 2: Vault, 3: Settings

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (activeNavTab != 3) {
                FloatingActionButton(
                    onClick = { viewModel.openAddEdit() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_add_task")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task or Bill")
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Home / Dashboard
                    val isHome = activeNavTab == 0
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { activeNavTab = 0 }
                            .padding(4.dp)
                            .testTag("nav_tab_home")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 30.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isHome) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🏠", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Home",
                            fontSize = 10.sp,
                            fontWeight = if (isHome) FontWeight.Bold else FontWeight.Medium,
                            color = if (isHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 2. Schedule
                    val isSchedule = activeNavTab == 1
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { activeNavTab = 1 }
                            .padding(4.dp)
                            .testTag("nav_tab_schedule")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 30.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSchedule) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🗓️", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Schedule",
                            fontSize = 10.sp,
                            fontWeight = if (isSchedule) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSchedule) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 3. Vault / Bills
                    val isVault = activeNavTab == 2
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { activeNavTab = 2 }
                            .padding(4.dp)
                            .testTag("nav_tab_vault")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 30.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isVault) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "💳", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Vault",
                            fontSize = 10.sp,
                            fontWeight = if (isVault) FontWeight.Bold else FontWeight.Medium,
                            color = if (isVault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 4. Settings & Profile
                    val isSettings = activeNavTab == 3
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { activeNavTab = 3 }
                            .padding(4.dp)
                            .testTag("nav_tab_settings")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 30.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSettings) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⚙️", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Settings",
                            fontSize = 10.sp,
                            fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 5. Voice Input Shortcut
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { viewModel.openVoiceDialog() }
                            .padding(4.dp)
                            .testTag("nav_tab_voice")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 30.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🎙️", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Voice",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (activeNavTab) {
            1 -> {
                ScheduleScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            2 -> {
                VaultScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            3 -> {
                SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                // 0: Main Home Dashboard
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // 1. Dashboard Header: Greeting, Hero Voice Mic Card, 3 Metric Cards
                    item {
                        StatsDashboardHeader(
                            stats = stats,
                            currentUser = currentUser,
                            syncState = syncState,
                            onTapToSpeak = { viewModel.openVoiceDialog() },
                            onCategoryClick = { cat -> viewModel.setSelectedCategory(cat) },
                            onProfileClick = { viewModel.openUserSwitchDialog() }
                        )
                    }

                    // 2. Search Bar: Prominent search input with title, description, and category filtering
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = {
                                    Text(
                                        text = "Search by title or category (e.g. Work, Bills)...",
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(
                                            onClick = { viewModel.clearSearch() },
                                            modifier = Modifier.testTag("clear_search_button")
                                        ) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_tasks_input"),
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }

                    // 3. Category Horizontal Filter Pills
                    item {
                        CategoryFilterBar(
                            selectedCategory = selectedCategory,
                            items = rawItems,
                            onCategorySelected = { viewModel.setSelectedCategory(it) }
                        )
                    }

                    // 4. Tab Selector (All, Tasks, Bills, Urgent)
                    item {
                        val tabTitles = listOf("All", "Tasks", "Bills", "Urgent")
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            tabTitles.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { viewModel.setSelectedTab(index) },
                                    text = {
                                        Text(
                                            text = title,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    modifier = Modifier.testTag("tab_$title")
                                )
                            }
                        }
                    }

                    // 5. Sorting Controls & Section Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "TASKS & REMINDERS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "${items.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Sort / Group Mode Chips
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TaskSortMode.entries.forEach { mode ->
                                    val isSelected = sortMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .clickable { viewModel.setSortMode(mode) }
                                            .testTag("sort_${mode.name.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(text = mode.icon, fontSize = 10.sp)
                                            Text(
                                                text = mode.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Items List (Grouped by Priority or Flat Chronological)
                    if (items.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "✨", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching tasks or categories" else "All caught up!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Try searching by a different title or category name." else "Tap the microphone above or + to create a reminder.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (sortMode == TaskSortMode.PRIORITY) {
                        // Group by Priority: Urgent, High, Medium, Low, and Completed
                        val activeItems = items.filter { !it.isCompleted && !(it.isBill && it.isPaid) }
                        val completedItems = items.filter { it.isCompleted || (it.isBill && it.isPaid) }

                        val urgentGroup = activeItems.filter { it.taskPriority == TaskPriority.URGENT }
                        val highGroup = activeItems.filter { it.taskPriority == TaskPriority.HIGH }
                        val mediumGroup = activeItems.filter { it.taskPriority == TaskPriority.MEDIUM }
                        val lowGroup = activeItems.filter { it.taskPriority == TaskPriority.LOW }

                        // 1. Urgent Group
                        if (urgentGroup.isNotEmpty()) {
                            item {
                                PrioritySectionHeader(
                                    title = "Urgent Priority",
                                    iconEmoji = "🔥",
                                    count = urgentGroup.size,
                                    accentColor = CategoryUrgent
                                )
                            }
                            items(urgentGroup, key = { it.id }) { item ->
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

                        // 2. High Priority Group
                        if (highGroup.isNotEmpty()) {
                            item {
                                PrioritySectionHeader(
                                    title = "High Priority",
                                    iconEmoji = "⚡",
                                    count = highGroup.size,
                                    accentColor = Color(0xFFF59E0B)
                                )
                            }
                            items(highGroup, key = { it.id }) { item ->
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

                        // 3. Medium Priority Group
                        if (mediumGroup.isNotEmpty()) {
                            item {
                                PrioritySectionHeader(
                                    title = "Medium Priority",
                                    iconEmoji = "🔹",
                                    count = mediumGroup.size,
                                    accentColor = Color(0xFF38BDF8)
                                )
                            }
                            items(mediumGroup, key = { it.id }) { item ->
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

                        // 4. Low Priority Group
                        if (lowGroup.isNotEmpty()) {
                            item {
                                PrioritySectionHeader(
                                    title = "Low Priority",
                                    iconEmoji = "🟢",
                                    count = lowGroup.size,
                                    accentColor = Color(0xFF34D399)
                                )
                            }
                            items(lowGroup, key = { it.id }) { item ->
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

                        // 5. Completed Items Group
                        if (completedItems.isNotEmpty()) {
                            item {
                                PrioritySectionHeader(
                                    title = "Completed",
                                    iconEmoji = "✓",
                                    count = completedItems.size,
                                    accentColor = StatusSuccess
                                )
                            }
                            items(completedItems, key = { it.id }) { item ->
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
                    } else {
                        // Regular Flat Stream (By Due Date or Newest)
                        items(items, key = { it.id }) { item ->
                            TaskItemCard(
                                item = item,
                                onToggleStatus = { toggled ->
                                    if (toggled.isBill) {
                                        viewModel.togglePaid(toggled)
                                    } else {
                                        viewModel.toggleCompleted(toggled)
                                    }
                                },
                                onEdit = { viewModel.openAddEdit(it) },
                                onDelete = { viewModel.deleteItem(it) },
                                onSnooze = { itm, min -> viewModel.snoozeItem(itm, min) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Auth Dialog Overlay
    AuthDialog(
        isOpen = isAuthOpen,
        isLoading = authLoading,
        onDismiss = { viewModel.closeAuthDialog() },
        onSignIn = { email, pass -> viewModel.signIn(email, pass) },
        onSignUp = { email, pass, name, role, emoji -> viewModel.signUp(email, pass, name, role, emoji) },
        onSwitchToDemoUser = { demoUser -> viewModel.switchUser(demoUser) }
    )

    // User Switch Dialog Overlay
    UserSwitchDialog(
        isOpen = isUserSwitchOpen,
        currentUser = currentUser,
        knownUsers = knownUsers,
        onDismiss = { viewModel.closeUserSwitchDialog() },
        onSelectUser = { selectedUser -> viewModel.switchUser(selectedUser) },
        onAddNewAccount = { viewModel.openAuthDialog() },
        onSignOut = { viewModel.signOut() }
    )

    // Voice Dialog Overlay
    if (isVoiceDialogOpen) {
        VoiceInputDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeVoiceDialog() }
        )
    }

    // Add/Edit Bottom Sheet
    if (isAddEditSheetOpen) {
        AddEditTaskSheet(
            itemToEdit = editingItem,
            onSave = { viewModel.saveItem(it) },
            onDismiss = { viewModel.closeAddEdit() }
        )
    }
}

@Composable
fun PrioritySectionHeader(
    title: String,
    iconEmoji: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = iconEmoji, fontSize = 14.sp)
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
        ) {
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
