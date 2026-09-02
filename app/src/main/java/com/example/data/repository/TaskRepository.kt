package com.example.data.repository

import android.content.Context
import com.example.calendar.GoogleCalendarSyncService
import com.example.data.local.TaskDao
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskPulseItem
import com.example.data.model.TaskType
import com.example.data.remote.FirestoreSyncService
import com.example.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao,
    private val context: Context,
    private val firestoreSyncService: FirestoreSyncService? = null,
    private val googleCalendarSyncService: GoogleCalendarSyncService? = null
) {

    fun getAllItems(userId: String): Flow<List<TaskPulseItem>> = taskDao.getAllItemsFlow(userId)

    fun getAllBills(userId: String): Flow<List<TaskPulseItem>> = taskDao.getAllBillsFlow(userId)

    fun getUnpaidBills(userId: String): Flow<List<TaskPulseItem>> = taskDao.getUnpaidBillsFlow(userId)

    fun getUrgentItems(userId: String): Flow<List<TaskPulseItem>> = taskDao.getUrgentItemsFlow(userId)

    fun getItemsByCategory(userId: String, category: TaskCategory): Flow<List<TaskPulseItem>> {
        return taskDao.getItemsByCategoryFlow(userId, category.name)
    }

    fun searchItems(userId: String, query: String): Flow<List<TaskPulseItem>> {
        return taskDao.searchItemsFlow(userId, query)
    }

    suspend fun getItemById(userId: String, id: Long): TaskPulseItem? {
        return taskDao.getItemById(userId, id)
    }

    suspend fun getItemById(id: Long): TaskPulseItem? {
        return taskDao.getItemByIdAnyUser(id)
    }

    suspend fun getItemByIdAnyUser(id: Long): TaskPulseItem? {
        return taskDao.getItemByIdAnyUser(id)
    }

    suspend fun insertItem(item: TaskPulseItem): Long {
        val insertedId = taskDao.insertItem(item)
        var savedItem = item.copy(id = insertedId)
        ReminderScheduler.scheduleReminder(context, savedItem)

        // Automatic Google Calendar Sync for reminders and scheduled deadlines
        val calPrefs = googleCalendarSyncService?.calendarPrefs?.value
        val shouldSyncToCalendar = item.isSyncedToCalendar || (calPrefs?.isConnected == true && calPrefs.autoSyncReminders && (item.dueDate != null || item.reminderTime != null))
        if (shouldSyncToCalendar) {
            val syncResult = googleCalendarSyncService?.syncReminderToCalendar(savedItem)
            if (syncResult?.success == true) {
                savedItem = savedItem.copy(
                    isSyncedToCalendar = true,
                    googleCalendarEventId = syncResult.eventId
                )
                taskDao.updateItem(savedItem)
            }
        }

        firestoreSyncService?.pushTask(savedItem)
        return insertedId
    }

    suspend fun updateItem(item: TaskPulseItem) {
        var updatedItem = item
        if (item.isCompleted || (item.isBill && item.isPaid)) {
            ReminderScheduler.cancelReminder(context, item.id)
        } else {
            ReminderScheduler.scheduleReminder(context, item)
            val calPrefs = googleCalendarSyncService?.calendarPrefs?.value
            if (item.isSyncedToCalendar || (calPrefs?.isConnected == true && calPrefs.autoSyncReminders && (item.dueDate != null || item.reminderTime != null))) {
                val syncResult = googleCalendarSyncService?.syncReminderToCalendar(item)
                if (syncResult?.success == true) {
                    updatedItem = item.copy(
                        isSyncedToCalendar = true,
                        googleCalendarEventId = syncResult.eventId ?: item.googleCalendarEventId
                    )
                }
            }
        }
        taskDao.updateItem(updatedItem)
        firestoreSyncService?.pushTask(updatedItem)
    }

    suspend fun syncSingleItemToGoogleCalendar(item: TaskPulseItem): Boolean {
        val syncResult = googleCalendarSyncService?.syncReminderToCalendar(item)
        if (syncResult?.success == true) {
            val updated = item.copy(
                isSyncedToCalendar = true,
                googleCalendarEventId = syncResult.eventId ?: item.googleCalendarEventId
            )
            taskDao.updateItem(updated)
            firestoreSyncService?.pushTask(updated)
            return true
        }
        return false
    }

    suspend fun syncAllItemsToGoogleCalendar(userId: String): Int {
        val items = taskDao.getAllItemsSync(userId)
        val count = googleCalendarSyncService?.syncAllScheduledItems(items) ?: 0
        if (count > 0) {
            // Update items to marked synced
            for (it in items) {
                if (it.dueDate != null || it.reminderTime != null) {
                    if (!it.isSyncedToCalendar) {
                        val updated = it.copy(isSyncedToCalendar = true)
                        taskDao.updateItem(updated)
                    }
                }
            }
        }
        return count
    }

    suspend fun deleteItem(item: TaskPulseItem) {
        ReminderScheduler.cancelReminder(context, item.id)
        taskDao.deleteItem(item)
        firestoreSyncService?.deleteTask(item.userId, item.id)
    }

    suspend fun deleteItemById(userId: String, id: Long) {
        ReminderScheduler.cancelReminder(context, id)
        taskDao.deleteItemById(userId, id)
        firestoreSyncService?.deleteTask(userId, id)
    }

    suspend fun toggleCompleted(item: TaskPulseItem) {
        val newStatus = !item.isCompleted
        taskDao.updateCompletionStatus(item.userId, item.id, newStatus)
        val updated = item.copy(isCompleted = newStatus)
        if (newStatus) {
            ReminderScheduler.cancelReminder(context, item.id)
        } else {
            ReminderScheduler.scheduleReminder(context, updated)
        }
        firestoreSyncService?.pushTask(updated)
    }

    suspend fun togglePaid(item: TaskPulseItem) {
        val newPaidStatus = !item.isPaid
        taskDao.updatePaidStatus(item.userId, item.id, newPaidStatus)
        val updated = item.copy(isPaid = newPaidStatus)
        if (newPaidStatus) {
            ReminderScheduler.cancelReminder(context, item.id)
        } else {
            ReminderScheduler.scheduleReminder(context, updated)
        }
        firestoreSyncService?.pushTask(updated)
    }

    suspend fun snoozeReminder(userId: String, itemId: Long, minutes: Int = 15) {
        val newReminderTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        taskDao.snoozeReminder(userId, itemId, newReminderTime)
        val item = taskDao.getItemById(userId, itemId)
        if (item != null) {
            val updated = item.copy(reminderTime = newReminderTime)
            ReminderScheduler.scheduleReminder(context, updated)
            firestoreSyncService?.pushTask(updated)
        }
    }

    suspend fun snoozeReminder(itemId: Long, minutes: Int = 15) {
        val item = getItemByIdAnyUser(itemId) ?: return
        snoozeReminder(item.userId, itemId, minutes)
    }

    suspend fun rescheduleAllPendingReminders() {
        val pending = taskDao.getUpcomingPendingReminders(System.currentTimeMillis())
        for (item in pending) {
            ReminderScheduler.scheduleReminder(context, item)
        }
    }

    suspend fun clearCompleted(userId: String) {
        taskDao.clearCompletedItems(userId)
    }

    suspend fun seedSampleDataForUser(userId: String, userName: String) {
        val calendar = Calendar.getInstance()

        when {
            userId.contains("sarah") -> {
                // Sarah's distinct design & creative tasks
                val due1 = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 14); set(Calendar.MINUTE, 0) }
                val due2 = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 4); set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0) }

                insertItem(
                    TaskPulseItem(
                        userId = userId,
                        title = "Design Mobile App Wireframes & Figma Prototype",
                        description = "Create dark theme mockups and component design system",
                        category = TaskCategory.WORK.name,
                        type = TaskType.TASK.name,
                        priority = TaskPriority.URGENT.name,
                        dueDate = due1.timeInMillis,
                        reminderTime = due1.timeInMillis - 3600000L
                    )
                )

                insertItem(
                    TaskPulseItem(
                        userId = userId,
                        title = "Adobe Creative Cloud Subscription",
                        description = "Monthly designer license renewal",
                        category = TaskCategory.BILLS.name,
                        type = TaskType.BILL.name,
                        priority = TaskPriority.HIGH.name,
                        dueDate = due2.timeInMillis,
                        reminderTime = due2.timeInMillis,
                        amount = 54.99,
                        billPayee = "Adobe Systems",
                        isRecurring = true,
                        recurringInterval = "MONTHLY"
                    )
                )

                insertItem(
                    TaskPulseItem(
                        userId = userId,
                        title = "Buy Stylus Pen & Sketchbook",
                        description = "Fine-tip digital stylus for tablet illustration",
                        category = TaskCategory.SHOPPING.name,
                        type = TaskType.TODO.name,
                        priority = TaskPriority.LOW.name
                    )
                )
            }
            userId.contains("alex") -> {
                // Alex's finance tasks
                val due1 = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 16); set(Calendar.MINUTE, 0) }
                val due2 = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6); set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0) }

                insertItem(
                    TaskPulseItem(
                        userId = userId,
                        title = "Quarterly Tax Audit & Expense Report",
                        description = "Review spreadsheet invoices and vendor receipts",
                        category = TaskCategory.WORK.name,
                        type = TaskType.TASK.name,
                        priority = TaskPriority.HIGH.name,
                        dueDate = due1.timeInMillis,
                        reminderTime = due1.timeInMillis - 7200000L
                    )
                )

                insertItem(
                    TaskPulseItem(
                        userId = userId,
                        title = "Commercial Office Rent Payment",
                        description = "Monthly lease payment for co-working studio",
                        category = TaskCategory.BILLS.name,
                        type = TaskType.BILL.name,
                        priority = TaskPriority.URGENT.name,
                        dueDate = due2.timeInMillis,
                        reminderTime = due2.timeInMillis,
                        amount = 450.00,
                        billPayee = "Downtown Properties",
                        isRecurring = true,
                        recurringInterval = "MONTHLY"
                    )
                )
            }
            else -> {
                // Nouman / Default User tasks
                val billCal = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 3); set(Calendar.HOUR_OF_DAY, 17); set(Calendar.MINUTE, 0) }
                val workCal = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0) }
                val personalCal = (calendar.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0) }
                val wifiCal = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 5); set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0) }

                insertItem(
                    TaskPulseItem(
                        userId = userId,
                        title = "Electricity & Utility Bill",
                        description = "Monthly electric power payment via PG&E online portal",
                        category = TaskCategory.BILLS.name,
                        type = TaskType.BILL.name,
                        priority = TaskPriority.HIGH.name,
                        dueDate = billCal.timeInMillis,
                        reminderTime = billCal.timeInMillis - (24 * 3600 * 1000L),
                        amount = 84.50,
                        billPayee = "Electric Utility",
                        isRecurring = true,
                        recurringInterval = "MONTHLY"
                    )
                )

                insertItem(
                    TaskPulseItem(
                        userId = userId,
                        title = "Submit Q3 Product Roadmap Slides",
                        description = "Finalize quarterly projections and team deliverables deck",
                        category = TaskCategory.WORK.name,
                        type = TaskType.TASK.name,
                        priority = TaskPriority.URGENT.name,
                        dueDate = workCal.timeInMillis,
                        reminderTime = workCal.timeInMillis - (2 * 3600 * 1000L)
                    )
                )

                insertItem(
                    TaskPulseItem(
                        userId = userId,
                        title = "Buy groceries & fresh fruit for dinner",
                        description = "Milk, sourdough bread, avocados, Greek yogurt",
                        category = TaskCategory.SHOPPING.name,
                        type = TaskType.TODO.name,
                        priority = TaskPriority.MEDIUM.name,
                        dueDate = personalCal.timeInMillis,
                        reminderTime = personalCal.timeInMillis
                    )
                )

                insertItem(
                    TaskPulseItem(
                        userId = userId,
                        title = "High-speed Internet & Fiber Bill",
                        description = "Auto-pay monthly broadband bill",
                        category = TaskCategory.BILLS.name,
                        type = TaskType.BILL.name,
                        priority = TaskPriority.MEDIUM.name,
                        dueDate = wifiCal.timeInMillis,
                        reminderTime = wifiCal.timeInMillis,
                        amount = 65.00,
                        billPayee = "FiberNet",
                        isRecurring = true,
                        recurringInterval = "MONTHLY"
                    )
                )
            }
        }
    }
}
