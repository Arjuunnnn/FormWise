package app.formsmanager.ui

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import app.formsmanager.R
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.formsmanager.editor.ui.FormsColors
import app.formsmanager.editor.ui.PlainField

/** Horizontal screen margin used by every screen. */
val Gutter = 20.dp

/**
 * The Formwise mark: the launcher icon artwork (stacked cards with a bar chart) on a rounded navy tile.
 * Pass [outlined] on a navy background so the tile edge stays visible.
 */
@Composable
fun FormwiseMark(
    size: Dp,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(FormsColors.NavyLight, FormsColors.Navy),
                        center = center,
                        radius = this.size.minDimension * 0.70f,
                    ),
                )
            }
            .then(if (outlined) Modifier.border(1.dp, FormsColors.OnDarkSoft, shape) else Modifier),
    ) {
        // The artwork only fills the middle of its 108 unit canvas, so scale it up inside the tile.
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.3f),
        )
    }
}

/** Keeps system bar icons readable. Defaults to the current theme; pass false for always-dark screens. */
@Composable
fun StatusBarStyle(darkIcons: Boolean = !FormsColors.isDark) {
    val view = LocalView.current
    SideEffect {
        var context = view.context
        while (context is ContextWrapper && context !is Activity) context = context.baseContext
        (context as? Activity)?.window?.let {
            val controller = WindowCompat.getInsetsController(it, view)
            controller.isAppearanceLightStatusBars = darkIcons
            controller.isAppearanceLightNavigationBars = darkIcons
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Chips and buttons
// ---------------------------------------------------------------------------------------------

@Composable
fun Chip(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    small: Boolean = false,
) {
    Text(
        text = text,
        color = content,
        fontSize = if (small) 12.sp else 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = if (small) 10.dp else 12.dp, vertical = if (small) 3.dp else 5.dp),
    )
}

/** "Open" / "Closed" with a status dot. Reads on any light surface. */
@Composable
fun StatusChip(
    accepting: Boolean,
    modifier: Modifier = Modifier,
    small: Boolean = false,
) {
    val container = if (accepting) FormsColors.Cyan.copy(alpha = 0.22f) else FormsColors.Hairline
    val dot = if (accepting) FormsColors.CyanDeep else FormsColors.InkMuted.copy(alpha = 0.55f)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = if (small) 9.dp else 11.dp, vertical = if (small) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(
            text = if (accepting) "Open" else "Closed",
            color = FormsColors.Ink,
            fontSize = if (small) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = RoundedCornerShape(height / 2),
        colors = ButtonDefaults.buttonColors(
            containerColor = FormsColors.Cyan,
            contentColor = FormsColors.OnCyan,
            disabledContainerColor = FormsColors.Hairline,
            disabledContentColor = FormsColors.InkMuted.copy(alpha = 0.6f),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = FormsColors.Surface,
    height: Dp = 52.dp,
    contentColor: Color = FormsColors.Ink,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(height / 2),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = contentColor),
        border = BorderStroke(1.dp, FormsColors.Hairline),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

/** Flat icon button for top bars. No background, 44dp touch target. */
@Composable
fun IconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = FormsColors.Ink,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

/** Round icon button with a white fill and hairline ring. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = FormsColors.Surface,
    tint: Color = FormsColors.Ink,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, FormsColors.Hairline, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

// ---------------------------------------------------------------------------------------------
// Navigation chrome
// ---------------------------------------------------------------------------------------------

/** Compact top bar for pushed screens: back arrow, optional title, trailing actions. */
@Composable
fun TopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String = "",
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconAction(Icons.AutoMirrored.Outlined.ArrowBack, "Go back", onBack)
        } else {
            Spacer(Modifier.width(10.dp))
        }
        if (title.isBlank()) {
            Spacer(Modifier.weight(1f))
        } else {
            Text(
                text = title,
                color = FormsColors.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
            )
        }
        actions()
    }
}

/** Big page title used at the top of the tab screens. */
@Composable
fun LargeTitle(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(eyebrow, color = FormsColors.InkMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Text(title, color = FormsColors.Ink, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold)
        }
        trailing()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = FormsColors.Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                color = FormsColors.CyanDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

/** Tinted track with a white thumb. Fills the width, every segment the same size. */
@Composable
fun SegmentedControl(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FormsColors.Track)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) FormsColors.Raised else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (on) FormsColors.Ink else FormsColors.InkMuted,
                    fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Full-width text tabs with a cyan indicator that sits on a hairline. */
@Composable
fun UnderlineTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                val on = index == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        color = if (on) FormsColors.Ink else FormsColors.InkMuted,
                        fontSize = 15.sp,
                        fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                if (on) FormsColors.Cyan else Color.Transparent,
                                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                            ),
                    )
                }
            }
        }
        HorizontalDivider(color = FormsColors.Hairline)
    }
}

