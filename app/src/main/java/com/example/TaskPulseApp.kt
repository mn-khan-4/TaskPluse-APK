package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.calendar.GoogleCalendarSyncService
import com.example.data.local.TaskPulseDatabase
import com.example.data.local.UserPreferences
import com.example.data.remote.FirestoreSyncService
import com.example.data.repository.AuthRepository
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TaskPulseApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: TaskPulseDatabase
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var firestoreSyncService: FirestoreSyncService
        private set

    lateinit var googleCalendarSyncService: GoogleCalendarSyncService
        private set

    lateinit var repository: TaskRepository
        private set

    lateinit var userPreferences: UserPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        userPreferences = UserPreferences(this)
        database = TaskPulseDatabase.getDatabase(this)
        authRepository = AuthRepository(this, applicationScope)
        firestoreSyncService = FirestoreSyncService(this, database.taskDao(), applicationScope)
        googleCalendarSyncService = GoogleCalendarSyncService(this, userPreferences)
        repository = TaskRepository(database.taskDao(), this, firestoreSyncService, googleCalendarSyncService)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // General & Tasks Reminders Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Task & To-Do Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled tasks, to-dos and approaching due dates"
                enableVibration(true)
                setShowBadge(true)
            }

            // Bills & Financial Channel
            val billsChannel = NotificationChannel(
                CHANNEL_BILLS,
                "Bill Due Dates & Payments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming and approaching bills"
                enableVibration(true)
                setShowBadge(true)
            }

            // Urgent Priority Channel
            val urgentChannel = NotificationChannel(
                CHANNEL_URGENT,
                "Urgent Task Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority and urgent task alarms"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(billsChannel)
            notificationManager.createNotificationChannel(urgentChannel)
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "channel_reminders"
        const val CHANNEL_BILLS = "channel_bills"
        const val CHANNEL_URGENT = "channel_urgent"

        lateinit var instance: TaskPulseApp
            private set
    }
}
