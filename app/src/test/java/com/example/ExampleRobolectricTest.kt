package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppAccentColor
import com.example.data.local.AppThemeMode
import com.example.data.local.TaskPulseDatabase
import com.example.data.local.UserPreferences
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskPulseItem
import com.example.data.model.UserAccount
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: TaskPulseDatabase
    private lateinit var repository: TaskRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, TaskPulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TaskRepository(database.taskDao(), context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verify app name resource`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("TaskPulse", appName)
    }

    @Test
    fun `verify multi-user database isolation`() = runBlocking {
        val user1Id = "user_nouman"
        val user2Id = "user_sarah"

        // Insert tasks for User 1
        repository.insertItem(
            TaskPulseItem(
                userId = user1Id,
                title = "Nouman's Private Task",
                category = TaskCategory.WORK.name,
                priority = TaskPriority.HIGH.name
            )
        )
        repository.insertItem(
            TaskPulseItem(
                userId = user1Id,
                title = "Nouman's Electric Bill",
                category = TaskCategory.BILLS.name,
                priority = TaskPriority.URGENT.name,
                amount = 85.0
            )
        )

        // Insert tasks for User 2
        repository.insertItem(
            TaskPulseItem(
                userId = user2Id,
                title = "Sarah's Design Review",
                category = TaskCategory.WORK.name,
                priority = TaskPriority.URGENT.name
            )
        )

        // Verify User 1 only sees their 2 items
        val user1Items = repository.getAllItems(user1Id).first()
        assertEquals(2, user1Items.size)
        assertTrue(user1Items.all { it.userId == user1Id })
        assertTrue(user1Items.none { it.title.contains("Sarah") })

        // Verify User 2 only sees their 1 item
        val user2Items = repository.getAllItems(user2Id).first()
        assertEquals(1, user2Items.size)
        assertEquals("Sarah's Design Review", user2Items.first().title)
        assertTrue(user2Items.none { it.title.contains("Nouman") })
    }

    @Test
    fun `verify UserPreferences theme and accent customization`() {
        val prefs = UserPreferences(context)

        prefs.setThemeMode(AppThemeMode.AMOLED)
        assertEquals(AppThemeMode.AMOLED, prefs.themeMode.value)

        prefs.setAccentColor(AppAccentColor.EMERALD)
        assertEquals(AppAccentColor.EMERALD, prefs.accentColor.value)
    }

    @Test
    fun `verify task overdue and due soon logic`() {
        val now = System.currentTimeMillis()
        val overdueItem = TaskPulseItem(
            title = "Overdue task",
            dueDate = now - 3600000L,
            isCompleted = false
        )
        assertTrue(overdueItem.isOverdue())

        val completedItem = TaskPulseItem(
            title = "Completed task",
            dueDate = now - 3600000L,
            isCompleted = true
        )
        assertFalse(completedItem.isOverdue())
    }

    @Test
    fun `verify priority ranking and sorting`() {
        val itemLow = TaskPulseItem(title = "Low item", priority = TaskPriority.LOW.name)
        val itemMed = TaskPulseItem(title = "Med item", priority = TaskPriority.MEDIUM.name)
        val itemHigh = TaskPulseItem(title = "High item", priority = TaskPriority.HIGH.name)
        val itemUrgent = TaskPulseItem(title = "Urgent item", priority = TaskPriority.URGENT.name)

        val list = listOf(itemLow, itemHigh, itemMed, itemUrgent)
        val sortedByPriority = list.sortedByDescending { it.taskPriority.rank }

        assertEquals(TaskPriority.URGENT, sortedByPriority[0].taskPriority)
        assertEquals(TaskPriority.HIGH, sortedByPriority[1].taskPriority)
        assertEquals(TaskPriority.MEDIUM, sortedByPriority[2].taskPriority)
        assertEquals(TaskPriority.LOW, sortedByPriority[3].taskPriority)
    }

    @Test
    fun `verify search filter matching on title or category`() {
        val task1 = TaskPulseItem(title = "Doctor appointment", category = TaskCategory.HEALTH.name)
        val task2 = TaskPulseItem(title = "Pay water bill", category = TaskCategory.BILLS.name)
        val task3 = TaskPulseItem(title = "Submit quarterly review", category = TaskCategory.WORK.name)

        val items = listOf(task1, task2, task3)

        // Filter by title "water"
        val queryTitle = "water"
        val matchTitle = items.filter {
            it.title.contains(queryTitle, ignoreCase = true) ||
            it.category.contains(queryTitle, ignoreCase = true) ||
            it.taskCategory.displayName.contains(queryTitle, ignoreCase = true)
        }
        assertEquals(1, matchTitle.size)
        assertEquals("Pay water bill", matchTitle[0].title)

        // Filter by category "health"
        val queryCat = "health"
        val matchCat = items.filter {
            it.title.contains(queryCat, ignoreCase = true) ||
            it.category.contains(queryCat, ignoreCase = true) ||
            it.taskCategory.displayName.contains(queryCat, ignoreCase = true)
        }
        assertEquals(1, matchCat.size)
        assertEquals("Doctor appointment", matchCat[0].title)
    }
}
