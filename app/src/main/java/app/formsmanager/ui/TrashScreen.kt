package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.api.FormSummary
import app.formsmanager.editor.ui.FormsColors

/** Forms moved to the Drive trash, with restore and delete-forever. [trash] is null while loading. */
@Composable
fun TrashScreen(
    trash: List<FormSummary>?,
    onBack: () -> Unit,
    onRestore: (String) -> Unit,
    onDeleteForever: (String) -> Unit,
) {
    StatusBarStyle()
    var confirming by remember { mutableStateOf<FormSummary?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas),
    ) {
        TopBar(onBack = onBack, title = "Trash")
        when {
            trash == null -> Box(Modifier.weight(1f)) { FullScreenLoading(FormsColors.Canvas, FormsColors.CyanDeep) }
            trash.isEmpty() -> Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Gutter),
            ) {
                EmptyState(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Trash is empty",
                    body = "Forms you move to the trash show up here so you can restore them.",
                )
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "Trashed forms stop accepting responses. Drive removes them automatically after about 30 days.",
                        color = FormsColors.InkMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(trash, key = { it.id }) { form ->
                    WhiteCard(padding = PaddingValues(14.dp)) {
                        Text(
                            form.name,
                            color = FormsColors.Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Last changed ${formatRelativeTime(form.modifiedTime)}",
                            color = FormsColors.InkMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            PrimaryButton("Restore", { onRestore(form.id) }, Modifier.weight(1f), height = 44.dp)
                            SecondaryButton(
                                "Delete forever",
                                { confirming = form },
                                Modifier.weight(1f),
                                height = 44.dp,
                                contentColor = FormsColors.Error,
                            )
                        }
                    }
                }
            }
        }
    }

    confirming?.let { form ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            containerColor = FormsColors.Surface,
            title = { Text("Delete forever?") },
            text = { Text("“${form.name}” and the responses stored in it will be deleted. A linked spreadsheet in Drive is not deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirming = null
                    onDeleteForever(form.id)
                }) { Text("Delete forever", color = FormsColors.Error, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text("Cancel", color = FormsColors.Ink) } },
        )
    }
}
