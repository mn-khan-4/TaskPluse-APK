package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.TaskPulseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Device rebooted, rescheduling pending reminders")
            val repository = TaskPulseApp.instance.repository
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.rescheduleAllPendingReminders()
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling reminders on boot: ${e.message}")
                }
            }
        }
    }
}
