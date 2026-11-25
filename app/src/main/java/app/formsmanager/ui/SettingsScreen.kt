package app.formsmanager.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.FormData
import app.formsmanager.api.Collaborator
import app.formsmanager.api.FormSummary
import app.formsmanager.editor.ui.FormsColors

class SettingsActions(
    val onBack: () -> Unit,
    val onRename: (String) -> Unit,
    val onTrash: () -> Unit,
    val onQuiz: (Boolean) -> Unit,
    val onEmailCollection: (String) -> Unit,
    val onCopyLink: (String) -> Unit,
    val onSave: (Boolean) -> Unit,
    val onOpenInGoogle: () -> Unit,
    val onShortLink: () -> Unit,
    val onInvite: (String) -> Unit,
    val onRemove: (Collaborator) -> Unit,
)

@Composable
fun SettingsScreen(
    summary: FormSummary?,
    data: FormData?,
    link: String,
    shortLink: String?,
    collaborators: List<Collaborator>?,
    actions: SettingsActions,
) {
    StatusBarStyle()
    val saved = data?.form?.isAcceptingResponses ?: true
    var accepting by rememberSaveable(saved) { mutableStateOf(saved) }
    var showQr by remember { mutableStateOf(false) }
    var inviting by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Collaborator?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var confirmTrash by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas),
    ) {
        TopBar(onBack = actions.onBack, title = "Share and settings") {
            StatusChip(saved, Modifier.padding(end = 12.dp))
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { GroupLabel("Share") }
            item {
                WhiteCard(padding = PaddingValues(16.dp)) {
                    Text("Form link", color = FormsColors.InkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 14.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FormsColors.Canvas)
                            .padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            link.removePrefix("https://"),
                            color = FormsColors.Ink,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconAction(Icons.Outlined.ContentCopy, "Copy link", { actions.onCopyLink(link) }, tint = FormsColors.CyanDeep)
                    }
                    if (shortLink != null) {
                        Text("Short link", color = FormsColors.InkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 14.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(FormsColors.Canvas)
                                .padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                shortLink.removePrefix("https://"),
                                color = FormsColors.Ink,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconAction(Icons.Outlined.ContentCopy, "Copy short link", actions.onShortLink, tint = FormsColors.CyanDeep)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton("Copy link", { actions.onCopyLink(link) }, Modifier.weight(1f), height = 46.dp)
                        SecondaryButton(
                            "Copy short link",
                            actions.onShortLink,
                            Modifier.weight(1f),
                            height = 46.dp,
                        )
                    }
                    SecondaryButton(
                        "Show QR code",
                        { showQr = true },
                        Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth(),
                        height = 46.dp,
                    )
                    Text(
                        "Short links are made by is.gd. Only this form's public link is sent to it, and only when you tap Copy short link.",
                        color = FormsColors.InkMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            item { GroupLabel("Responses", Modifier.padding(top = 10.dp)) }
            item {
                WhiteCard(padding = PaddingValues(0.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Accepting responses", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (accepting) "People can submit now" else "The form is closed to new responses",
                                color = FormsColors.InkMuted,
                                fontSize = 13.sp,
                            )
                        }
                        Switch(
                            checked = accepting,
                            onCheckedChange = { accepting = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = FormsColors.Cyan,
                                checkedThumbColor = FormsColors.OnCyan,
                                checkedBorderColor = FormsColors.Cyan,
                                uncheckedTrackColor = Color.Transparent,
                                uncheckedThumbColor = FormsColors.Ink,
                                uncheckedBorderColor = FormsColors.Ink,
                            ),
                        )
                    }
                    HorizontalDivider(color = FormsColors.Hairline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("More settings", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "Close date, email alerts and one response per person are set in Google Forms",
                                color = FormsColors.InkMuted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            )
                        }
                        IconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in Google Forms", actions.onOpenInGoogle, tint = FormsColors.CyanDeep)
                    }
                }
            }

            if (data != null) {
                item { GroupLabel("Form options", Modifier.padding(top = 10.dp)) }
                item {
                    WhiteCard(padding = PaddingValues(0.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Make this a quiz", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "Grade answers and show scores. Set points and correct answers in the editor.",
                                    color = FormsColors.InkMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                            Switch(
                                checked = data.form.isQuiz,
                                onCheckedChange = actions.onQuiz,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = FormsColors.Cyan,
                                    checkedThumbColor = FormsColors.OnCyan,
                                    checkedBorderColor = FormsColors.Cyan,
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedThumbColor = FormsColors.Ink,
                                    uncheckedBorderColor = FormsColors.Ink,
                                ),
                            )
                        }
                        HorizontalDivider(color = FormsColors.Hairline)
                        Column(Modifier.padding(16.dp)) {
                            Text("Collect email addresses", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "Verified uses the respondent's Google account. Ask lets them type one.",
                                color = FormsColors.InkMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                            )
                            val types = listOf("DO_NOT_COLLECT", "VERIFIED", "RESPONDER_INPUT")
                            SegmentedControl(
                                labels = listOf("Off", "Verified", "Ask"),
                                selected = types.indexOf(data.form.emailCollection).coerceAtLeast(0),
                                onSelect = { actions.onEmailCollection(types[it]) },
                            )
                        }
                    }
                }
            }

            if (summary != null) {
                item { GroupLabel("Details", Modifier.padding(top = 10.dp)) }
                item {
                    WhiteCard(padding = PaddingValues(0.dp)) {
                        DetailRow("Name in Drive", summary.name)
                        HorizontalDivider(color = FormsColors.Hairline)
                        DetailRow("Owner", if (summary.ownedByMe) "You" else summary.ownerName.ifBlank { "Someone else" })
                        if (summary.createdTime.isNotBlank()) {
                            HorizontalDivider(color = FormsColors.Hairline)
                            DetailRow("Created", formatDateTime(summary.createdTime))
                        }
                        HorizontalDivider(color = FormsColors.Hairline)
                        DetailRow("Last changed", formatDateTime(summary.modifiedTime))
                        HorizontalDivider(color = FormsColors.Hairline)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            SecondaryButton("Rename", { renaming = true }, Modifier.weight(1f), height = 44.dp)
                            SecondaryButton(
                                "Move to trash",
                                { confirmTrash = true },
                                Modifier.weight(1f),
                                height = 44.dp,
                                contentColor = FormsColors.Error,
                            )
                        }
                    }
                }
            }

            if (collaborators != null) {
                item { GroupLabel("People", Modifier.padding(top = 10.dp)) }
                item {
                    WhiteCard(padding = PaddingValues(0.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (collaborators.size == 1) "1 collaborator" else "${collaborators.size} collaborators",
                                    color = FormsColors.Ink,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text("Can view and edit responses", color = FormsColors.InkMuted, fontSize = 13.sp)
                            }
                            SecondaryButton("Invite", { inviting = true }, height = 40.dp)
                        }
                        collaborators.forEach { person ->
                            HorizontalDivider(color = FormsColors.Hairline)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Avatar(person.name, size = 38.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(person.name, color = FormsColors.Ink, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(
                                        "${if (person.isOwner) "Owner" else "Editor"} \u00b7 ${person.email}",
                                        color = FormsColors.InkMuted,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (!person.isOwner) {
                                    TextButton(onClick = { removing = person }) {
                                        Text("Remove", color = FormsColors.Error, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        BottomActionBar {
            PrimaryButton(
                text = "Save changes",
                onClick = { actions.onSave(accepting) },
                modifier = Modifier.weight(1f),
                enabled = data != null && accepting != saved,
            )
        }
    }

    if (renaming && summary != null) {
        var name by remember { mutableStateOf(summary.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            containerColor = FormsColors.Surface,
            title = { Text("Rename form") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text(
                        "This is the name in Google Drive. The title people see on the form is changed in the editor.",
                        color = FormsColors.InkMuted,
                        fontSize = 13.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank() && name.trim() != summary.name,
                    onClick = {
                        renaming = false
                        actions.onRename(name.trim())
                    },
                ) { Text("Rename", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel", color = FormsColors.Ink) } },
        )
    }

    if (confirmTrash && summary != null) {
        AlertDialog(
            onDismissRequest = { confirmTrash = false },
            containerColor = FormsColors.Surface,
            title = { Text("Move to trash?") },
            text = { Text("“${summary.name}” stops accepting responses. You can restore it from Trash in Account.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmTrash = false
                    actions.onTrash()
                }) { Text("Move to trash", color = FormsColors.Error, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { confirmTrash = false }) { Text("Cancel", color = FormsColors.Ink) } },
        )
    }

    if (showQr) {
        val qr = remember(link) { qrBitmap(link) }
        AlertDialog(
            onDismissRequest = { showQr = false },
            title = { Text("Scan to open the form") },
            text = {
                Image(
                    bitmap = qr,
                    contentDescription = "QR code for the form link",
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(260.dp),
                )
            },
            confirmButton = { TextButton(onClick = { showQr = false }) { Text("Close") } },
        )
    }

    if (inviting) {
        var email by rememberSaveable { mutableStateOf("") }
        val valid = email.trim().let { it.contains('@') && it.substringAfter('@').contains('.') }
        AlertDialog(
            onDismissRequest = { inviting = false },
            title = { Text("Invite an editor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    Text(
                        "They get an email and can edit the form and see its responses.",
                        color = FormsColors.InkMuted,
                        fontSize = 13.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        actions.onInvite(email.trim())
                        inviting = false
                    },
                ) {
                    Text("Send invite")
                }
            },
            dismissButton = { TextButton(onClick = { inviting = false }) { Text("Cancel") } },
        )
    }

    removing?.let { person ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${person.name}?") },
            text = { Text("They will no longer be able to open or edit this form.") },
            confirmButton = {
                TextButton(onClick = {
                    actions.onRemove(person)
                    removing = null
                }) {
                    Text("Remove")
                }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = FormsColors.InkMuted,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier.padding(start = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, color = FormsColors.InkMuted, fontSize = 14.sp)
        Text(
            value,
            color = FormsColors.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
