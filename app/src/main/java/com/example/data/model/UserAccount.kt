package com.example.data.model

data class UserAccount(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: String = "Productivity Member",
    val avatarEmoji: String = "⚡",
    val avatarColorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false
) {
    companion object {
        val DEFAULT_DEMO_USERS = listOf(
            UserAccount(
                uid = "user_nouman_prod",
                email = "noumanjamil2004@gmail.com",
                displayName = "Nouman",
                role = "Lead Creator",
                avatarEmoji = "⚡",
                avatarColorIndex = 0,
                isDemo = true
            ),
            UserAccount(
                uid = "user_sarah_design",
                email = "sarah.design@example.com",
                displayName = "Sarah Miller",
                role = "Product Designer",
                avatarEmoji = "🎨",
                avatarColorIndex = 1,
                isDemo = true
            ),
            UserAccount(
                uid = "user_alex_finance",
                email = "alex.finance@example.com",
                displayName = "Alex Carter",
                role = "Finance Operations",
                avatarEmoji = "💼",
                avatarColorIndex = 2,
                isDemo = true
            )
        )
    }
}
