package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val displayName: String) {
    DARK("Velvet Dark"),
    AMOLED("AMOLED Black"),
    LIGHT("Minimal Light"),
    SYSTEM("System Default")
}

enum class AppAccentColor(
    val displayName: String,
    val primaryColor: Color,
    val containerColor: Color,
    val onPrimaryColor: Color,
    val hexCode: String
) {
    LAVENDER(
        displayName = "Lavender Bloom",
        primaryColor = Color(0xFFD0BCFF),
        containerColor = Color(0xFF4F378B),
        onPrimaryColor = Color(0xFF381E72),
        hexCode = "#D0BCFF"
    ),
    EMERALD(
        displayName = "Emerald Mint",
        primaryColor = Color(0xFF34D399),
        containerColor = Color(0xFF065F46),
        onPrimaryColor = Color(0xFF022C22),
        hexCode = "#34D399"
    ),
    CYAN(
        displayName = "Electric Cyan",
        primaryColor = Color(0xFF38BDF8),
        containerColor = Color(0xFF075985),
        onPrimaryColor = Color(0xFF082F49),
        hexCode = "#38BDF8"
    ),
    AMBER(
        displayName = "Solar Amber",
        primaryColor = Color(0xFFFBBF24),
        containerColor = Color(0xFF78350F),
        onPrimaryColor = Color(0xFF451A03),
        hexCode = "#FBBF24"
    ),
    ROSE(
        displayName = "Rose Coral",
        primaryColor = Color(0xFFF472B6),
        containerColor = Color(0xFF831843),
        onPrimaryColor = Color(0xFF500724),
        hexCode = "#F472B6"
    ),
    INDIGO(
        displayName = "Royal Indigo",
        primaryColor = Color(0xFF818CF8),
        containerColor = Color(0xFF3730A3),
        onPrimaryColor = Color(0xFF1E1B4B),
        hexCode = "#818CF8"
    )
}

data class UserProfile(
    val name: String = "Alex Johnson",
    val email: String = "noumanjamil2004@gmail.com",
    val role: String = "Productivity Champion",
    val avatarEmoji: String = "⚡",
    val avatarColorIndex: Int = 0
)

data class NotificationPreferences(
    val enabled: Boolean = true,
    val leadTimeMinutes: Int = 15, // Alert 15 min before due date
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoRescheduleMissed: Boolean = true
)

