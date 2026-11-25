package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.editor.ui.FormsColors

/** Plain-language list of the Google permissions the app asks for and what each one is used for. */
@Composable
fun PrivacyScreen(onBack: () -> Unit, onManageAccess: () -> Unit) {
    StatusBarStyle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas),
    ) {
        TopBar(onBack = onBack, title = "Privacy & permissions")
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Formwise asks Google for three permissions. Here is what each one is for.",
                    color = FormsColors.InkMuted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            }
            item {
                WhiteCard(padding = PaddingValues(0.dp)) {
                    PermissionRow(
                        Icons.Outlined.EditNote,
                        "Google Forms: view and edit",
                        "Read and change the title, questions and settings of your forms, and create new ones.",
                    )
                    HorizontalDivider(color = FormsColors.Hairline)
                    PermissionRow(
                        Icons.Outlined.TableChart,
                        "Form responses: view only",
                        "Read the answers people submit. This permission is read-only.",
                    )
                    HorizontalDivider(color = FormsColors.Hairline)
                    PermissionRow(
                        Icons.Outlined.Cloud,
                        "Google Drive",
                        "Used to list your Google Forms, see who can edit each one, and invite or remove editors. " +
                            "Google words this permission broadly, as access to your Drive files.",
                    )
                }
            }
            item { Heading("Where your data goes") }
            item {
                WhiteCard(padding = PaddingValues(0.dp)) {
                    PermissionRow(
                        Icons.Outlined.PhoneAndroid,
                        "Straight to Google",
                        "Formwise has no backend, no analytics and no ads. Requests go from your phone to Google's APIs. The one exception is Copy short link: when you tap it, the form's public link is sent to is.gd to make a short version.",
                    )
                    HorizontalDivider(color = FormsColors.Hairline)
                    PermissionRow(
                        Icons.Outlined.Lock,
                        "Nothing stored on the app side",
                        "Access tokens are requested from Google when needed rather than saved, and form data is held in memory while the app is open.",
                    )
                }
            }
            item { Heading("Your control") }
            item {
                WhiteCard {
                    Text(
                        "Signing out returns you to the sign-in screen. To remove Formwise's access completely, revoke it in your Google Account.",
                        color = FormsColors.InkMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                    SecondaryButton(
                        "Manage access in Google",
                        onManageAccess,
                        Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        color = FormsColors.Ink,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconTile(icon, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
            Text(body, color = FormsColors.InkMuted, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
