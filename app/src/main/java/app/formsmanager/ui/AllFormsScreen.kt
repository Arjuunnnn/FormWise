package app.formsmanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.FormData
import app.formsmanager.api.FormSummary
import app.formsmanager.editor.ui.FormsColors

class FormActions(
    val onOpenSheets: (String) -> Unit,
    val onCopyLink: (String) -> Unit,
    val onDuplicate: (String) -> Unit,
    val onSetAccepting: (String, Boolean) -> Unit,
    val onToggleStar: (String) -> Unit,
    val onTrash: (String) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllFormsScreen(
    forms: List<FormSummary>,
    data: Map<String, FormData>,
    onOpenForm: (String) -> Unit,
    onBack: () -> Unit,
    onNewForm: () -> Unit,
    actions: FormActions,
) {
    StatusBarStyle()

    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }

    val openCount = remember(forms, data) { forms.count { data[it.id]?.form?.isAcceptingResponses == true } }
    val closedCount = remember(forms, data) { forms.count { data[it.id]?.form?.isAcceptingResponses == false } }
    val starredCount = remember(forms) { forms.count { it.starred } }

    val visible = remember(forms, data, filter, query) {
        forms.filter { form ->
            val accepting = data[form.id]?.form?.isAcceptingResponses
            val matchesFilter = when (filter) {
                1 -> accepting == true
                2 -> accepting == false
                3 -> form.starred
                else -> true
            }
            matchesFilter && form.name.contains(query.trim(), ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas),
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(onBack = onBack, title = "All forms")
            Column(
                modifier = Modifier
                    .padding(horizontal = Gutter)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SearchField(query, { query = it }, "Search forms")
                SegmentedControl(
                    labels = listOf("All ${forms.size}", "Open $openCount", "Closed $closedCount", "Starred $starredCount"),
                    selected = filter,
                    onSelect = { filter = it },
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 4.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (visible.isEmpty()) {
                    item {
                        if (forms.isEmpty()) {
                            EmptyState(
                                icon = Icons.Outlined.Description,
                                title = "No forms yet",
                                body = "Forms you own or can edit will show up here.",
                                actionLabel = "Create a form",
                                onAction = onNewForm,
                            )
                        } else {
                            Text("No forms match.", color = FormsColors.InkMuted, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
                items(visible, key = { it.id }) { form ->
                    FormRow(
                        form = form,
                        data = data[form.id],
                        highlighted = form.id == sheetFor,
                        onClick = { onOpenForm(form.id) },
                        onMore = { sheetFor = form.id },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = Gutter, bottom = 20.dp)
                .height(56.dp)
                .clip(CircleShape)
                .background(FormsColors.Hero)
                .clickable(onClick = onNewForm)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = FormsColors.Cyan)
            Text("New form", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }

    sheetFor?.let { id ->
        val form = forms.firstOrNull { it.id == id }
        if (form != null) {
            QuickActionsSheet(form, data[id], actions, onDismiss = { sheetFor = null })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormRow(
    form: FormSummary,
    data: FormData?,
    highlighted: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FormsColors.Surface)
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) FormsColors.Cyan else FormsColors.Hairline,
                shape = shape,
            )
            .combinedClickable(onClick = onClick, onLongClick = onMore)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(Icons.Outlined.Description)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                form.name,
                color = FormsColors.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val edited = formatRelativeTime(form.modifiedTime)
            val sharedBy = if (!form.ownedByMe && form.ownerName.isNotBlank()) "Shared by ${form.ownerName} · " else ""
            Text(
                text = sharedBy + if (data != null) {
                    "${data.countLabel()} responses" + if (edited.isNotEmpty()) " · edited $edited" else ""
                } else {
                    if (edited.isNotEmpty()) "Edited $edited" else ""
                },
                color = FormsColors.InkMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (form.starred) {
            Icon(Icons.Filled.Star, contentDescription = "Starred", tint = FormsColors.CyanDeep, modifier = Modifier.size(18.dp))
        }
        if (data != null) {
            StatusChip(data.form.isAcceptingResponses, small = true)
        }
        IconAction(Icons.Outlined.MoreVert, "More actions for ${form.name}", onMore, tint = FormsColors.InkMuted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsSheet(
    form: FormSummary,
    data: FormData?,
    actions: FormActions,
    onDismiss: () -> Unit,
) {
    val accepting = data?.form?.isAcceptingResponses ?: true
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = FormsColors.Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = FormsColors.Ink.copy(alpha = 0.25f)) },
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = Gutter, end = Gutter, bottom = 26.dp),
        ) {
            Text(form.name, color = FormsColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
            Text(
                text = if (data != null) {
                    "${data.countLabel()} responses · ${if (accepting) "Open" else "Closed"}"
                } else {
                    "Edited ${formatRelativeTime(form.modifiedTime)}"
                },
                color = FormsColors.InkMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )
            SheetAction(Icons.Outlined.TableChart, "Open responses in Sheets") {
                onDismiss()
                actions.onOpenSheets(form.id)
            }
            SheetAction(Icons.Outlined.Link, "Copy link") {
                onDismiss()
                actions.onCopyLink(form.id)
            }
            SheetAction(Icons.Outlined.ContentCopy, "Duplicate form") {
                onDismiss()
                actions.onDuplicate(form.id)
            }
            SheetAction(
                icon = if (form.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                label = if (form.starred) "Remove star" else "Add star",
            ) {
                onDismiss()
                actions.onToggleStar(form.id)
            }
            SheetAction(
                icon = if (accepting) Icons.Outlined.Block else Icons.Outlined.PlayCircleOutline,
                label = if (accepting) "Stop accepting responses" else "Resume accepting responses",
            ) {
                onDismiss()
                actions.onSetAccepting(form.id, !accepting)
            }
            SheetAction(Icons.Outlined.DeleteOutline, "Move to trash", danger = true) {
                onDismiss()
                actions.onTrash(form.id)
            }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon, size = 40.dp)
        Text(label, color = if (danger) FormsColors.Error else FormsColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
