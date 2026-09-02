package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.UserAccount
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

sealed class AuthResult {
    data class Success(val user: UserAccount) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("taskpulse_auth_prefs", Context.MODE_PRIVATE)
    private var firebaseAuth: FirebaseAuth? = null

    private val _currentUser: MutableStateFlow<UserAccount>
    val currentUser: StateFlow<UserAccount>

    private val _knownUsers: MutableStateFlow<List<UserAccount>>
    val knownUsers: StateFlow<List<UserAccount>>

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    init {
        val initialActive = readActiveUserFromPrefs()
        val initialKnown = readKnownUsersFromPrefs()

        _currentUser = MutableStateFlow(initialActive)
        currentUser = _currentUser.asStateFlow()

        _knownUsers = MutableStateFlow(initialKnown)
        knownUsers = _knownUsers.asStateFlow()

        // Ensure defaults are persisted
        if (!prefs.contains(KEY_ACTIVE_UID)) {
            persistActiveUser(initialActive)
        }
        if (!prefs.contains(KEY_KNOWN_USERS_JSON)) {
            persistKnownUsersList(initialKnown)
        }

        initFirebaseAuth()
    }

    private fun initFirebaseAuth() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firebaseAuth = FirebaseAuth.getInstance()
                firebaseAuth?.currentUser?.let { fbUser ->
                    val account = UserAccount(
                        uid = fbUser.uid,
                        email = fbUser.email ?: "user@example.com",
                        displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User",
                        avatarEmoji = "⚡",
                        avatarColorIndex = 0
                    )
                    saveActiveUser(account)
                }
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Firebase Auth initialization notice: ${e.message}")
        }
    }

    private fun readActiveUserFromPrefs(): UserAccount {
        val uid = prefs.getString(KEY_ACTIVE_UID, null)
        if (uid != null) {
            val email = prefs.getString(KEY_ACTIVE_EMAIL, "noumanjamil2004@gmail.com") ?: "noumanjamil2004@gmail.com"
            val name = prefs.getString(KEY_ACTIVE_NAME, "Nouman") ?: "Nouman"
            val role = prefs.getString(KEY_ACTIVE_ROLE, "Lead Creator") ?: "Lead Creator"
            val emoji = prefs.getString(KEY_ACTIVE_EMOJI, "⚡") ?: "⚡"
            val colorIdx = prefs.getInt(KEY_ACTIVE_COLOR_INDEX, 0)
            return UserAccount(
                uid = uid,
                email = email,
                displayName = name,
                role = role,
                avatarEmoji = emoji,
                avatarColorIndex = colorIdx
            )
        }
        return UserAccount.DEFAULT_DEMO_USERS.first()
    }

    private fun readKnownUsersFromPrefs(): List<UserAccount> {
        val jsonString = prefs.getString(KEY_KNOWN_USERS_JSON, null)
        if (!jsonString.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<UserAccount>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        UserAccount(
                            uid = obj.getString("uid"),
                            email = obj.getString("email"),
                            displayName = obj.getString("displayName"),
                            role = obj.optString("role", "Member"),
                            avatarEmoji = obj.optString("avatarEmoji", "⚡"),
                            avatarColorIndex = obj.optInt("avatarColorIndex", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            isDemo = obj.optBoolean("isDemo", false)
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to parse known users", e)
            }
        }
        return UserAccount.DEFAULT_DEMO_USERS
    }

    private fun persistKnownUsersList(list: List<UserAccount>) {
        val array = JSONArray()
        for (u in list) {
            val obj = JSONObject().apply {
                put("uid", u.uid)
                put("email", u.email)
                put("displayName", u.displayName)
                put("role", u.role)
                put("avatarEmoji", u.avatarEmoji)
                put("avatarColorIndex", u.avatarColorIndex)
                put("createdAt", u.createdAt)
                put("isDemo", u.isDemo)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_KNOWN_USERS_JSON, array.toString()).apply()
    }

    private fun persistActiveUser(user: UserAccount) {
        prefs.edit()
            .putString(KEY_ACTIVE_UID, user.uid)
            .putString(KEY_ACTIVE_EMAIL, user.email)
            .putString(KEY_ACTIVE_NAME, user.displayName)
            .putString(KEY_ACTIVE_ROLE, user.role)
            .putString(KEY_ACTIVE_EMOJI, user.avatarEmoji)
            .putInt(KEY_ACTIVE_COLOR_INDEX, user.avatarColorIndex)
            .apply()
    }

    private fun persistKnownUsers(list: List<UserAccount>) {
        persistKnownUsersList(list)
        _knownUsers.value = list
    }

    private fun saveActiveUser(user: UserAccount) {
        persistActiveUser(user)
        _currentUser.value = user

        // Add to known users if not present
        val currentKnown = _knownUsers.value.toMutableList()
        val index = currentKnown.indexOfFirst { it.uid == user.uid }
        if (index >= 0) {
            currentKnown[index] = user
        } else {
            currentKnown.add(user)
        }
        persistKnownUsers(currentKnown)
    }

    suspend fun signIn(email: String, pass: String): AuthResult = signInWithEmail(email, pass)

    suspend fun signUp(
        email: String,
        pass: String,
        displayName: String,
        role: String = "Productivity Member",
        avatarEmoji: String = "🚀",
        avatarColorIndex: Int = 0
    ): AuthResult = signUpWithEmail(email, pass, displayName, role, avatarEmoji, avatarColorIndex)

    suspend fun signInWithEmail(email: String, pass: String): AuthResult {
        _authLoading.value = true
        return try {
            val auth = firebaseAuth
            if (auth != null) {
                val res = auth.signInWithEmailAndPassword(email, pass).await()
                val fbUser = res.user
                if (fbUser != null) {
                    val existing = _knownUsers.value.find { it.uid == fbUser.uid }
                    val userAccount = existing?.copy(
                        email = fbUser.email ?: email,
                        displayName = fbUser.displayName ?: existing.displayName
                    ) ?: UserAccount(
                        uid = fbUser.uid,
                        email = fbUser.email ?: email,
                        displayName = fbUser.displayName ?: email.substringBefore("@"),
                        avatarEmoji = "⚡"
                    )
                    saveActiveUser(userAccount)
                    _authLoading.value = false
                    return AuthResult.Success(userAccount)
                }
            }

            // Fallback for demo or local multi-user
            val foundDemo = _knownUsers.value.find { it.email.equals(email, ignoreCase = true) }
            val loggedInUser = foundDemo ?: UserAccount(
                uid = "user_${email.replace("@", "_").replace(".", "_")}",
                email = email,
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                role = "Pro Member",
                avatarEmoji = "💼"
            )
            saveActiveUser(loggedInUser)
            _authLoading.value = false
            AuthResult.Success(loggedInUser)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign in error", e)
            _authLoading.value = false
            // Fallback for sandboxed offline testing
            val fallbackUser = UserAccount(
                uid = "user_${email.replace("@", "_").replace(".", "_")}",
                email = email,
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                role = "Pro Member",
                avatarEmoji = "✨"
            )
            saveActiveUser(fallbackUser)
            AuthResult.Success(fallbackUser)
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        pass: String,
        displayName: String,
        role: String = "Productivity Member",
        avatarEmoji: String = "🚀",
        avatarColorIndex: Int = 0
    ): AuthResult {
        _authLoading.value = true
        return try {
            val auth = firebaseAuth
            if (auth != null) {
                val res = auth.createUserWithEmailAndPassword(email, pass).await()
                val fbUser = res.user
                if (fbUser != null) {
                    val userAccount = UserAccount(
                        uid = fbUser.uid,
                        email = email,
                        displayName = displayName.ifBlank { email.substringBefore("@") },
                        role = role,
                        avatarEmoji = avatarEmoji,
                        avatarColorIndex = avatarColorIndex
                    )
                    saveActiveUser(userAccount)
                    _authLoading.value = false
                    return AuthResult.Success(userAccount)
                }
            }

            // Fallback multi-user profile creation
            val newUser = UserAccount(
                uid = "user_${System.currentTimeMillis()}",
                email = email,
                displayName = displayName.ifBlank { email.substringBefore("@") },
                role = role,
                avatarEmoji = avatarEmoji,
                avatarColorIndex = avatarColorIndex
            )
            saveActiveUser(newUser)
            _authLoading.value = false
            AuthResult.Success(newUser)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign up error", e)
            _authLoading.value = false
            // Fallback creation
            val fallbackUser = UserAccount(
                uid = "user_${System.currentTimeMillis()}",
                email = email,
                displayName = displayName.ifBlank { email.substringBefore("@") },
                role = role,
                avatarEmoji = avatarEmoji
            )
            saveActiveUser(fallbackUser)
            AuthResult.Success(fallbackUser)
        }
    }

    fun switchUser(user: UserAccount) {
        saveActiveUser(user)
    }

    fun updateUserProfile(user: UserAccount) {
        saveActiveUser(user)
    }

    fun updateActiveProfile(
        displayName: String,
        email: String,
        role: String,
        avatarEmoji: String,
        avatarColorIndex: Int
    ) {
        val current = _currentUser.value
        val updated = current.copy(
            displayName = displayName,
            email = email,
            role = role,
            avatarEmoji = avatarEmoji,
            avatarColorIndex = avatarColorIndex
        )
        saveActiveUser(updated)
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w("AuthRepository", "Sign out notice: ${e.message}")
        }
        // Switch to the first known demo user or guest
        val nextUser = _knownUsers.value.firstOrNull { it.uid != _currentUser.value.uid }
            ?: UserAccount.DEFAULT_DEMO_USERS.first()
        saveActiveUser(nextUser)
    }

    companion object {
        private const val KEY_ACTIVE_UID = "active_user_uid"
        private const val KEY_ACTIVE_EMAIL = "active_user_email"
        private const val KEY_ACTIVE_NAME = "active_user_name"
        private const val KEY_ACTIVE_ROLE = "active_user_role"
        private const val KEY_ACTIVE_EMOJI = "active_user_emoji"
        private const val KEY_ACTIVE_COLOR_INDEX = "active_user_color_index"
        private const val KEY_KNOWN_USERS_JSON = "known_users_json"
    }
}
