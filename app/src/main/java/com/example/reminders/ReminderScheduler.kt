package com.example.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.TaskPulseApp
import com.example.data.model.TaskPulseItem
import com.example.receiver.ReminderAlarmReceiver

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"
    const val TEST_ITEM_ID = 999999L

    fun scheduleReminder(context: Context, item: TaskPulseItem) {
        val userPrefs = TaskPulseApp.instance.userPreferences.notificationPrefs.value
        if (!userPrefs.enabled) {
            Log.d(TAG, "Notifications globally disabled by user preference")
            return
        }

        val leadTimeMillis = userPrefs.leadTimeMinutes * 60 * 1000L
        val now = System.currentTimeMillis()

        // Determine exact trigger time
        val triggerTime = when {
            item.reminderTime != null && item.reminderTime > now -> item.reminderTime
            item.dueDate != null -> {
                val approachingTime = item.dueDate - leadTimeMillis
                if (approachingTime > now) {
                    approachingTime
                } else if (item.dueDate > now) {
                    item.dueDate
                } else {
                    null
                }
            }
            else -> null
        } ?: return

        if (item.isCompleted || (item.isBill && item.isPaid)) {
            // Do not schedule past or completed items
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val isApproaching = item.dueDate != null && triggerTime < item.dueDate

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_ITEM_ID, item.id)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, item.title)
            putExtra(ReminderAlarmReceiver.EXTRA_DESCRIPTION, item.description)
            putExtra(ReminderAlarmReceiver.EXTRA_CATEGORY, item.category)
            putExtra(ReminderAlarmReceiver.EXTRA_PRIORITY, item.priority)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_BILL, item.isBill)
            putExtra(ReminderAlarmReceiver.EXTRA_AMOUNT, item.amount ?: 0.0)
            putExtra(ReminderAlarmReceiver.EXTRA_PAYEE, item.billPayee ?: "")
            putExtra(ReminderAlarmReceiver.EXTRA_DUE_DATE, item.dueDate ?: 0L)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_APPROACHING, isApproaching)
        }

        val requestCode = item.id.toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Successfully scheduled AlarmManager reminder for ${item.title} at $triggerTime (in ${(triggerTime - now) / 1000}s)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing: ${e.message}, falling back to inexact alarm")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for ${item.title}: ${e.message}")
        }
    }

    fun cancelReminder(context: Context, itemId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val requestCode = itemId.toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled reminder for item $itemId")
        }
    }

    fun scheduleTestAlarm(context: Context, delaySeconds: Int = 3) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_ITEM_ID, TEST_ITEM_ID)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, "Test Alarm: Electricity & Wi-Fi Bill")
            putExtra(ReminderAlarmReceiver.EXTRA_DESCRIPTION, "Approaching due date in 15 minutes! AlarmManager is working perfectly.")
            putExtra(ReminderAlarmReceiver.EXTRA_CATEGORY, "BILLS")
            putExtra(ReminderAlarmReceiver.EXTRA_PRIORITY, "URGENT")
            putExtra(ReminderAlarmReceiver.EXTRA_IS_BILL, true)
            putExtra(ReminderAlarmReceiver.EXTRA_AMOUNT, 84.50)
            putExtra(ReminderAlarmReceiver.EXTRA_PAYEE, "Electric Utility")
            putExtra(ReminderAlarmReceiver.EXTRA_DUE_DATE, triggerTime + 15 * 60 * 1000L)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_APPROACHING, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_ITEM_ID.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled test alarm for $delaySeconds seconds from now")
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            return alarmManager?.canScheduleExactAlarms() ?: false
        }
        return true
    }
}
