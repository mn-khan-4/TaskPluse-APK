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
        val surface = if (isAmoled) Color(0xFF101010) else ElegantSurface
        val surfaceVariant = if (isAmoled) Color(0xFF1A1A1A) else ElegantSurfaceVariant
        val border = if (isAmoled) Color(0xFF262626) else ElegantBorder

        darkColorScheme(
            primary = accent.darkPrimary,
            onPrimary = accent.darkOnPrimary,
            primaryContainer = accent.darkContainer,
            onPrimaryContainer = accent.darkOnContainer,
            secondary = accent.darkPrimary.copy(alpha = 0.85f),
            onSecondary = accent.darkOnPrimary,
            secondaryContainer = accent.darkContainer.copy(alpha = 0.7f),
            onSecondaryContainer = accent.darkOnContainer,
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
            outlineVariant = border.copy(alpha = 0.5f),
            surfaceContainerHigh = surfaceVariant,
            surfaceContainerLowest = bg,
            error = StatusDanger,
            onError = Color.White,
            errorContainer = Color(0xFF451A1A),
            onErrorContainer = Color(0xFFFCA5A5)
        )
    } else {
        // High-contrast, clean modern Light Theme
        lightColorScheme(
            primary = accent.lightPrimary,
            onPrimary = accent.lightOnPrimary,
            primaryContainer = accent.lightContainer,
            onPrimaryContainer = accent.lightOnContainer,
            secondary = accent.lightPrimary.copy(alpha = 0.85f),
            onSecondary = Color.White,
            secondaryContainer = accent.lightContainer,
            onSecondaryContainer = accent.lightOnContainer,
            tertiary = LightCategoryBills,
            onTertiary = Color.White,
            tertiaryContainer = LightCategoryBillsBg,
            onTertiaryContainer = LightCategoryBills,
            background = LightBackground,
            onBackground = LightTextPrimary,
            surface = LightSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightTextSecondary,
            outline = LightBorder,
            outlineVariant = LightBorder.copy(alpha = 0.6f),
            surfaceContainerHigh = LightSurfaceVariant,
            surfaceContainerLowest = LightBackground,
            error = StatusDanger,
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
