package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.local.AppAccentColor
import com.example.data.local.AppThemeMode

// CompositionLocal to easily access active accent color anywhere
val LocalAppAccentColor = staticCompositionLocalOf { AppAccentColor.LAVENDER }

fun getAppColorScheme(
    themeMode: AppThemeMode,
    accent: AppAccentColor,
    isSystemDark: Boolean
): ColorScheme {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.AMOLED -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemDark
    }

    val isAmoled = themeMode == AppThemeMode.AMOLED

    return if (isDark) {
        val bg = if (isAmoled) Color(0xFF000000) else ElegantBackground
        val surface = if (isAmoled) Color(0xFF121212) else ElegantSurface
        val surfaceVariant = if (isAmoled) Color(0xFF1E1E1E) else ElegantSurfaceVariant
        val border = if (isAmoled) Color(0xFF2E2E2E) else ElegantBorder

        darkColorScheme(
            primary = accent.primaryColor,
            onPrimary = accent.onPrimaryColor,
            primaryContainer = accent.containerColor,
            onPrimaryContainer = Color.White,
            secondary = accent.primaryColor.copy(alpha = 0.8f),
            onSecondary = accent.onPrimaryColor,
            secondaryContainer = accent.containerColor.copy(alpha = 0.7f),
            onSecondaryContainer = Color.White,
            tertiary = CategoryBills,
            onTertiary = Color(0xFF003822),
            tertiaryContainer = CategoryBillsBg,
            onTertiaryContainer = CategoryBills,
            background = bg,
            onBackground = ElegantTextPrimary,
            surface = surface,
            onSurface = ElegantTextPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = ElegantTextSecondary,
            outline = border,
            error = StatusDanger,
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = CategoryUrgent
        )
    } else {
        // Minimal Light Theme
        lightColorScheme(
            primary = accent.primaryColor,
            onPrimary = Color.White,
            primaryContainer = accent.primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = accent.onPrimaryColor,
            secondary = accent.primaryColor.copy(alpha = 0.7f),
            onSecondary = Color.White,
            secondaryContainer = accent.containerColor.copy(alpha = 0.2f),
            onSecondaryContainer = Color(0xFF1E1B4B),
            tertiary = Color(0xFF059669),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFD1FAE5),
            onTertiaryContainer = Color(0xFF065F46),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF64748B),
            outline = Color(0xFFE2E8F0),
            error = Color(0xFFDC2626),
            onError = Color.White,
            errorContainer = Color(0xFFFEE2E2),
            onErrorContainer = Color(0xFF991B1B)
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    accentColor: AppAccentColor = AppAccentColor.LAVENDER,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = getAppColorScheme(themeMode, accentColor, isSystemDark)

    CompositionLocalProvider(LocalAppAccentColor provides accentColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
