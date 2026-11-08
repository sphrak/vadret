package se.vadretapp.vadret.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.vadretapp.vadret.setting.model.Theme

private val LocalVadretColors = staticCompositionLocalOf { lightVadretColors }
private val LocalVadretIsDark = staticCompositionLocalOf { false }
private val LocalVadretSpacing = staticCompositionLocalOf { VadretSpacing() }
private val LocalVadretTextStyles = staticCompositionLocalOf { VadretTextStyles() }

internal val lightAccent = Color(color = 0xFF3F51B5)
private val darkAccent = Color(color = 0xFFA1B3E6)
private val darkAccentContainer = Color(color = 0xFF1F2D52)
private val darkSurface = Color(color = 0xFF1C1E25)
private val lightText = Color(color = 0xFFE6E7EC)
private val amoledSurfaceContainer = Color(color = 0xFF141414)

private val vadretShapes = Shapes(
    medium = RoundedCornerShape(size = 2.dp),
    large = RoundedCornerShape(size = 20.dp)
)

private val baseTypography = Typography()

private val vadretTypography = Typography(
    displayLarge = baseTypography.displayLarge.withTabularNumbers(),
    displayMedium = baseTypography.displayMedium.withTabularNumbers(),
    displaySmall = baseTypography.displaySmall.withTabularNumbers(),
    headlineLarge = baseTypography.headlineLarge.withTabularNumbers(),
    headlineMedium = baseTypography.headlineMedium.withTabularNumbers(),
    headlineSmall = baseTypography.headlineSmall.withTabularNumbers(),
    titleLarge = baseTypography.titleLarge.withTabularNumbers(),
    titleMedium = baseTypography.titleMedium.copy(letterSpacing = 0.sp).withTabularNumbers(),
    titleSmall = baseTypography.titleSmall.withTabularNumbers(),
    bodyLarge = baseTypography.bodyLarge.copy(letterSpacing = 0.sp).withTabularNumbers(),
    bodyMedium = baseTypography.bodyMedium.copy(letterSpacing = 0.sp).withTabularNumbers(),
    bodySmall = baseTypography.bodySmall.copy(letterSpacing = 0.sp).withTabularNumbers(),
    labelLarge = baseTypography.labelLarge.copy(letterSpacing = 0.sp).withTabularNumbers(),
    labelMedium = baseTypography.labelMedium.withTabularNumbers(),
    labelSmall = baseTypography.labelSmall.withTabularNumbers()
)
private val mutedText = Color(color = 0xFF5A5E6B)
private val darkText = Color(color = 0xFF1B1C21)

private val lightScheme: ColorScheme = lightColorScheme(
    primary = lightAccent,
    onPrimary = Color.White,
    primaryContainer = lightAccent,
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(color = 0xFFE3E6F8),
    onSecondaryContainer = lightAccent,
    background = Color(color = 0xFFF3F4F8),
    onBackground = darkText,
    surface = Color.White,
    onSurface = darkText,
    onSurfaceVariant = mutedText,
    outline = Color(color = 0xFFD9DCE6),
    outlineVariant = Color(color = 0xFFE7E9F0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(color = 0xFFE3E6F8)
)

private val darkScheme: ColorScheme = darkColorScheme(
    primary = darkAccent,
    onPrimary = Color(color = 0xFF141833),
    primaryContainer = darkAccentContainer,
    onPrimaryContainer = Color(color = 0xFFE6E9F2),
    secondaryContainer = darkAccentContainer,
    onSecondaryContainer = darkAccent,
    background = Color(color = 0xFF111318),
    onBackground = lightText,
    surface = darkSurface,
    onSurface = lightText,
    onSurfaceVariant = Color(color = 0xFFA3A7B5),
    outline = Color(color = 0xFF353946),
    outlineVariant = Color(color = 0xFF2A2D37),
    surfaceContainerLowest = darkSurface,
    surfaceContainerLow = darkSurface,
    surfaceContainer = darkSurface,
    surfaceContainerHigh = darkSurface,
    surfaceContainerHighest = Color(color = 0xFF2A2E3A)
)

private val amoledScheme: ColorScheme = darkScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    outline = Color(color = 0xFF333333),
    outlineVariant = Color(color = 0xFF262626),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = amoledSurfaceContainer,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = amoledSurfaceContainer
)

// Screens are drawn straight on the background, outside any Surface, so the content colour is the background's text
// colour. Material windows such as the open search pick dark or light status bar icons from it.
@Composable
fun VadretTheme(
    theme: Theme,
    content: @Composable () -> Unit
) {
    val isSystemDark: Boolean = isSystemInDarkTheme()
    CompositionLocalProvider(
        LocalVadretColors provides theme.toVadretColors(isSystemDark = isSystemDark),
        LocalVadretIsDark provides theme.isDark(isSystemDark = isSystemDark),
        LocalVadretSpacing provides VadretSpacing(),
        LocalVadretTextStyles provides VadretTextStyles()
    ) {
        MaterialTheme(
            colorScheme = theme.toColorScheme(isSystemDark = isSystemDark),
            shapes = vadretShapes,
            typography = vadretTypography
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                content = content
            )
        }
    }
}

object VadretTheme {

    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes

    val colors: VadretColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVadretColors.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalVadretIsDark.current

    val spacing: VadretSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalVadretSpacing.current

    val textStyles: VadretTextStyles
        @Composable
        @ReadOnlyComposable
        get() = LocalVadretTextStyles.current
}

fun Theme.isDark(isSystemDark: Boolean): Boolean =
    when (this) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.AMOLED -> true
        Theme.SYSTEM -> isSystemDark
    }

// Following the system gives the plain dark look, since pure black is something a person picks on purpose.
private fun Theme.toColorScheme(isSystemDark: Boolean): ColorScheme =
    when (this) {
        Theme.LIGHT -> lightScheme
        Theme.DARK -> darkScheme
        Theme.AMOLED -> amoledScheme
        Theme.SYSTEM -> if (isSystemDark) {
            darkScheme
        } else {
            lightScheme
        }
    }

private fun Theme.toVadretColors(isSystemDark: Boolean): VadretColors =
    when (this) {
        Theme.LIGHT -> lightVadretColors
        Theme.DARK -> darkVadretColors
        Theme.AMOLED -> amoledVadretColors
        Theme.SYSTEM -> if (isSystemDark) {
            darkVadretColors
        } else {
            lightVadretColors
        }
    }