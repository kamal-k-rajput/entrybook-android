package com.greenworldlighting.entrybook.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val BrandGreen = Color(0xFF0E3B2C)
val BrandGold = Color(0xFFD7C08A)
val BrandPaper = Color(0xFFF3EFE6)
val InflowLight = Color(0xFF1B5E45)
val InflowDark = Color(0xFF8FCBB0)
val OutflowLight = Color(0xFF8D2F2F)
val OutflowDark = Color(0xFFFFB4AB)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E45),
    onPrimary = Color(0xFFF3EFE6),
    primaryContainer = Color(0xFFD7E6DE),
    onPrimaryContainer = Color(0xFF0E3B2C),
    secondary = Color(0xFFA67C32),
    onSecondary = Color(0xFFFFFDF8),
    secondaryContainer = Color(0xFFF3E4C4),
    onSecondaryContainer = Color(0xFF3E2C0E),
    background = Color(0xFFF3EFE6),
    onBackground = Color(0xFF1A2118),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF1A2118),
    surfaceVariant = Color(0xFFE7E0D4),
    onSurfaceVariant = Color(0xFF5D675C),
    outline = Color(0xFFD9D0C2),
    error = Color(0xFF8D2F2F),
    onError = Color(0xFFFFFDF8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FCBB0),
    onPrimary = Color(0xFF0E3B2C),
    primaryContainer = Color(0xFF1B5E45),
    onPrimaryContainer = Color(0xFFD7E6DE),
    secondary = Color(0xFFE0C48A),
    onSecondary = Color(0xFF3E2C0E),
    background = Color(0xFF121814),
    onBackground = Color(0xFFF3EFE6),
    surface = Color(0xFF1C2620),
    onSurface = Color(0xFFF3EFE6),
    surfaceVariant = Color(0xFF2A3630),
    onSurfaceVariant = Color(0xFFC9D2CB),
    outline = Color(0xFF3E4A43),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun inflowColor(): Color = if (isSystemInDarkTheme()) InflowDark else InflowLight

@Composable
fun outflowColor(): Color = if (isSystemInDarkTheme()) OutflowDark else OutflowLight

@Composable
fun EntrybookTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
