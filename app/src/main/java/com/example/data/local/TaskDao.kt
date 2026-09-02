package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TaskPulseItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM task_items WHERE userId = :userId ORDER BY isCompleted ASC, isPaid ASC, CASE WHEN dueDate IS NOT NULL THEN dueDate ELSE reminderTime END ASC, createdAt DESC")
    fun getAllItemsFlow(userId: String): Flow<List<TaskPulseItem>>

    @Query("SELECT * FROM task_items WHERE userId = :userId")
    suspend fun getAllItemsSync(userId: String): List<TaskPulseItem>

    @Query("SELECT * FROM task_items WHERE userId = :userId AND category = :category ORDER BY isCompleted ASC, isPaid ASC, CASE WHEN dueDate IS NOT NULL THEN dueDate ELSE reminderTime END ASC, createdAt DESC")
    fun getItemsByCategoryFlow(userId: String, category: String): Flow<List<TaskPulseItem>>

    @Query("SELECT * FROM task_items WHERE userId = :userId AND (type = 'BILL' OR category = 'BILLS' OR amount IS NOT NULL) ORDER BY isPaid ASC, dueDate ASC, createdAt DESC")
    fun getAllBillsFlow(userId: String): Flow<List<TaskPulseItem>>

    @Query("SELECT * FROM task_items WHERE userId = :userId AND (type = 'BILL' OR category = 'BILLS' OR amount IS NOT NULL) AND isPaid = 0 ORDER BY dueDate ASC")
    fun getUnpaidBillsFlow(userId: String): Flow<List<TaskPulseItem>>

    @Query("SELECT * FROM task_items WHERE userId = :userId AND (category = 'URGENT' OR priority = 'URGENT') AND isCompleted = 0 AND isPaid = 0 ORDER BY CASE WHEN dueDate IS NOT NULL THEN dueDate ELSE reminderTime END ASC")
    fun getUrgentItemsFlow(userId: String): Flow<List<TaskPulseItem>>

    @Query("SELECT * FROM task_items WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR billPayee LIKE '%' || :query || '%') ORDER BY isCompleted ASC, isPaid ASC, createdAt DESC")
    fun searchItemsFlow(userId: String, query: String): Flow<List<TaskPulseItem>>

    @Query("SELECT * FROM task_items WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getItemById(userId: String, id: Long): TaskPulseItem?

    @Query("SELECT * FROM task_items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdAnyUser(id: Long): TaskPulseItem?

    @Query("SELECT * FROM task_items WHERE reminderTime IS NOT NULL AND reminderTime > :currentTime AND isCompleted = 0 AND isPaid = 0")
    suspend fun getUpcomingPendingReminders(currentTime: Long): List<TaskPulseItem>

    @Query("SELECT * FROM task_items WHERE userId = :userId AND reminderTime IS NOT NULL AND reminderTime > :currentTime AND isCompleted = 0 AND isPaid = 0")
    suspend fun getUpcomingPendingRemindersForUser(userId: String, currentTime: Long): List<TaskPulseItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TaskPulseItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<TaskPulseItem>)

    @Update
    suspend fun updateItem(item: TaskPulseItem)

    @Delete
    suspend fun deleteItem(item: TaskPulseItem)

    @Query("DELETE FROM task_items WHERE id = :id AND userId = :userId")
    suspend fun deleteItemById(userId: String, id: Long)

    @Query("UPDATE task_items SET isCompleted = :completed WHERE id = :id AND userId = :userId")
    suspend fun updateCompletionStatus(userId: String, id: Long, completed: Boolean)

    @Query("UPDATE task_items SET isPaid = :paid WHERE id = :id AND userId = :userId")
    suspend fun updatePaidStatus(userId: String, id: Long, paid: Boolean)

    @Query("UPDATE task_items SET reminderTime = :newReminderTime WHERE id = :id AND userId = :userId")
    suspend fun snoozeReminder(userId: String, id: Long, newReminderTime: Long)

    @Query("DELETE FROM task_items WHERE userId = :userId AND (isCompleted = 1 OR (isPaid = 1 AND (type = 'BILL' OR category = 'BILLS')))")
    suspend fun clearCompletedItems(userId: String)

    @Query("DELETE FROM task_items WHERE userId = :userId")
    suspend fun deleteAllItemsForUser(userId: String)
}
