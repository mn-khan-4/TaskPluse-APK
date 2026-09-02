package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.TaskDao
import com.example.data.model.TaskPulseItem
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class SyncState {
    data object Synced : SyncState()
    data object Syncing : SyncState()
    data object LocalOnly : SyncState()
    data class Error(val message: String) : SyncState()
}

class FirestoreSyncService(
    private val context: Context,
    private val taskDao: TaskDao,
    private val scope: CoroutineScope
) {
    private var firestore: FirebaseFirestore? = null
    private var snapshotListener: ListenerRegistration? = null
    private var currentActiveUserId: String? = null

    private val _syncState = MutableStateFlow<SyncState>(SyncState.LocalOnly)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        initFirebaseIfAvailable()
    }

    private fun initFirebaseIfAvailable() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance()
                _syncState.value = SyncState.Synced
            } else {
                _syncState.value = SyncState.LocalOnly
            }
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Firebase not initialized, running in local-first database mode: ${e.message}")
            _syncState.value = SyncState.LocalOnly
        }
    }

    fun startListeningForUser(userId: String) {
        if (userId.isBlank()) return
        stopListening()
        currentActiveUserId = userId

        val db = firestore
        if (db == null) {
            initFirebaseIfAvailable()
        }

        val activeDb = firestore ?: run {
            _syncState.value = SyncState.LocalOnly
            return
        }

        _syncState.value = SyncState.Syncing
        try {
            snapshotListener = activeDb.collection("users")
                .document(userId)
                .collection("tasks")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("FirestoreSync", "Listen error: ${error.message}")
                        _syncState.value = SyncState.Error("Cloud sync offline: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val remoteItems = snapshot.documents.mapNotNull { doc ->
                                    try {
                                        val data = doc.data ?: return@mapNotNull null
                                        val id = (data["id"] as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: 0L
                                        TaskPulseItem(
                                            id = id,
                                            userId = userId,
                                            title = data["title"] as? String ?: "Untitled",
                                            description = data["description"] as? String ?: "",
                                            category = data["category"] as? String ?: "OTHER",
                                            type = data["type"] as? String ?: "TASK",
                                            priority = data["priority"] as? String ?: "MEDIUM",
                                            dueDate = (data["dueDate"] as? Number)?.toLong(),
                                            reminderTime = (data["reminderTime"] as? Number)?.toLong(),
                                            isCompleted = data["isCompleted"] as? Boolean ?: false,
                                            isPaid = data["isPaid"] as? Boolean ?: false,
                                            amount = (data["amount"] as? Number)?.toDouble(),
                                            billPayee = data["billPayee"] as? String,
                                            isRecurring = data["isRecurring"] as? Boolean ?: false,
                                            recurringInterval = data["recurringInterval"] as? String,
                                            rawVoiceTranscript = data["rawVoiceTranscript"] as? String,
                                            googleCalendarEventId = data["googleCalendarEventId"] as? String,
                                            isSyncedToCalendar = data["isSyncedToCalendar"] as? Boolean ?: false,
                                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                if (remoteItems.isNotEmpty()) {
                                    taskDao.insertItems(remoteItems)
                                }
                                _syncState.value = SyncState.Synced
                            } catch (e: Exception) {
                                Log.e("FirestoreSync", "Failed to sync remote documents to Room", e)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Cannot attach snapshot listener: ${e.message}")
            _syncState.value = SyncState.LocalOnly
        }
    }

    fun stopListening() {
        snapshotListener?.remove()
        snapshotListener = null
        currentActiveUserId = null
    }

    suspend fun pushTask(item: TaskPulseItem) {
        val db = firestore ?: return
        val uid = item.userId
        if (uid.isBlank()) return

        _syncState.value = SyncState.Syncing
        try {
            val taskMap = hashMapOf(
                "id" to item.id,
                "userId" to item.userId,
                "title" to item.title,
                "description" to item.description,
                "category" to item.category,
                "type" to item.type,
                "priority" to item.priority,
                "dueDate" to item.dueDate,
                "reminderTime" to item.reminderTime,
                "isCompleted" to item.isCompleted,
                "isPaid" to item.isPaid,
                "amount" to item.amount,
                "billPayee" to item.billPayee,
                "isRecurring" to item.isRecurring,
                "recurringInterval" to item.recurringInterval,
                "rawVoiceTranscript" to item.rawVoiceTranscript,
                "googleCalendarEventId" to item.googleCalendarEventId,
                "isSyncedToCalendar" to item.isSyncedToCalendar,
                "createdAt" to item.createdAt
            )

            db.collection("users")
                .document(uid)
                .collection("tasks")
                .document(item.id.toString())
                .set(taskMap, SetOptions.merge())
                .await()

            _syncState.value = SyncState.Synced
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Failed to push task to cloud: ${e.message}")
            _syncState.value = SyncState.Error("Cloud push deferred: ${e.message}")
        }
    }

    suspend fun deleteTask(userId: String, taskId: Long) {
        val db = firestore ?: return
        if (userId.isBlank()) return
        try {
            db.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Failed to delete task from cloud: ${e.message}")
        }
    }
}
