package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.TaskPulseApp
import com.example.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(ReminderAlarmReceiver.EXTRA_ITEM_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationId != -1) {
            notificationManager.cancel(notificationId)
        }

        if (itemId == -1L) return

        if (itemId == ReminderScheduler.TEST_ITEM_ID) {
            Toast.makeText(context, "Test notification action handled!", Toast.LENGTH_SHORT).show()
            return
        }

        val repository = TaskPulseApp.instance.repository
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 15)

        when (intent.action) {
            ACTION_MARK_DONE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val item = repository.getItemById(itemId)
                    if (item != null) {
                        repository.toggleCompleted(item)
                    }
                }
                Toast.makeText(context, "Marked task as done ✓", Toast.LENGTH_SHORT).show()
            }
            ACTION_MARK_PAID -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val item = repository.getItemById(itemId)
                    if (item != null) {
                        repository.togglePaid(item)
                    }
                }
                Toast.makeText(context, "Bill marked as paid 💰", Toast.LENGTH_SHORT).show()
            }
            ACTION_SNOOZE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.snoozeReminder(itemId, minutes = snoozeMinutes)
                }
                Toast.makeText(context, "Snoozed for $snoozeMinutes minutes ⏰", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.example.action.MARK_DONE"
        const val ACTION_MARK_PAID = "com.example.action.MARK_PAID"
        const val ACTION_SNOOZE = "com.example.action.SNOOZE"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }
}
