package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TaskCategory(val displayName: String, val iconName: String) {
    WORK("Work", "work"),
    PERSONAL("Personal", "person"),
    URGENT("Urgent", "warning"),
    BILLS("Bills", "receipt"),
    SHOPPING("Shopping", "shopping_cart"),
    HEALTH("Health", "health_and_safety"),
    OTHER("Other", "label");

    companion object {
        fun fromString(value: String?): TaskCategory {
            if (value.isNullOrBlank()) return OTHER
            val clean = value.trim().uppercase(Locale.getDefault())
            return entries.firstOrNull { it.name == clean || it.displayName.uppercase(Locale.getDefault()) == clean }
                ?: when {
                    clean.contains("WORK") || clean.contains("JOB") || clean.contains("OFFICE") || clean.contains("MEETING") -> WORK
                    clean.contains("URGENT") || clean.contains("ASAP") || clean.contains("EMERGENCY") || clean.contains("HIGH") -> URGENT
                    clean.contains("BILL") || clean.contains("PAY") || clean.contains("RENT") || clean.contains("UTILITY") || clean.contains("SUBSCRIPTION") -> BILLS
                    clean.contains("SHOP") || clean.contains("GROCER") || clean.contains("BUY") -> SHOPPING
                    clean.contains("HEALTH") || clean.contains("DOCTOR") || clean.contains("MED") || clean.contains("GYM") -> HEALTH
                    clean.contains("PERSON") || clean.contains("HOME") || clean.contains("FAMILY") -> PERSONAL
                    else -> OTHER
                }
        }
    }
}

enum class TaskType(val displayName: String) {
    TASK("Task"),
    TODO("To-Do"),
    BILL("Bill"),
    REMINDER("Reminder");

    companion object {
        fun fromString(value: String?): TaskType {
            if (value.isNullOrBlank()) return TASK
            val clean = value.trim().uppercase(Locale.getDefault())
            return entries.firstOrNull { it.name == clean || it.displayName.uppercase(Locale.getDefault()) == clean }
                ?: when {
                    clean.contains("BILL") || clean.contains("PAY") || clean.contains("RENT") || clean.contains("INVOICE") -> BILL
                    clean.contains("REMIND") -> REMINDER
                    clean.contains("TODO") || clean.contains("TO-DO") -> TODO
                    else -> TASK
                }
        }
    }
}

enum class TaskPriority(val displayName: String, val rank: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    companion object {
        fun fromString(value: String?): TaskPriority {
            if (value.isNullOrBlank()) return MEDIUM
            val clean = value.trim().uppercase(Locale.getDefault())
            return entries.firstOrNull { it.name == clean || it.displayName.uppercase(Locale.getDefault()) == clean }
                ?: when {
                    clean.contains("URGENT") || clean.contains("CRITICAL") || clean.contains("ASAP") -> URGENT
                    clean.contains("HIGH") || clean.contains("IMPORTANT") -> HIGH
                    clean.contains("LOW") -> LOW
                    else -> MEDIUM
                }
        }
    }
}

@Entity(tableName = "task_items")
data class TaskPulseItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val title: String,
    val description: String = "",
    val category: String = TaskCategory.OTHER.name,
    val type: String = TaskType.TASK.name,
    val priority: String = TaskPriority.MEDIUM.name,
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val isCompleted: Boolean = false,
    val isPaid: Boolean = false,
    val amount: Double? = null,
    val billPayee: String? = null,
    val isRecurring: Boolean = false,
    val recurringInterval: String? = null, // "MONTHLY", "WEEKLY", "YEARLY"
    val rawVoiceTranscript: String? = null,
    val googleCalendarEventId: String? = null,
    val isSyncedToCalendar: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val taskCategory: TaskCategory
        get() = TaskCategory.fromString(category)

    val taskType: TaskType
        get() = TaskType.fromString(type)

    val taskPriority: TaskPriority
        get() = TaskPriority.fromString(priority)

    val isBill: Boolean
        get() = taskType == TaskType.BILL || taskCategory == TaskCategory.BILLS || amount != null

    fun getFormattedDueDate(): String? {
        if (dueDate == null) return null
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val todayYear = calendar.get(Calendar.YEAR)

        val dueCal = Calendar.getInstance().apply { timeInMillis = dueDate }
        val dueDay = dueCal.get(Calendar.DAY_OF_YEAR)
        val dueYear = dueCal.get(Calendar.YEAR)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dueDate))
        return when {
            todayYear == dueYear && today == dueDay -> "Today at $timeFormat"
            todayYear == dueYear && dueDay - today == 1 -> "Tomorrow at $timeFormat"
            todayYear == dueYear && today - dueDay == 1 -> "Yesterday at $timeFormat"
            else -> SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(dueDate))
        }
    }

    fun getFormattedReminderTime(): String? {
        if (reminderTime == null) return null
        return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(reminderTime))
    }

    fun isOverdue(): Boolean {
        if (isCompleted || (isBill && isPaid)) return false
        val now = System.currentTimeMillis()
        return (dueDate != null && dueDate < now) || (reminderTime != null && reminderTime < now)
    }

    fun isDueSoon(hours: Int = 24): Boolean {
        if (isCompleted || (isBill && isPaid)) return false
        val now = System.currentTimeMillis()
        val target = dueDate ?: reminderTime ?: return false
        val diff = target - now
        return diff in 1..(hours * 3600 * 1000L)
    }

    fun getFormattedAmount(): String {
        return if (amount != null && amount > 0) {
            String.format(Locale.US, "$%.2f", amount)
        } else {
            ""
        }
    }
}
