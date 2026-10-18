package com.interviewprep.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interviewprep.core.model.ThemeMode

/** Extra semantic colours that Material's scheme doesn't have. */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val streak: Color,
    val xp: Color,
    val codeBackground: Color,
    val codeText: Color,
    val codeBorder: Color,
    val outputBackground: Color,
    val outputText: Color,
)

private val LightExtended = ExtendedColors(
    success = Color(0xFF1B7F4B), onSuccess = Color.White, successContainer = Color(0xFFD5F2E1),
    warning = Color(0xFF9A6400), warningContainer = Color(0xFFFFE9C2),
    danger = Color(0xFFB3261E), dangerContainer = Color(0xFFFADAD6),
    streak = Color(0xFFE8590C), xp = Color(0xFF7048E8),
    codeBackground = Color(0xFFF4F5F8), codeText = Color(0xFF1F2430), codeBorder = Color(0xFFE1E4EA),
    outputBackground = Color(0xFF14181F), outputText = Color(0xFFD7E3D0),
)

private val DarkExtended = ExtendedColors(
    success = Color(0xFF5DD39E), onSuccess = Color(0xFF00391F), successContainer = Color(0xFF14432C),
    warning = Color(0xFFFFC66D), warningContainer = Color(0xFF4A3300),
    danger = Color(0xFFFFB4AB), dangerContainer = Color(0xFF5C1611),
    streak = Color(0xFFFF8A4C), xp = Color(0xFFB197FC),
    codeBackground = Color(0xFF1B1F27), codeText = Color(0xFFE3E7EF), codeBorder = Color(0xFF2B313C),
    outputBackground = Color(0xFF0E1116), outputText = Color(0xFFBFE3B4),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF3B4FD8), onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE3FF), onPrimaryContainer = Color(0xFF0A1673),
    secondary = Color(0xFF0E7C7B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEFEC), onSecondaryContainer = Color(0xFF00302F),
    tertiary = Color(0xFF8A4FBF), tertiaryContainer = Color(0xFFF1DBFF),
    background = Color(0xFFF8F9FC), onBackground = Color(0xFF181B23),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF181B23),
    surfaceVariant = Color(0xFFEEF0F5), onSurfaceVariant = Color(0xFF4A5060),
    surfaceContainerLow = Color(0xFFF4F5F9), surfaceContainer = Color(0xFFEFF1F6), surfaceContainerHigh = Color(0xFFE9EBF1),
    outline = Color(0xFF7A8091), outlineVariant = Color(0xFFD5D8E1),
    error = Color(0xFFB3261E),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFB9C3FF), onPrimary = Color(0xFF0F1D86),
    primaryContainer = Color(0xFF2A39B6), onPrimaryContainer = Color(0xFFDFE3FF),
    secondary = Color(0xFF7FD6D2), onSecondary = Color(0xFF003736),
    secondaryContainer = Color(0xFF0A4F4E), onSecondaryContainer = Color(0xFFCDEFEC),
    tertiary = Color(0xFFDDB8FF), tertiaryContainer = Color(0xFF5B2F86),
    background = Color(0xFF0F1116), onBackground = Color(0xFFE3E5EC),
    surface = Color(0xFF15181F), onSurface = Color(0xFFE3E5EC),
    surfaceVariant = Color(0xFF242833), onSurfaceVariant = Color(0xFFC1C6D3),
    surfaceContainerLow = Color(0xFF181B22), surfaceContainer = Color(0xFF1C2028), surfaceContainerHigh = Color(0xFF232733),
    outline = Color(0xFF8C92A2), outlineVariant = Color(0xFF3A3F4B),
    error = Color(0xFFFFB4AB),
)

val CodeFont = FontFamily.Monospace

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AppTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    CompositionLocalProvider(LocalExtendedColors provides if (dark) DarkExtended else LightExtended) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object AppColors {
    val extended: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
