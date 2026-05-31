package com.cubiquitous.tracura.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

@Immutable
data class NotificationPopupPalette(
    val tier1Background: Color,
    val tier2Surface: Color,
    val tier3Surface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val divider: Color,
    val accent: Color,
    val unreadSurface: Color,
    val neutralIcon: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val project: Color
)

@Composable
fun notificationPopupPalette(): NotificationPopupPalette {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        NotificationPopupPalette(
            tier1Background = Color(0xFF000000),
            tier2Surface = Color(0xFF1C1C1E),
            tier3Surface = Color(0xFF2C2C2E),
            primaryText = Color(0xFFFFFFFF),
            secondaryText = Color(0x99EBEBF5),
            divider = Color(0xFF38383A),
            accent = Color(0xFF4285F4),
            unreadSurface = Color(0xFF2C2C2E),
            neutralIcon = Color(0x99EBEBF5),
            success = Color(0xFF34C759),
            warning = Color(0xFFFF9F0A),
            danger = Color(0xFFFF453A),
            project = Color(0xFFBF5AF2)
        )
    } else {
        NotificationPopupPalette(
            tier1Background = Color(0xFFF2F2F7),
            tier2Surface = Color(0xFFFFFFFF),
            tier3Surface = Color(0xFFFFFFFF),
            primaryText = Color(0xFF000000),
            secondaryText = Color(0x993C3C43),
            divider = Color(0xFFD1D1D6),
            accent = Color(0xFF4285F4),
            unreadSurface = Color(0xFFF0F8FF),
            neutralIcon = Color(0x993C3C43),
            success = Color(0xFF34C759),
            warning = Color(0xFFFF9500),
            danger = Color(0xFFFF3B30),
            project = Color(0xFFAF52DE)
        )
    }
}
