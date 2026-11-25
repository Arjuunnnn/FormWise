package app.formsmanager.editor.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Formwise design tokens (v3: light and dark).
 *
 * Tokens are named by ROLE, not by colour, so a screen never says "navy" when it means "text":
 *  - Brand constants never change: [Navy], [Cyan], [Ice], [OnCyan], [MutedOnDark].
 *  - Role tokens ([Canvas], [Surface], [Ink], [InkMuted], [Hero] ...) switch with the theme.
 *
 * [isDark] is Compose state, so anything that reads a role token recomposes when the theme flips.
 * It is written once, by FormsTheme.
 */
object FormsColors {
    var isDark by mutableStateOf(false)
        internal set

    private fun pick(light: Long, dark: Long) = Color(if (isDark) dark else light)

    // ---- Brand constants -------------------------------------------------------------------
    val Navy = Color(0xFF071A29)
    val NavyLight = Color(0xFF123A56)     // centre of the icon gradient, also the dark-theme hero card
    val Cyan = Color(0xFF36D2FF)
    val Ice = Color(0xFFD3EFFF)
    val Page = Color(0xFFEAF1F6)
    val MutedOnDark = Color(0xFFA9C9DC)   // secondary text on Hero / navy surfaces
    val OnCyan = Navy                     // text and icons that sit on a cyan fill
    val Scrim = Color(0x8C071A29)

    // Translucent ice used on navy surfaces
    val OnDarkSoft = Color(0x1FD3EFFF)
    val OnDarkRow = Color(0x17D3EFFF)
    val OnDarkField = Color(0x1AD3EFFF)

    // ---- Role tokens (switch with the theme) ----------------------------------------------
    /** Screen background. */
    val Canvas: Color get() = pick(0xFFF1F7FB, 0xFF06131D)
    /** Cards, bars, sheets, dialogs. */
    val Surface: Color get() = pick(0xFFFFFFFF, 0xFF0E2434)
    /** The thumb of a segmented control and other raised-on-track elements. */
    val Raised: Color get() = pick(0xFFFFFFFF, 0xFF1F4560)
    /** 1dp borders and dividers. */
    val Hairline: Color get() = pick(0xFFDCEAF2, 0xFF1F3B51)
    /** Soft wash behind icon tiles and selected rows. */
    val Tint: Color get() = pick(0xFFE6F6FD, 0xFF133349)
    /** Progress tracks, segmented-control background, avatar fill. */
    val Track: Color get() = pick(0xFFD9F0FD, 0xFF173B54)
    /** Primary text and icons. */
    val Ink: Color get() = pick(0xFF071A29, 0xFFF1F8FC)
    /** Secondary text and icons. */
    val InkMuted: Color get() = pick(0xFF3C566B, 0xFFA9C9DC)
    /** Cyan that stays readable: links, chart fills and accent icons on Surface. */
    val CyanDeep: Color get() = pick(0xFF0B7FA5, 0xFF36D2FF)
    /** Hero cards (today total, responses chart). */
    val Hero: Color get() = pick(0xFF071A29, 0xFF123A56)
    val Error: Color get() = pick(0xFFB3261E, 0xFFFF8A80)

    // ---- Aliases kept for older call sites --------------------------------------------------
    val Background: Color get() = Canvas
    val Outline: Color get() = Hairline
    val MutedOnLight: Color get() = InkMuted
}

/** Corner radii. */
object FormsShapes {
    val Card = RoundedCornerShape(20.dp)
    val Pill = RoundedCornerShape(percent = 50)
    val Chip = RoundedCornerShape(12.dp)
}
