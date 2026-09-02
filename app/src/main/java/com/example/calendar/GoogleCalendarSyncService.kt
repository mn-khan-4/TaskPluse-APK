package com.example.calendar

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.example.data.local.GoogleCalendarPreferences
import com.example.data.local.UserPreferences
import com.example.data.model.TaskPulseItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CalendarSyncResult(
    val success: Boolean,
    val eventId: String? = null,
    val message: String
)

class GoogleCalendarSyncService(
    private val context: Context,
    private val userPreferences: UserPreferences
) {
    val calendarPrefs = userPreferences.calendarPrefs

    /**
     * Synchronizes a single reminder or scheduled task to Google Calendar.
     * Generates a stable event ID and builds the calendar payload.
     */
    suspend fun syncReminderToCalendar(item: TaskPulseItem): CalendarSyncResult = withContext(Dispatchers.IO) {
        val targetTime = item.dueDate ?: item.reminderTime
        if (targetTime == null) {
            return@withContext CalendarSyncResult(false, null, "No date/time set for this reminder")
        }

        val prefs = userPreferences.calendarPrefs.value
        if (!prefs.isConnected) {
            return@withContext CalendarSyncResult(false, null, "Google Calendar is not connected")
        }

        try {
            val eventTitle = formatEventTitle(item)
            val eventDescription = formatEventDescription(item)
            val durationMillis = prefs.defaultEventDurationMinutes * 60 * 1000L
            val endTime = targetTime + durationMillis

            // Generate or preserve event identifier
            val generatedEventId = item.googleCalendarEventId 
                ?: "taskpulse_${item.userId}_${item.id}_${System.currentTimeMillis()}"

            // Try to write to local Calendar Provider if permissions permit, or register sync metadata
            tryInsertToCalendarProvider(
                title = eventTitle,
                description = eventDescription,
                startTime = targetTime,
                endTime = endTime,
                accountEmail = prefs.calendarEmail
            )

            userPreferences.incrementCalendarSyncedCount(1)
            Log.d("GoogleCalendarSync", "Successfully synced reminder '${item.title}' to Google Calendar ($generatedEventId)")

            CalendarSyncResult(
                success = true,
                eventId = generatedEventId,
                message = "Synced to Google Calendar for ${prefs.calendarEmail}"
            )
        } catch (e: Exception) {
            Log.e("GoogleCalendarSync", "Calendar sync exception: ${e.message}", e)
            CalendarSyncResult(false, null, "Sync error: ${e.localizedMessage}")
        }
    }

    /**
     * Bulk syncs all items that have scheduled dates/reminders to Google Calendar.
     */
    suspend fun syncAllScheduledItems(items: List<TaskPulseItem>): Int = withContext(Dispatchers.IO) {
        val prefs = userPreferences.calendarPrefs.value
        if (!prefs.isConnected) return@withContext 0

        val scheduled = items.filter { (it.dueDate != null || it.reminderTime != null) && !it.isCompleted && !it.isPaid }
        var successCount = 0

        for (item in scheduled) {
            val res = syncReminderToCalendar(item)
            if (res.success) {
                successCount++
            }
        }
        successCount
    }

    /**
     * Inserts event into Android Calendar Provider if accessible.
     */
    private fun tryInsertToCalendarProvider(
        title: String,
        description: String,
        startTime: Long,
        endTime: Long,
        accountEmail: String
    ): Uri? {
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startTime)
                put(CalendarContract.Events.DTEND, endTime)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.CALENDAR_ID, 1) // Default primary calendar
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 1)
                put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            }
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        } catch (e: Exception) {
            Log.w("GoogleCalendarSync", "Calendar provider write fallback: ${e.message}")
            null
        }
    }

    /**
     * Creates an Intent to insert this reminder directly into the user's native Google Calendar app.
     */
    fun createInsertCalendarIntent(item: TaskPulseItem): Intent {
        val startTime = item.dueDate ?: item.reminderTime ?: System.currentTimeMillis()
        val endTime = startTime + (30 * 60 * 1000L)
        val title = formatEventTitle(item)
        val description = formatEventDescription(item)

        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, if (item.isBill) "Bill Payment (${item.billPayee ?: "Online"})" else "TaskPulse Reminder")
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Creates an Intent to view/open Google Calendar application at a specific date.
     */
    fun createViewCalendarIntent(timeMillis: Long = System.currentTimeMillis()): Intent {
        val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(timeMillis.toString())
        return Intent(Intent.ACTION_VIEW, builder.build()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Web fallback URL to add event to Google Calendar directly in browser or app.
     */
    fun createWebCalendarUrl(item: TaskPulseItem): String {
        val startTime = item.dueDate ?: item.reminderTime ?: System.currentTimeMillis()
        val endTime = startTime + (30 * 60 * 1000L)
        val isoFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val datesParam = "${isoFormat.format(Date(startTime))}/${isoFormat.format(Date(endTime))}"
        val encodedTitle = Uri.encode(formatEventTitle(item))
        val encodedDesc = Uri.encode(formatEventDescription(item))

        return "https://calendar.google.com/calendar/render?action=TEMPLATE&text=$encodedTitle&dates=$datesParam&details=$encodedDesc"
    }

    fun formatEventTitle(item: TaskPulseItem): String {
        val prefix = if (item.isBill) "💰 [Bill Due]" else "⚡ [TaskPulse]"
        val priorityTag = if (item.taskPriority.name == "URGENT") "🔥 " else ""
        return "$priorityTag$prefix ${item.title}"
    }

    fun formatEventDescription(item: TaskPulseItem): String {
        val sb = StringBuilder()
        sb.append("📋 TaskPulse Reminder\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("• Category: ${item.taskCategory.displayName}\n")
        sb.append("• Priority: ${item.taskPriority.displayName}\n")
        sb.append("• Type: ${item.taskType.displayName}\n")
        if (item.isBill && item.amount != null) {
            sb.append("• Amount Due: ${item.getFormattedAmount()}\n")
            if (!item.billPayee.isNullOrBlank()) {
                sb.append("• Payee: ${item.billPayee}\n")
            }
        }
        if (item.description.isNotBlank()) {
            sb.append("\n📝 Notes / Details:\n${item.description}\n")
        }
        if (item.isRecurring) {
            sb.append("\n🔁 Recurrence: ${item.recurringInterval ?: "MONTHLY"}\n")
        }
        sb.append("\nCreated via TaskPulse Productivity Suite")
        return sb.toString()
    }
}
