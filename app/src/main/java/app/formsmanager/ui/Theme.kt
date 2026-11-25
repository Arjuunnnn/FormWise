package app.formsmanager.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.formsmanager.R
import app.formsmanager.editor.ui.FormsColors

/**
 * Manrope 500 / 700 / 800 as three static font files.
 *
 * These are instances cut from the variable font at exactly those weights. A single variable file
 * was tried first, but some devices ignore the weight setting and draw its default (ExtraLight, 200),
 * which made every heading hairline thin. Static files render the same everywhere.
 */
val Manrope = FontFamily(
    Font(R.font.manrope_medium, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_bold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

private fun TextStyle.manrope() = copy(fontFamily = Manrope)

private val base = Typography()
private val FormsTypography = Typography(
    displayLarge = base.displayLarge.manrope(),
    displayMedium = base.displayMedium.manrope(),
    displaySmall = base.displaySmall.manrope(),
    headlineLarge = base.headlineLarge.manrope(),
    headlineMedium = base.headlineMedium.manrope(),
    headlineSmall = base.headlineSmall.manrope(),
    titleLarge = base.titleLarge.manrope(),
    titleMedium = base.titleMedium.manrope(),
    titleSmall = base.titleSmall.manrope(),
    bodyLarge = base.bodyLarge.manrope(),
    bodyMedium = base.bodyMedium.manrope(),
    bodySmall = base.bodySmall.manrope(),
    labelLarge = base.labelLarge.manrope(),
    labelMedium = base.labelMedium.manrope(),
    labelSmall = base.labelSmall.manrope(),
)

/** How the app picks light or dark. */
enum class ThemeMode(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
}

@Composable
fun FormsTheme(mode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    // Written before any child reads a role token, so the first frame is already correct.
    if (FormsColors.isDark != dark) FormsColors.isDark = dark

    val scheme = if (dark) {
        darkColorScheme(
            primary = FormsColors.Cyan,
            onPrimary = FormsColors.OnCyan,
            secondary = FormsColors.Cyan,
            onSecondary = FormsColors.OnCyan,
            background = FormsColors.Canvas,
            onBackground = FormsColors.Ink,
            surface = FormsColors.Surface,
            onSurface = FormsColors.Ink,
            surfaceVariant = FormsColors.Track,
            onSurfaceVariant = FormsColors.InkMuted,
            outline = FormsColors.InkMuted,
            outlineVariant = FormsColors.Hairline,
            error = FormsColors.Error,
        )
    } else {
        lightColorScheme(
            primary = FormsColors.Cyan,
            onPrimary = FormsColors.OnCyan,
            secondary = FormsColors.Navy,
            onSecondary = FormsColors.Cyan,
            background = FormsColors.Canvas,
            onBackground = FormsColors.Ink,
            surface = FormsColors.Surface,
            onSurface = FormsColors.Ink,
            surfaceVariant = FormsColors.Track,
            onSurfaceVariant = FormsColors.InkMuted,
            outline = FormsColors.InkMuted,
            outlineVariant = FormsColors.Hairline,
            error = FormsColors.Error,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = FormsTypography, content = content)
}
