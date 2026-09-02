package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.TaskPulseApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId == -1L) return

        val userPrefs = TaskPulseApp.instance.userPreferences.notificationPrefs.value
        if (!userPrefs.enabled) return

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Task Reminder"
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "OTHER"
        val priority = intent.getStringExtra(EXTRA_PRIORITY) ?: "MEDIUM"
        val isBill = intent.getBooleanExtra(EXTRA_IS_BILL, false)
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val payee = intent.getStringExtra(EXTRA_PAYEE) ?: ""
        val dueDate = intent.getLongExtra(EXTRA_DUE_DATE, 0L)
        val isApproaching = intent.getBooleanExtra(EXTRA_IS_APPROACHING, false)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Choose channel based on type
        val channelId = when {
            isBill || category.equals("BILLS", ignoreCase = true) -> TaskPulseApp.CHANNEL_BILLS
            priority.equals("URGENT", ignoreCase = true) || category.equals("URGENT", ignoreCase = true) -> TaskPulseApp.CHANNEL_URGENT
            else -> TaskPulseApp.CHANNEL_REMINDERS
        }

        // Tap content intent
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_ITEM_ID", itemId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            itemId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark as Done / Mark as Paid
        val doneActionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = if (isBill) NotificationActionReceiver.ACTION_MARK_PAID else NotificationActionReceiver.ACTION_MARK_DONE
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, itemId.toInt())
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (itemId * 10 + 1).toInt(),
            doneActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 15 min
        val snooze15ActionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, 15)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, itemId.toInt())
        }
        val snooze15PendingIntent = PendingIntent.getBroadcast(
            context,
            (itemId * 10 + 2).toInt(),
            snooze15ActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Format due time string if present
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        val dueStr = if (dueDate > 0) dateFormat.format(Date(dueDate)) else ""

        // Build title & text
        val notificationTitle = when {
            isBill && isApproaching -> "💰 Bill Approaching Due Date: $title"
            isBill -> "💰 Bill Due: $title"
            isApproaching -> "⏰ Approaching Due Date: $title"
            else -> "⏰ Reminder: $title"
        }

        val contentText = buildString {
            if (isBill && amount > 0) {
                append(String.format(Locale.US, "$%.2f", amount))
                if (payee.isNotBlank()) append(" to $payee")
                if (dueStr.isNotBlank()) append(" (Due $dueStr)")
                if (description.isNotBlank()) append("\n$description")
            } else {
                if (dueStr.isNotBlank()) append("Due at $dueStr • ")
                if (description.isNotBlank()) {
                    append(description)
                } else {
                    append("Category: $category")
                }
            }
        }

        val actionTitle = if (isBill) "Mark Paid" else "Mark Done"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(if (isBill) Color.parseColor("#10B981") else Color.parseColor("#D0BCFF"))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, actionTitle, donePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 15m", snooze15PendingIntent)

        if (userPrefs.vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 300, 200, 300))
        }

        notificationManager.notify(itemId.toInt(), builder.build())
    }

    companion object {
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_PRIORITY = "extra_priority"
        const val EXTRA_IS_BILL = "extra_is_bill"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_PAYEE = "extra_payee"
        const val EXTRA_DUE_DATE = "extra_due_date"
        const val EXTRA_IS_APPROACHING = "extra_is_approaching"
    }
}