enum class NavItem(val icon: ImageVector, val label: String) {
    Forms(Icons.Outlined.Today, "Today"),
    Responses(Icons.Outlined.BarChart, "Responses"),
    Templates(Icons.Outlined.Add, "New"),
    Account(Icons.Outlined.Person, "Account"),
}

/** Labelled bottom navigation. "New" is an action, so it is drawn as a filled navy button. */
@Composable
fun BottomNav(
    selected: NavItem?,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(FormsColors.Surface)) {
        HorizontalDivider(color = FormsColors.Hairline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 8.dp),
        ) {
            NavItem.entries.forEach { item ->
                val on = item == selected
                val isAction = item == NavItem.Templates
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(item) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(if (isAction) 48.dp else 56.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isAction -> FormsColors.Hero
                                    on -> FormsColors.Cyan
                                    else -> Color.Transparent
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = when {
                                isAction -> FormsColors.Cyan
                                on -> FormsColors.OnCyan
                                else -> FormsColors.Ink
                            },
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = item.label,
                        color = if (on) FormsColors.Ink else FormsColors.InkMuted,
                        fontSize = 12.sp,
                        fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/** Sticky action area at the bottom of a pushed screen. Place it last in the screen's Column. */
@Composable
fun BottomActionBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(FormsColors.Surface),
    ) {
        HorizontalDivider(color = FormsColors.Hairline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Gutter, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Surfaces and content pieces
// ---------------------------------------------------------------------------------------------

@Composable
fun WhiteCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FormsColors.Surface)
            .border(1.dp, FormsColors.Hairline, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Tappable settings-style row: icon tile, title, optional subtitle, chevron. Put it inside a WhiteCard with zero padding. */
@Composable
fun LinkRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
            if (subtitle != null) {
                Text(subtitle, color = FormsColors.InkMuted, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = FormsColors.InkMuted)
    }
}

/** Small number-over-label tile. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(FormsColors.Surface)
            .border(1.dp, FormsColors.Hairline, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(value, color = FormsColors.Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(label, color = FormsColors.InkMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Rounded tile that holds an icon. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 44.dp, container: Color = FormsColors.Tint) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = FormsColors.CyanDeep)
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(FormsColors.Surface)
            .border(1.dp, FormsColors.Hairline, shape)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = FormsColors.InkMuted)
        Box(Modifier.weight(1f)) {
            PlainField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                textStyle = TextStyle(fontSize = 16.sp, color = FormsColors.Ink),
            )
        }
        if (value.isNotEmpty()) {
            IconAction(Icons.Outlined.Close, "Clear search", { onValueChange("") }, tint = FormsColors.InkMuted)
        }
    }
}

@Composable
fun Avatar(letter: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(FormsColors.Track),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter.take(1).uppercase(),
            color = FormsColors.Ink,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.4f).sp,
        )
    }
}

/** Bars scaled to the largest value. With [emphasizeLast] the final bar is full strength and the rest are softened. */
@Composable
fun MiniBars(
    values: List<Int>,
    color: Color,
    modifier: Modifier = Modifier,
    gap: Dp = 4.dp,
    emphasizeLast: Boolean = false,
) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(gap)) {
        values.forEachIndexed { index, value ->
            val soft = emphasizeLast && index != values.lastIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight((value.toFloat() / max).coerceIn(0.08f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (soft) color.copy(alpha = 0.45f) else color),
            )
        }
    }
}

@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    track: Color = FormsColors.Track,
    fill: Color = FormsColors.CyanDeep,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(fill),
        )
    }
}

@Composable
fun FullScreenLoading(background: Color = FormsColors.Navy, tint: Color = FormsColors.Cyan) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = tint)
    }
}

@Composable
fun BusyOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Scrim)
            .clickable(enabled = true, indication = null, interactionSource = null) {},
        contentAlignment = Alignment.Center,
    ) {
        WhiteCard(
            modifier = Modifier.padding(horizontal = 48.dp),
            padding = PaddingValues(24.dp),
        ) {
            CircularProgressIndicator(color = FormsColors.CyanDeep, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(
                message,
                color = FormsColors.Ink,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp),
            )
        }
    }
}