data class GoogleCalendarPreferences(
    val isConnected: Boolean = true,
    val calendarEmail: String = "noumanjamil2004@gmail.com",
    val autoSyncReminders: Boolean = true,
    val syncBills: Boolean = true,
    val defaultEventDurationMinutes: Int = 30,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val syncedCount: Int = 0
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("taskpulse_user_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(loadAccentColor())
    val accentColor: StateFlow<AppAccentColor> = _accentColor.asStateFlow()

    private val _userProfile = MutableStateFlow(loadUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _notificationPrefs = MutableStateFlow(loadNotificationPreferences())
    val notificationPrefs: StateFlow<NotificationPreferences> = _notificationPrefs.asStateFlow()

    private val _calendarPrefs = MutableStateFlow(loadCalendarPreferences())
    val calendarPrefs: StateFlow<GoogleCalendarPreferences> = _calendarPrefs.asStateFlow()

    private fun loadThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        return try {
            AppThemeMode.valueOf(name)
        } catch (e: Exception) {
            AppThemeMode.DARK
        }
    }

    private fun loadAccentColor(): AppAccentColor {
        val name = prefs.getString(KEY_ACCENT_COLOR, AppAccentColor.LAVENDER.name) ?: AppAccentColor.LAVENDER.name
        return try {
            AppAccentColor.valueOf(name)
        } catch (e: Exception) {
            AppAccentColor.LAVENDER
        }
    }

    private fun loadUserProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString(KEY_USER_NAME, "Alex Johnson") ?: "Alex Johnson",
            email = prefs.getString(KEY_USER_EMAIL, "noumanjamil2004@gmail.com") ?: "noumanjamil2004@gmail.com",
            role = prefs.getString(KEY_USER_ROLE, "Productivity Champion") ?: "Productivity Champion",
            avatarEmoji = prefs.getString(KEY_USER_AVATAR, "⚡") ?: "⚡",
            avatarColorIndex = prefs.getInt(KEY_USER_AVATAR_COLOR, 0)
        )
    }

    private fun loadNotificationPreferences(): NotificationPreferences {
        return NotificationPreferences(
            enabled = prefs.getBoolean(KEY_NOTIF_ENABLED, true),
            leadTimeMinutes = prefs.getInt(KEY_NOTIF_LEAD_TIME, 15),
            soundEnabled = prefs.getBoolean(KEY_NOTIF_SOUND, true),
            vibrationEnabled = prefs.getBoolean(KEY_NOTIF_VIBRATION, true),
            autoRescheduleMissed = prefs.getBoolean(KEY_NOTIF_AUTO_RESCHEDULE, true)
        )
    }

    private fun loadCalendarPreferences(): GoogleCalendarPreferences {
        return GoogleCalendarPreferences(
            isConnected = prefs.getBoolean(KEY_CALENDAR_CONNECTED, true),
            calendarEmail = prefs.getString(KEY_CALENDAR_EMAIL, "noumanjamil2004@gmail.com") ?: "noumanjamil2004@gmail.com",
            autoSyncReminders = prefs.getBoolean(KEY_CALENDAR_AUTO_SYNC, true),
            syncBills = prefs.getBoolean(KEY_CALENDAR_SYNC_BILLS, true),
            defaultEventDurationMinutes = prefs.getInt(KEY_CALENDAR_DURATION, 30),
            lastSyncTime = prefs.getLong(KEY_CALENDAR_LAST_SYNC, System.currentTimeMillis()),
            syncedCount = prefs.getInt(KEY_CALENDAR_SYNCED_COUNT, 0)
        )
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setAccentColor(accent: AppAccentColor) {
        prefs.edit().putString(KEY_ACCENT_COLOR, accent.name).apply()
        _accentColor.value = accent
    }

    fun updateUserProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_USER_NAME, profile.name)
            .putString(KEY_USER_EMAIL, profile.email)
            .putString(KEY_USER_ROLE, profile.role)
            .putString(KEY_USER_AVATAR, profile.avatarEmoji)
            .putInt(KEY_USER_AVATAR_COLOR, profile.avatarColorIndex)
            .apply()
        _userProfile.value = profile
    }

    fun updateNotificationPreferences(notifPrefs: NotificationPreferences) {
        prefs.edit()
            .putBoolean(KEY_NOTIF_ENABLED, notifPrefs.enabled)
            .putInt(KEY_NOTIF_LEAD_TIME, notifPrefs.leadTimeMinutes)
            .putBoolean(KEY_NOTIF_SOUND, notifPrefs.soundEnabled)
            .putBoolean(KEY_NOTIF_VIBRATION, notifPrefs.vibrationEnabled)
            .putBoolean(KEY_NOTIF_AUTO_RESCHEDULE, notifPrefs.autoRescheduleMissed)
            .apply()
        _notificationPrefs.value = notifPrefs
    }

    fun updateCalendarPreferences(calPrefs: GoogleCalendarPreferences) {
        prefs.edit()
            .putBoolean(KEY_CALENDAR_CONNECTED, calPrefs.isConnected)
            .putString(KEY_CALENDAR_EMAIL, calPrefs.calendarEmail)
            .putBoolean(KEY_CALENDAR_AUTO_SYNC, calPrefs.autoSyncReminders)
            .putBoolean(KEY_CALENDAR_SYNC_BILLS, calPrefs.syncBills)
            .putInt(KEY_CALENDAR_DURATION, calPrefs.defaultEventDurationMinutes)
            .putLong(KEY_CALENDAR_LAST_SYNC, calPrefs.lastSyncTime)
            .putInt(KEY_CALENDAR_SYNCED_COUNT, calPrefs.syncedCount)
            .apply()
        _calendarPrefs.value = calPrefs
    }

    fun setCalendarConnected(connected: Boolean, email: String = "noumanjamil2004@gmail.com") {
        val updated = _calendarPrefs.value.copy(
            isConnected = connected,
            calendarEmail = email,
            lastSyncTime = if (connected) System.currentTimeMillis() else _calendarPrefs.value.lastSyncTime
        )
        updateCalendarPreferences(updated)
    }

    fun setCalendarAutoSync(enabled: Boolean) {
        val updated = _calendarPrefs.value.copy(autoSyncReminders = enabled)
        updateCalendarPreferences(updated)
    }

    fun incrementCalendarSyncedCount(amount: Int = 1) {
        val updated = _calendarPrefs.value.copy(
            syncedCount = _calendarPrefs.value.syncedCount + amount,
            lastSyncTime = System.currentTimeMillis()
        )
        updateCalendarPreferences(updated)
    }

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_ACCENT_COLOR = "pref_accent_color"
        private const val KEY_USER_NAME = "pref_user_name"
        private const val KEY_USER_EMAIL = "pref_user_email"
        private const val KEY_USER_ROLE = "pref_user_role"
        private const val KEY_USER_AVATAR = "pref_user_avatar"
        private const val KEY_USER_AVATAR_COLOR = "pref_user_avatar_color"
        private const val KEY_NOTIF_ENABLED = "pref_notif_enabled"
        private const val KEY_NOTIF_LEAD_TIME = "pref_notif_lead_time"
        private const val KEY_NOTIF_SOUND = "pref_notif_sound"
        private const val KEY_NOTIF_VIBRATION = "pref_notif_vibration"
        private const val KEY_NOTIF_AUTO_RESCHEDULE = "pref_notif_auto_reschedule"
        private const val KEY_CALENDAR_CONNECTED = "pref_calendar_connected"
        private const val KEY_CALENDAR_EMAIL = "pref_calendar_email"
        private const val KEY_CALENDAR_AUTO_SYNC = "pref_calendar_auto_sync"
        private const val KEY_CALENDAR_SYNC_BILLS = "pref_calendar_sync_bills"
        private const val KEY_CALENDAR_DURATION = "pref_calendar_duration"
        private const val KEY_CALENDAR_LAST_SYNC = "pref_calendar_last_sync"
        private const val KEY_CALENDAR_SYNCED_COUNT = "pref_calendar_synced_count"
    }
}
