package com.cubiquitous.tracura.ui.view.auth

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AuthUiPalette(
    val tier1Background: Color,
    val tier2Surface: Color,
    val tier3Field: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val outline: Color,
    val accent: Color,
    val accentSecondary: Color,
    val accentTertiary: Color,
    val danger: Color,
    val disabled: Color
)

@Composable
fun authUiPalette(): AuthUiPalette {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        AuthUiPalette(
            tier1Background = Color(0xFF000000),
            tier2Surface = Color(0xFF1C1C1E),
            tier3Field = Color(0xFF2C2C2E),
            primaryText = Color(0xFFFFFFFF),
            secondaryText = Color(0x99EBEBF5),
            outline = Color(0xFF38383A),
            accent = Color(0xFF3B82F6),
            accentSecondary = Color(0xFF8B5CF6),
            accentTertiary = Color(0xFF10B981),
            danger = Color(0xFFFF453A),
            disabled = Color(0xFF636366)
        )
    } else {
        AuthUiPalette(
            tier1Background = Color(0xFFF2F2F7),
            tier2Surface = Color(0xFFFFFFFF),
            tier3Field = Color(0xFFFFFFFF),
            primaryText = Color(0xFF000000),
            secondaryText = Color(0x993C3C43),
            outline = Color(0xFFD1D1D6),
            accent = Color(0xFF3B5BDB),
            accentSecondary = Color(0xFF7950F2),
            accentTertiary = Color(0xFF12B886),
            danger = Color(0xFFC62828),
            disabled = Color(0xFFBDBDBD)
        )
    }
}
