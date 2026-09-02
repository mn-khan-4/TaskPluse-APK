package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TaskPulseApp
import com.example.ai.GeminiTaskParser
import com.example.ai.ParsedTaskResult
import com.example.data.local.AppAccentColor
import com.example.data.local.AppThemeMode
import com.example.data.local.NotificationPreferences
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskPulseItem
import com.example.data.model.TaskType
import com.example.data.model.UserAccount
import com.example.data.remote.SyncState
import com.example.data.repository.AuthResult
import com.example.reminders.ReminderScheduler
import com.example.voice.SpeechRecognizerHelper
import com.example.voice.VoiceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class TaskStats(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val unpaidBillsCount: Int = 0,
    val unpaidBillsTotal: Double = 0.0,
    val urgentCount: Int = 0,
    val dueTodayCount: Int = 0
)

enum class TaskSortMode(val displayName: String, val icon: String) {
    PRIORITY("Priority", "🔥"),
    DUE_DATE("Due Date", "⏰"),
    CREATED("Newest", "✨")
}

@OptIn(ExperimentalCoroutinesApi::class)
class TaskPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskPulseApp.instance.repository
    private val authRepository = TaskPulseApp.instance.authRepository
    private val firestoreSyncService = TaskPulseApp.instance.firestoreSyncService
    private val userPreferences = TaskPulseApp.instance.userPreferences

    val speechHelper = SpeechRecognizerHelper(application)

    // Active User Authentication & Profiles
    val currentUser: StateFlow<UserAccount> = authRepository.currentUser
    val knownUsers: StateFlow<List<UserAccount>> = authRepository.knownUsers
    val authLoading: StateFlow<Boolean> = authRepository.authLoading
    val syncState: StateFlow<SyncState> = firestoreSyncService.syncState

    // User Preferences & Theme Customization
    val themeMode: StateFlow<AppThemeMode> = userPreferences.themeMode
    val accentColor: StateFlow<AppAccentColor> = userPreferences.accentColor
    val notificationPrefs: StateFlow<NotificationPreferences> = userPreferences.notificationPrefs
    val calendarPrefs = userPreferences.calendarPrefs
    val calendarSyncService = TaskPulseApp.instance.googleCalendarSyncService

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: All, 1: Tasks, 2: Bills, 3: Urgent
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow<TaskCategory?>(null)
    val selectedCategory: StateFlow<TaskCategory?> = _selectedCategory.asStateFlow()

    private val _sortMode = MutableStateFlow(TaskSortMode.PRIORITY)
    val sortMode: StateFlow<TaskSortMode> = _sortMode.asStateFlow()

    private val _isVoiceDialogOpen = MutableStateFlow(false)
    val isVoiceDialogOpen: StateFlow<Boolean> = _isVoiceDialogOpen.asStateFlow()

    private val _isAddEditSheetOpen = MutableStateFlow(false)
    val isAddEditSheetOpen: StateFlow<Boolean> = _isAddEditSheetOpen.asStateFlow()

    private val _editingItem = MutableStateFlow<TaskPulseItem?>(null)
    val editingItem: StateFlow<TaskPulseItem?> = _editingItem.asStateFlow()

    private val _parsedTaskPreview = MutableStateFlow<ParsedTaskResult?>(null)
    val parsedTaskPreview: StateFlow<ParsedTaskResult?> = _parsedTaskPreview.asStateFlow()

    private val _isAiParsing = MutableStateFlow(false)
    val isAiParsing: StateFlow<Boolean> = _isAiParsing.asStateFlow()

    private val _isAuthDialogOpen = MutableStateFlow(false)
    val isAuthDialogOpen: StateFlow<Boolean> = _isAuthDialogOpen.asStateFlow()

    private val _isUserSwitchDialogOpen = MutableStateFlow(false)
    val isUserSwitchDialogOpen: StateFlow<Boolean> = _isUserSwitchDialogOpen.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Base flow from Room DB strictly isolated by currentUser.uid
    val rawItems: StateFlow<List<TaskPulseItem>> = currentUser.flatMapLatest { user ->
        firestoreSyncService.startListeningForUser(user.uid)
        repository.getAllItems(user.uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted items state based on Search, Category, Active Tab, and Priority/Sort Mode
    val filteredItems: StateFlow<List<TaskPulseItem>> = combine(
        rawItems,
        _searchQuery,
        _selectedTab,
        _selectedCategory,
        _sortMode
    ) { items, query, tab, category, sort ->
        val filtered = items.filter { item ->
            // Search query check (title, description, category, payee)
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                item.title.contains(query, ignoreCase = true) ||
                        item.description.contains(query, ignoreCase = true) ||
                        item.category.contains(query, ignoreCase = true) ||
                        item.taskCategory.displayName.contains(query, ignoreCase = true) ||
                        (item.billPayee?.contains(query, ignoreCase = true) ?: false)
            }

            // Tab filter
            val matchesTab = when (tab) {
                1 -> item.taskType != TaskType.BILL && item.taskCategory != TaskCategory.BILLS // Tasks & To-Dos
                2 -> item.isBill // Bills & Finance
                3 -> item.taskCategory == TaskCategory.URGENT || item.taskPriority == TaskPriority.URGENT || item.isOverdue() || item.isDueSoon(24) // Urgent & Alerts
                else -> true // All
            }

            // Category filter
            val matchesCategory = if (category == null) true else item.taskCategory == category

            matchesQuery && matchesTab && matchesCategory
        }

        // Apply Sorting / Grouping Logic
        when (sort) {
            TaskSortMode.PRIORITY -> {
                filtered.sortedWith(
                    compareBy<TaskPulseItem> { it.isCompleted || (it.isBill && it.isPaid) } // Active first, completed at the end
                        .thenByDescending { it.taskPriority.rank } // URGENT (4) > HIGH (3) > MEDIUM (2) > LOW (1)
                        .thenBy { it.dueDate ?: Long.MAX_VALUE }
                        .thenByDescending { it.createdAt }
                )
            }
            TaskSortMode.DUE_DATE -> {
                filtered.sortedWith(
                    compareBy<TaskPulseItem> { it.isCompleted || (it.isBill && it.isPaid) }
                        .thenBy { it.dueDate ?: Long.MAX_VALUE }
                        .thenByDescending { it.taskPriority.rank }
                )
            }
            TaskSortMode.CREATED -> {
                filtered.sortedWith(
                    compareBy<TaskPulseItem> { it.isCompleted || (it.isBill && it.isPaid) }
                        .thenByDescending { it.createdAt }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Realtime Stats computation for active user
    val stats: StateFlow<TaskStats> = rawItems.combine(_selectedTab) { items, _ ->
        val nowCal = Calendar.getInstance()
        val todayDay = nowCal.get(Calendar.DAY_OF_YEAR)
        val todayYear = nowCal.get(Calendar.YEAR)

        var completed = 0
        var pending = 0
        var unpaidBillsCount = 0
        var unpaidBillsTotal = 0.0
        var urgentCount = 0
        var dueTodayCount = 0

        for (item in items) {
            if (item.isCompleted || (item.isBill && item.isPaid)) {
                completed++
            } else {
                pending++
            }

            if (item.isBill && !item.isPaid) {
                unpaidBillsCount++
                unpaidBillsTotal += (item.amount ?: 0.0)
            }

            if ((item.taskCategory == TaskCategory.URGENT || item.taskPriority == TaskPriority.URGENT) && !item.isCompleted && !item.isPaid) {
                urgentCount++
            }

            val targetDate = item.dueDate ?: item.reminderTime
            if (targetDate != null && !item.isCompleted && !item.isPaid) {
                val cal = Calendar.getInstance().apply { timeInMillis = targetDate }
                if (cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay) {
                    dueTodayCount++
                }
            }
        }

        TaskStats(
            totalCount = items.size,
            completedCount = completed,
            pendingCount = pending,
            unpaidBillsCount = unpaidBillsCount,
            unpaidBillsTotal = unpaidBillsTotal,
            urgentCount = urgentCount,
            dueTodayCount = dueTodayCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskStats())

    init {
        // Observe speech recognition results
        viewModelScope.launch {
            speechHelper.voiceState.collect { state ->
                if (state is VoiceState.Success) {
                    parseAndPreviewVoiceInput(state.transcript)
                }
            }
        }

        // Seed realistic starter data for active user if their database is empty
        viewModelScope.launch {
            currentUser.collect { user ->
                val current = repository.getAllItems(user.uid)
                // We check if empty by collecting first emission
                current.collect { items ->
                    if (items.isEmpty()) {
                        repository.seedSampleDataForUser(user.uid, user.displayName)
                    }
                }
            }
        }
    }

    // ==================== AUTHENTICATION & MULTI-USER METHODS ====================

    fun signIn(email: String, pass: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            when (val result = authRepository.signIn(email, pass)) {
                is AuthResult.Success -> {
                    _userMessage.value = "Welcome back, ${result.user.displayName}! 👋"
                    _isAuthDialogOpen.value = false
                    onComplete?.invoke(true, null)
                }
                is AuthResult.Error -> {
                    _userMessage.value = result.message
                    onComplete?.invoke(false, result.message)
                }
            }
        }
    }

    fun signUp(
        email: String,
        pass: String,
        displayName: String,
        role: String = "Member",
        avatarEmoji: String = "⚡",
        avatarColorIndex: Int = 0,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            when (val result = authRepository.signUp(email, pass, displayName, role, avatarEmoji, avatarColorIndex)) {
                is AuthResult.Success -> {
                    _userMessage.value = "Account created! Welcome, ${result.user.displayName}! 🎉"
                    _isAuthDialogOpen.value = false
                    onComplete?.invoke(true, null)
                }
                is AuthResult.Error -> {
                    _userMessage.value = result.message
                    onComplete?.invoke(false, result.message)
                }
            }
        }
    }

    fun switchUser(user: UserAccount) {
        authRepository.switchUser(user)
        _userMessage.value = "Switched to ${user.displayName}'s workspace 🔄"
        _isUserSwitchDialogOpen.value = false
    }

    fun signOut() {
        val oldName = currentUser.value.displayName
        authRepository.signOut()
        _userMessage.value = "Signed out from $oldName. Switched account."
    }

    fun openAuthDialog() {
        _isAuthDialogOpen.value = true
    }

    fun closeAuthDialog() {
        _isAuthDialogOpen.value = false
    }

    fun openUserSwitchDialog() {
        _isUserSwitchDialogOpen.value = true
    }

    fun closeUserSwitchDialog() {
        _isUserSwitchDialogOpen.value = false
    }

    // ==================== THEME & PREFERENCES METHODS ====================

    fun setThemeMode(mode: AppThemeMode) {
        userPreferences.setThemeMode(mode)
        _userMessage.value = "Theme set to ${mode.displayName}"
    }

    fun setAccentColor(accent: AppAccentColor) {
        userPreferences.setAccentColor(accent)
        _userMessage.value = "Color palette: ${accent.displayName}"
    }

    fun updateUserProfile(name: String, email: String, role: String, emoji: String, colorIndex: Int) {
        val updated = currentUser.value.copy(
            displayName = name,
            email = email,
            role = role,
            avatarEmoji = emoji,
            avatarColorIndex = colorIndex
        )
        authRepository.updateUserProfile(updated)
        _userMessage.value = "Profile updated: $name"
    }

    fun updateNotificationPreferences(prefs: NotificationPreferences) {
        userPreferences.updateNotificationPreferences(prefs)
        _userMessage.value = "Notification settings saved"
        viewModelScope.launch {
            repository.rescheduleAllPendingReminders()
        }
    }

    fun triggerTestAlarm(context: Context) {
        ReminderScheduler.scheduleTestAlarm(context, delaySeconds = 3)
        _userMessage.value = "AlarmManager test set for 3 seconds! ⏰"
    }

    // ==================== FILTERING & SEARCH ====================

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSelectedCategory(category: TaskCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun setSortMode(mode: TaskSortMode) {
        _sortMode.value = mode
        _userMessage.value = "Sorting by ${mode.displayName}"
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // ==================== DIALOG & BOTTOM SHEET CONTROLS ====================

    fun openVoiceDialog() {
        _parsedTaskPreview.value = null
        _isVoiceDialogOpen.value = true
        speechHelper.startListening()
    }

    fun closeVoiceDialog() {
        speechHelper.cancel()
        _isVoiceDialogOpen.value = false
        _parsedTaskPreview.value = null
    }

    fun openAddEdit(item: TaskPulseItem? = null) {
        _editingItem.value = item
        _isAddEditSheetOpen.value = true
    }

    fun closeAddEdit() {
        _isAddEditSheetOpen.value = false
        _editingItem.value = null
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // ==================== VOICE / AI TASK PARSING ====================

    fun parseAndPreviewVoiceInput(transcript: String) {
        viewModelScope.launch {
            _isAiParsing.value = true
            try {
                val parsed = GeminiTaskParser.parseNaturalLanguage(transcript)
                _parsedTaskPreview.value = parsed
            } catch (e: Exception) {
                val localFallback = GeminiTaskParser.parseWithLocalNLP(transcript)
                _parsedTaskPreview.value = localFallback
            } finally {
                _isAiParsing.value = false
            }
        }
    }

    fun updateParsedPreview(updated: ParsedTaskResult) {
        _parsedTaskPreview.value = updated
    }

    fun confirmSaveParsedTask(result: ParsedTaskResult) {
        viewModelScope.launch {
            val item = TaskPulseItem(
                userId = currentUser.value.uid,
                title = result.title,
                description = result.description,
                category = result.category.name,
                type = result.type.name,
                priority = result.priority.name,
                dueDate = result.dueDate,
                reminderTime = result.reminderTime,
                amount = result.amount,
                billPayee = result.billPayee,
                isRecurring = result.isRecurring,
                recurringInterval = result.recurringInterval,
                rawVoiceTranscript = result.rawInput
            )
            repository.insertItem(item)
            _userMessage.value = "Saved: \"${result.title}\" (${result.category.displayName})"
            closeVoiceDialog()
        }
    }

    // ==================== TASK ITEM CRUD OPERATIONS ====================

    fun saveItem(item: TaskPulseItem) {
        viewModelScope.launch {
            val scopedItem = if (item.userId.isBlank()) item.copy(userId = currentUser.value.uid) else item
            if (scopedItem.id == 0L) {
                repository.insertItem(scopedItem)
                _userMessage.value = "Added \"${scopedItem.title}\""
            } else {
                repository.updateItem(scopedItem)
                _userMessage.value = "Updated \"${scopedItem.title}\""
            }
            closeAddEdit()
        }
    }

    fun deleteItem(item: TaskPulseItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            _userMessage.value = "Deleted \"${item.title}\""
        }
    }

    fun toggleCompleted(item: TaskPulseItem) {
        viewModelScope.launch {
            repository.toggleCompleted(item)
            if (!item.isCompleted) {
                _userMessage.value = "Completed \"${item.title}\" ✓"
            }
        }
    }

    fun togglePaid(item: TaskPulseItem) {
        viewModelScope.launch {
            repository.togglePaid(item)
            if (!item.isPaid) {
                _userMessage.value = "Marked ${item.title} as paid 💰"
            }
        }
    }

    fun snoozeItem(item: TaskPulseItem, minutes: Int = 15) {
        viewModelScope.launch {
            repository.snoozeReminder(currentUser.value.uid, item.id, minutes)
            _userMessage.value = "Snoozed for $minutes minutes ⏰"
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            repository.clearCompleted(currentUser.value.uid)
            _userMessage.value = "Cleared completed items"
        }
    }

    fun resetSampleData() {
        viewModelScope.launch {
            val uid = currentUser.value.uid
            for (item in rawItems.value) {
                repository.deleteItem(item)
            }
            repository.seedSampleDataForUser(uid, currentUser.value.displayName)
            _userMessage.value = "Reset to sample tasks & bills for ${currentUser.value.displayName}"
        }
    }

    // ==================== GOOGLE CALENDAR SYNC ACTIONS ====================

    fun syncItemToGoogleCalendar(item: TaskPulseItem) {
        viewModelScope.launch {
            val success = repository.syncSingleItemToGoogleCalendar(item)
            if (success) {
                _userMessage.value = "Synced \"${item.title}\" to Google Calendar 📅"
            } else {
                _userMessage.value = "Failed to sync to Google Calendar"
            }
        }
    }

    fun syncAllToGoogleCalendar() {
        viewModelScope.launch {
            val count = repository.syncAllItemsToGoogleCalendar(currentUser.value.uid)
            _userMessage.value = if (count > 0) "Synced $count reminder(s) to Google Calendar 📅" else "All scheduled reminders are up to date in Google Calendar!"
        }
    }

    fun setCalendarAutoSync(enabled: Boolean) {
        userPreferences.setCalendarAutoSync(enabled)
        _userMessage.value = if (enabled) "Google Calendar auto-sync enabled 📅" else "Google Calendar auto-sync paused"
    }

    fun connectGoogleCalendar(email: String = "noumanjamil2004@gmail.com") {
        userPreferences.setCalendarConnected(true, email)
        _userMessage.value = "Connected Google Calendar ($email) 📅"
    }

    fun disconnectGoogleCalendar() {
        userPreferences.setCalendarConnected(false)
        _userMessage.value = "Google Calendar disconnected"
    }
}
