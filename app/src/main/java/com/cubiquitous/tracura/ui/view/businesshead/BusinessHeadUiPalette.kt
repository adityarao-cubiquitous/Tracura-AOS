package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class BusinessHeadUiPalette(
    val tier1Background: Color,
    val tier2Surface: Color,
    val tier3Field: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val outline: Color,
    val accent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val scrim: Color,
    val userContainer: Color,
    val approverContainer: Color,
    val bhContainer: Color,
    val adminContainer: Color,
    val managerContainer: Color,
    val userText: Color,
    val approverText: Color,
    val bhText: Color,
    val adminText: Color,
    val managerText: Color
)

@Composable
fun businessHeadUiPalette(): BusinessHeadUiPalette {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        BusinessHeadUiPalette(
            tier1Background = Color(0xFF000000),
            tier2Surface = Color(0xFF1C1C1E),
            tier3Field = Color(0xFF2C2C2E),
            primaryText = Color(0xFFFFFFFF),
            secondaryText = Color(0x99EBEBF5),
            outline = Color(0xFF38383A),
            accent = Color(0xFF4285F4),
            success = Color(0xFF34C759),
            warning = Color(0xFFFF9F0A),
            danger = Color(0xFFFF453A),
            scrim = Color(0xB3000000),
            userContainer = Color(0xFF1C3A2C),
            approverContainer = Color(0xFF1E3557),
            bhContainer = Color(0xFF35264B),
            adminContainer = Color(0xFF4A2525),
            managerContainer = Color(0xFF1A3540),
            userText = Color(0xFF7EE2AA),
            approverText = Color(0xFF8AB4F8),
            bhText = Color(0xFFD7AEFB),
            adminText = Color(0xFFFF9B95),
            managerText = Color(0xFF4DD0E1)
        )
    } else {
        BusinessHeadUiPalette(
            tier1Background = Color(0xFFF2F2F7),
            tier2Surface = Color(0xFFFFFFFF),
            tier3Field = Color(0xFFFFFFFF),
            primaryText = Color(0xFF000000),
            secondaryText = Color(0x993C3C43),
            outline = Color(0xFFD1D1D6),
            accent = Color(0xFF4285F4),
            success = Color(0xFF34A853),
            warning = Color(0xFFFF9500),
            danger = Color(0xFFD32F2F),
            scrim = Color(0x4D000000),
            userContainer = Color(0xFFD4F4DD),
            approverContainer = Color(0xFFD6EBFF),
            bhContainer = Color(0xFFE1D4F4),
            adminContainer = Color(0xFFFFE0E0),
            managerContainer = Color(0xFFD4F0F4),
            userText = Color(0xFF34A853),
            approverText = Color(0xFF1976D2),
            bhText = Color(0xFF7B1FA2),
            adminText = Color(0xFFD32F2F),
            managerText = Color(0xFF0097A7)
        )
    }
}
