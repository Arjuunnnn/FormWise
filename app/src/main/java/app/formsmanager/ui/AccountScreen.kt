package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.api.UserInfo
import app.formsmanager.editor.ui.FormsColors

@Composable
fun AccountScreen(
    user: UserInfo?,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    onTrash: () -> Unit,
    onHelp: () -> Unit,
    onPrivacy: () -> Unit,
    onSignOut: () -> Unit,
) {
    StatusBarStyle()
    var confirmSignOut by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LargeTitle("Account")

        WhiteCard(
            modifier = Modifier.padding(horizontal = Gutter),
            padding = PaddingValues(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Avatar(user?.name?.ifBlank { null } ?: user?.email ?: "?", size = 64.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        user?.name.orEmpty().ifBlank { "Signed in" },
                        color = FormsColors.Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        user?.email.orEmpty(),
                        color = FormsColors.InkMuted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        WhiteCard(
            modifier = Modifier.padding(horizontal = Gutter),
            padding = PaddingValues(16.dp),
        ) {
            Text("Appearance", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
            Text(
                "System follows your phone's light or dark setting.",
                color = FormsColors.InkMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            SegmentedControl(
                labels = ThemeMode.entries.map { it.label },
                selected = themeMode.ordinal,
                onSelect = { onThemeMode(ThemeMode.entries[it]) },
            )
        }

        WhiteCard(
            modifier = Modifier.padding(horizontal = Gutter),
            padding = PaddingValues(0.dp),
        ) {
            LinkRow(Icons.Outlined.DeleteOutline, "Trash", onTrash, "Restore forms you moved to the trash")
            HorizontalDivider(color = FormsColors.Hairline)
            LinkRow(Icons.Outlined.Shield, "Privacy & permissions", onPrivacy, "What Formwise can access")
            HorizontalDivider(color = FormsColors.Hairline)
            LinkRow(Icons.AutoMirrored.Outlined.HelpOutline, "Help & about", onHelp, "Questions, feedback and version")
        }

        SecondaryButton(
            text = "Sign out",
            onClick = { confirmSignOut = true },
            modifier = Modifier
                .padding(horizontal = Gutter)
                .fillMaxWidth(),
            contentColor = FormsColors.Error,
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            containerColor = FormsColors.Surface,
            title = { Text("Sign out of Formwise?") },
            text = {
                Text("You'll return to the sign-in screen. Formwise stays authorised in your Google Account until you remove it there.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) {
                    Text("Sign out", color = FormsColors.Error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) {
                    Text("Cancel", color = FormsColors.Ink)
                }
            },
        )
    }
}
