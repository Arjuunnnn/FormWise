package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.editor.ui.FormsColors

/**
 * Full-screen message with one main action. Used when something can't be shown:
 * no connection, a form that no longer exists, a response that didn't load.
 */
@Composable
fun MessageScreen(
    icon: ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    StatusBarStyle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas),
    ) {
        if (onBack != null) TopBar(onBack = onBack) else Spacer(Modifier.statusBarsPadding())
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(FormsColors.Tint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = FormsColors.CyanDeep, modifier = Modifier.size(40.dp))
            }
            Text(
                title,
                color = FormsColors.Ink,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                body,
                color = FormsColors.InkMuted,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
            )
            PrimaryButton(primaryLabel, onPrimary, Modifier.fillMaxWidth())
            if (secondaryLabel != null && onSecondary != null) {
                SecondaryButton(
                    secondaryLabel,
                    onSecondary,
                    Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

/** A message that sits inside a list instead of taking over the screen. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    WhiteCard(modifier = modifier, padding = PaddingValues(24.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconTile(icon, size = 56.dp)
            Text(
                title,
                color = FormsColors.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                body,
                color = FormsColors.InkMuted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (actionLabel != null && onAction != null) {
                PrimaryButton(actionLabel, onAction, Modifier.padding(top = 20.dp), height = 48.dp)
            }
        }
    }
}

/** Branded splash shown while the app checks for an existing Google session. */
@Composable
fun BrandedLoading() {
    StatusBarStyle(darkIcons = false)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Navy),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormwiseMark(size = 56.dp, outlined = true)
            Text("Formwise", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }
        CircularProgressIndicator(
            color = FormsColors.Cyan,
            strokeWidth = 3.dp,
            modifier = Modifier
                .padding(top = 32.dp)
                .size(28.dp),
        )
    }
}
