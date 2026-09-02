package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    var activeNavTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Schedule, 2: Vault, 3: Settings

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearUserMessage()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp
        val isDualPane = maxWidth >= 840.dp
        val isCompactLandscape = maxHeight < 500.dp && maxWidth >= 500.dp
        val useNavRail = isWideScreen || isCompactLandscape

        if (useNavRail) {
            // Tablet, Foldable Unfolded, Desktop, or Landscape Phone Layout with NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "TaskPulse",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            FloatingActionButton(
                                onClick = { viewModel.openAddEdit() },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("fab_add_task")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Task or Bill")
                            }
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Home
                    NavigationRailItem(
                        selected = activeNavTab == 0,
                        onClick = { activeNavTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    // Schedule
                    NavigationRailItem(
                        selected = activeNavTab == 1,
                        onClick = { activeNavTab = 1 },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Schedule") },
                        label = { Text("Schedule") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_schedule")
                    )

                    // Vault
                    NavigationRailItem(
                        selected = activeNavTab == 2,
                        onClick = { activeNavTab = 2 },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Vault") },
                        label = { Text("Vault") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_vault")
                    )

                    // Settings
                    NavigationRailItem(
                        selected = activeNavTab == 3,
                        onClick = { activeNavTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_settings")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Voice Input Shortcut
                    NavigationRailItem(
                        selected = false,
                        onClick = { viewModel.openVoiceDialog() },
                        icon = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        label = { Text("Voice", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .testTag("nav_tab_voice")
                    )
                }

                // Main Content Pane
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (activeNavTab) {
                        1 -> ScheduleScreen(viewModel = viewModel)
                        2 -> VaultScreen(viewModel = viewModel)
                        3 -> SettingsScreen(viewModel = viewModel)
                        else -> {
                            if (isDualPane) {
                                // Side-by-Side Dual Pane for Tablets & Expanded Displays
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    // Left Pane: Interactive Stats & Category Filter
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        StatsDashboardHeader(
                                            stats = stats,
                                            currentUser = currentUser,
                                            syncState = syncState,
                                            onTapToSpeak = { viewModel.openVoiceDialog() },
                                            onCategoryClick = { cat -> viewModel.setSelectedCategory(cat) },
                                            onProfileClick = { viewModel.openUserSwitchDialog() },
                                            onOpenAuth = { viewModel.openAuthDialog() },
                                            onSignOut = { viewModel.signOut() }
                                        )

                                        Text(
                                            text = "Filter Categories",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        CategoryFilterBar(
                                            selectedCategory = selectedCategory,
                                            items = rawItems,
                                            onCategorySelected = { cat -> viewModel.setSelectedCategory(cat) }
                                        )
                                    }

                                    // Right Pane: Search, Tabs, and Task Items List
                                    Column(
                                        modifier = Modifier
                                            .weight(1.35f)
                                            .fillMaxHeight()
                                    ) {
                                        // Search Bar
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { viewModel.setSearchQuery(it) },
                                            placeholder = {
                                                Text(
                                                    "Search tasks, bills, payees...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = "Search",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                if (searchQuery.isNotEmpty()) {
                                                    IconButton(
                                                        onClick = { viewModel.setSearchQuery("") },
                                                        modifier = Modifier.testTag("btn_clear_search")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Clear,
                                                            contentDescription = "Clear search",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                                .testTag("search_text_input")
                                        )

                                        // Status Tabs
                                        val tabs = listOf("All", "Pending", "Bills", "Completed")
                                        TabRow(
                                            selectedTabIndex = selectedTab,
                                            containerColor = Color.Transparent,
                                            contentColor = MaterialTheme.colorScheme.primary,
                                            indicator = { tabPositions ->
                                                if (selectedTab < tabPositions.size) {
                                                    TabRowDefaults.SecondaryIndicator(
                                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        height = 3.dp
                                                    )
                                                }
                                            },
                                            divider = {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(1.dp)
                                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            tabs.forEachIndexed { index, title ->
                                                val isSelected = selectedTab == index
                                                Tab(
                                                    selected = isSelected,
                                                    onClick = { viewModel.setSelectedTab(index) },
                                                    text = {
                                                        Text(
                                                            text = title,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            fontSize = 13.sp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    },
                                                    modifier = Modifier.testTag("home_tab_$index")
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Header Bar with Sort Selection & Count
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${items.size} ${if (items.size == 1) "item" else "items"}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TaskSortMode.entries.forEach { mode ->
                                                    val isSelected = sortMode == mode
                                                    val sortIcon = when (mode) {
                                                        TaskSortMode.PRIORITY -> Icons.Default.PriorityHigh
                                                        TaskSortMode.DUE_DATE -> Icons.Default.Schedule
                                                        TaskSortMode.CREATED -> Icons.Default.FiberNew
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                        ),
                                                        modifier = Modifier
                                                            .clickable { viewModel.setSortMode(mode) }
                                                            .testTag("sort_${mode.name.lowercase()}")
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = sortIcon,
                                                                contentDescription = null,
                                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(12.dp)
                                                            )
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

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Task Cards Stream
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(bottom = 32.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            renderTaskListItems(
                                                items = items,
                                                searchQuery = searchQuery,
                                                sortMode = sortMode,
                                                viewModel = viewModel
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Single Column Centered for Medium Screens
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .widthIn(max = 720.dp),
                                        contentPadding = PaddingValues(bottom = 32.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        renderFullHomeSingleColumn(
                                            stats = stats,
                                            currentUser = currentUser,
                                            syncState = syncState,
                                            searchQuery = searchQuery,
                                            selectedCategory = selectedCategory,
                                            rawItems = rawItems,
                                            selectedTab = selectedTab,
                                            sortMode = sortMode,
                                            items = items,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        } else {
            // Compact Portrait Screen with Bottom Navigation Bar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
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
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("nav_tab_home")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 30.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isHome) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Home",
                                        tint = if (isHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Home",
                                    fontSize = 11.sp,
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
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("nav_tab_schedule")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 30.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSchedule) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Schedule",
                                        tint = if (isSchedule) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Schedule",
                                    fontSize = 11.sp,
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
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("nav_tab_vault")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 30.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isVault) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Vault",
                                        tint = if (isVault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Vault",
                                    fontSize = 11.sp,
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
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("nav_tab_settings")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 30.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSettings) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = if (isSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Settings",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 5. Voice Input Shortcut
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.openVoiceDialog() }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("nav_tab_voice")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 30.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Voice",
                                    fontSize = 11.sp,
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
                        // Main Dashboard Screen Single Column
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            renderFullHomeSingleColumn(
                                stats = stats,
                                currentUser = currentUser,
                                syncState = syncState,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                rawItems = rawItems,
                                selectedTab = selectedTab,
                                sortMode = sortMode,
                                items = items,
                                viewModel = viewModel
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

private fun androidx.compose.foundation.lazy.LazyListScope.renderFullHomeSingleColumn(
    stats: com.example.ui.viewmodel.TaskStats,
    currentUser: com.example.data.model.UserAccount,
    syncState: com.example.data.remote.SyncState,
    searchQuery: String,
    selectedCategory: TaskCategory?,
    rawItems: List<TaskPulseItem>,
    selectedTab: Int,
    sortMode: TaskSortMode,
    items: List<TaskPulseItem>,
    viewModel: TaskPulseViewModel
) {
    // 1. Interactive Stats & Voice Hero Header
    item {
        StatsDashboardHeader(
            stats = stats,
            currentUser = currentUser,
            syncState = syncState,
            onTapToSpeak = { viewModel.openVoiceDialog() },
            onCategoryClick = { cat -> viewModel.setSelectedCategory(cat) },
            onProfileClick = { viewModel.openUserSwitchDialog() },
            onOpenAuth = { viewModel.openAuthDialog() },
            onSignOut = { viewModel.signOut() }
        )
    }

    // 2. Search & Filter Bar
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        "Search tasks, bills, payees...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.testTag("btn_clear_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_input")
            )
        }
    }

    // 3. Category Filter Horizontal Carousel
    item {
        CategoryFilterBar(
            selectedCategory = selectedCategory,
            items = rawItems,
            onCategorySelected = { cat -> viewModel.setSelectedCategory(cat) }
        )
    }

    // 4. Status Tabs (All, Pending, Completed, Bills)
    item {
        val tabs = listOf("All", "Pending", "Bills", "Completed")
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedTab(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("home_tab_$index")
                )
            }
        }
    }

    // 5. Header Bar with Sort Selection & Count
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${items.size} ${if (items.size == 1) "item" else "items"}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Sort selector pills with Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TaskSortMode.entries.forEach { mode ->
                    val isSelected = sortMode == mode
                    val sortIcon = when (mode) {
                        TaskSortMode.PRIORITY -> Icons.Default.PriorityHigh
                        TaskSortMode.DUE_DATE -> Icons.Default.Schedule
                        TaskSortMode.CREATED -> Icons.Default.FiberNew
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .clickable { viewModel.setSortMode(mode) }
                            .testTag("sort_${mode.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = sortIcon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
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

    // 6. Items List
    renderTaskListItems(items, searchQuery, sortMode, viewModel)
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderTaskListItems(
    items: List<TaskPulseItem>,
    searchQuery: String,
    sortMode: TaskSortMode,
    viewModel: TaskPulseViewModel
) {
    if (items.isEmpty()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
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
        val activeItems = items.filter { !it.isCompleted && !(it.isBill && it.isPaid) }
        val completedItems = items.filter { it.isCompleted || (it.isBill && it.isPaid) }

        val urgentGroup = activeItems.filter { it.taskPriority == TaskPriority.URGENT }
        val highGroup = activeItems.filter { it.taskPriority == TaskPriority.HIGH }
        val mediumGroup = activeItems.filter { it.taskPriority == TaskPriority.MEDIUM }
        val lowGroup = activeItems.filter { it.taskPriority == TaskPriority.LOW }

        if (urgentGroup.isNotEmpty()) {
            item {
                PrioritySectionHeader(
                    title = "Urgent Priority",
                    icon = Icons.Default.Warning,
                    count = urgentGroup.size,
                    accentColor = MaterialTheme.colorScheme.error
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

        if (highGroup.isNotEmpty()) {
            item {
                PrioritySectionHeader(
                    title = "High Priority",
                    icon = Icons.Default.Bolt,
                    count = highGroup.size,
                    accentColor = Color(0xFFD97706)
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

        if (mediumGroup.isNotEmpty()) {
            item {
                PrioritySectionHeader(
                    title = "Mid Priority",
                    icon = Icons.Default.Flag,
                    count = mediumGroup.size,
                    accentColor = Color(0xFF0284C7)
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

        if (lowGroup.isNotEmpty()) {
            item {
                PrioritySectionHeader(
                    title = "Low Priority",
                    icon = Icons.Default.ArrowDownward,
                    count = lowGroup.size,
                    accentColor = Color(0xFF059669)
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

        if (completedItems.isNotEmpty()) {
            item {
                PrioritySectionHeader(
                    title = "Completed",
                    icon = Icons.Default.CheckCircle,
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

@Composable
fun PrioritySectionHeader(
    title: String,
    icon: ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
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

