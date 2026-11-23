package com.qionevale.seagullthinking.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.qionevale.seagullthinking.data.AppSettings
import com.qionevale.seagullthinking.data.DarkPref
import com.qionevale.seagullthinking.data.FontPref

val Ink = Color(0xFF101828)
val Muted = Color(0xFF667085)
val PageBg = Color(0xFFF4F6FA)
val CardBg = Color(0xFFFFFFFF)
val DarkPageBg = Color(0xFF0C1016)
val DarkCardBg = Color(0xFF161C24)

fun accentColor(settings: AppSettings): Color = Color(settings.accent.argb)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = blend(accent, Color.White, 0.86f),
    onPrimaryContainer = blend(accent, Color.Black, 0.35f),
    secondary = blend(accent, Color.Black, 0.18f),
    onSecondary = Color.White,
    background = PageBg,
    onBackground = Ink,
    surface = CardBg,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDEFF4),
    onSurfaceVariant = Muted,
    outline = Color(0xFFD7DBE3),
    error = Color(0xFFC0392B)
)

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = blend(accent, Color.White, 0.35f),
    onPrimary = Color(0xFF07101C),
    primaryContainer = blend(accent, Color.Black, 0.55f),
    onPrimaryContainer = blend(accent, Color.White, 0.8f),
    secondary = blend(accent, Color.White, 0.5f),
    onSecondary = Color(0xFF07101C),
    background = DarkPageBg,
    onBackground = Color(0xFFE6EDF3),
    surface = DarkCardBg,
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF222A35),
    onSurfaceVariant = Color(0xFF97A2B0),
    outline = Color(0xFF2E3844),
    error = Color(0xFFE07A6B)
)

fun blend(base: Color, other: Color, t: Float): Color = Color(
    red = base.red + (other.red - base.red) * t,
    green = base.green + (other.green - base.green) * t,
    blue = base.blue + (other.blue - base.blue) * t,
    alpha = 1f
)

private fun fontFamily(pref: FontPref): FontFamily = when (pref) {
    FontPref.SYSTEM -> FontFamily.Default
    FontPref.SERIF -> FontFamily.Serif
    FontPref.MONO -> FontFamily.Monospace
}

private fun typography(family: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.fit() = copy(fontFamily = family)
    return Typography(
        displayLarge = base.displayLarge.fit(),
        displayMedium = base.displayMedium.fit(),
        displaySmall = base.displaySmall.fit(),
        headlineLarge = base.headlineLarge.fit(),
        headlineMedium = base.headlineMedium.fit(),
        headlineSmall = base.headlineSmall.fit(),
        titleLarge = base.titleLarge.fit(),
        titleMedium = base.titleMedium.fit(),
        titleSmall = base.titleSmall.fit(),
        bodyLarge = base.bodyLarge.fit(),
        bodyMedium = base.bodyMedium.fit(),
        bodySmall = base.bodySmall.fit(),
        labelLarge = base.labelLarge.fit(),
        labelMedium = base.labelMedium.fit(),
        labelSmall = base.labelSmall.fit()
    )
}

@Composable
fun SeagullTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.dark) {
        DarkPref.LIGHT -> false
        DarkPref.DARK -> true
        DarkPref.SYSTEM -> systemDark
    }
    val accent = accentColor(settings)
    val base = if (dark) darkScheme(accent) else lightScheme(accent)
    // 设了背景图就把卡片做成半透明玻璃，让照片透出来，同时压住文字底色
    val scheme = if (settings.backgroundPath == null) base else base.copy(
        surface = base.surface.copy(alpha = if (dark) 0.80f else 0.84f),
        surfaceVariant = base.surfaceVariant.copy(alpha = 0.68f)
    )
    val family = fontFamily(settings.font)
    val density = LocalDensity.current
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * settings.fontScale)
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography(family),
            content = content
        )
    }
}
