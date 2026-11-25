package app.formsmanager.editor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.formsmanager.ui.StatusBarStyle
import app.formsmanager.ui.UnderlineTabs
import app.formsmanager.SaveProblem
import app.formsmanager.api.LoadedForm
import app.formsmanager.api.ResponsesState
import app.formsmanager.editor.DeleteQuestion
import app.formsmanager.editor.EditorViewModel
import app.formsmanager.editor.FormDraft
import app.formsmanager.editor.MoveQuestion
import app.formsmanager.editor.QuestionType
import app.formsmanager.editor.SetQuestionType
import app.formsmanager.editor.UpdateFormDescription
import app.formsmanager.editor.UpdateFormTitle
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

class EditorActions(
    val onBack: () -> Unit,
    val onSave: (FormDraft) -> Unit,
    val onDismissProblem: () -> Unit,
    val onReload: () -> Unit,
    val onLoadResponses: () -> Unit,
)

@Composable
fun EditorScreen(
    openId: Int,
    form: LoadedForm,
    saving: Boolean,
    saveProblem: SaveProblem?,
    responses: ResponsesState,
    actions: EditorActions,
) {
    val vm: EditorViewModel = viewModel(
        key = openId.toString(),
        factory = viewModelFactory { initializer { EditorViewModel(form.draft) } },
    )
    StatusBarStyle()
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(EditorTab.Questions) }
    var typePickerFor by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }

    fun requestBack() {
        if (state.isDirty) confirmDiscard = true else actions.onBack()
    }

    BackHandler { requestBack() }

    LaunchedEffect(tab) {
        if (tab == EditorTab.Responses) actions.onLoadResponses()
    }

    Scaffold(
        containerColor = FormsColors.Canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FormsColors.Surface)
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(start = 6.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderIconButton(onClick = { requestBack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = FormsColors.Ink)
                    }
                    Text(
                        text = state.draft.title.ifBlank { "Untitled form" },
                        color = FormsColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                    )
                    HeaderIconButton(onClick = vm::undo, enabled = state.canUndo) {
                        Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Undo", tint = FormsColors.Ink)
                    }
                    HeaderIconButton(onClick = vm::redo, enabled = state.canRedo) {
                        Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "Redo", tint = FormsColors.Ink)
                    }
                    Button(
                        onClick = { actions.onSave(state.draft) },
                        enabled = state.isDirty && !saving,
                        shape = FormsShapes.Pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FormsColors.Cyan,
                            contentColor = FormsColors.OnCyan,
                            disabledContainerColor = FormsColors.Hairline,
                            disabledContentColor = FormsColors.InkMuted,
                        ),
                        modifier = Modifier.padding(start = 6.dp),
                    ) {
                        Text(if (saving) "Saving…" else "Save", fontWeight = FontWeight.ExtraBold)
                    }
                }
                if (form.skippedItems > 0) {
                    Text(
                        text = "${form.skippedItems} items this editor can't show yet are hidden. They stay in your form.",
                        color = FormsColors.InkMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                    )
                }
                UnderlineTabs(
                    labels = EditorTab.entries.map { it.label },
                    selected = tab.ordinal,
                    onSelect = { tab = EditorTab.entries[it] },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        },
        bottomBar = {
            if (tab == EditorTab.Questions) {
                QuestionToolbar(onAdd = { type -> vm.addQuestion(type) })
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        when (tab) {
            EditorTab.Questions -> {
                val lastIndex = state.draft.questions.lastIndex
                val listState = rememberLazyListState()
                val reorderState = rememberReorderableLazyListState(listState) { from, to ->
                    vm.dispatch(MoveQuestion(from.index - 1, to.index - 1))
                }
                LazyColumn(
                    state = listState,
                    modifier = content,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "header") {
                        FormHeaderCard(
                            title = state.draft.title,
                            description = state.draft.description,
                            onTitleChange = { vm.dispatch(UpdateFormTitle(it)) },
                            onDescriptionChange = { vm.dispatch(UpdateFormDescription(it)) },
                        )
                    }
                    itemsIndexed(state.draft.questions, key = { _, question -> question.id }) { index, question ->
                        ReorderableItem(reorderState, key = question.id) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "drag-elevation")
                            QuestionCard(
                                question = question,
                                quiz = form.isQuiz,
                                expanded = question.id == state.focusedId,
                                onFocus = { vm.focus(question.id) },
                                onPickType = { typePickerFor = question.id },
                                onCommand = { command ->
                                    if (command is DeleteQuestion) {
                                        val deletedTitle = question.title.ifBlank { "Question" }
                                        vm.dispatch(command)
                                        scope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Deleted \"$deletedTitle\"",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short,
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                vm.undo()
                                            }
                                        }
                                    } else {
                                        vm.dispatch(command)
                                    }
                                },
                                onMoveUp = if (index > 0) ({ vm.dispatch(MoveQuestion(index, index - 1)) }) else null,
                                onMoveDown = if (index < lastIndex) ({ vm.dispatch(MoveQuestion(index, index + 1)) }) else null,
                                dragHandle = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .draggableHandle(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Outlined.DragHandle, contentDescription = "Drag to reorder", tint = FormsColors.InkMuted)
                                    }
                                },
                                modifier = Modifier.shadow(elevation, FormsShapes.Card),
                            )
                        }
                    }
                }
            }
            EditorTab.Preview -> PreviewTab(state.draft, form.responderUri, content)
            EditorTab.Responses -> ResponsesTab(
                questions = form.draft.questions,
                state = responses,
                onRetry = actions.onLoadResponses,
                modifier = content,
            )
        }
    }

    typePickerFor?.let { id ->
        AlertDialog(
            containerColor = FormsColors.Surface,
            shape = RoundedCornerShape(28.dp),
            onDismissRequest = { typePickerFor = null },
            confirmButton = {},
            title = { Text("Question type", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    QuestionType.entries.forEach { type ->
                        TextButton(onClick = {
                            vm.dispatch(SetQuestionType(id, type))
                            typePickerFor = null
                        }, colors = ButtonDefaults.textButtonColors(contentColor = FormsColors.Ink)) {
                            Text(type.label(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            containerColor = FormsColors.Surface,
            shape = RoundedCornerShape(28.dp),
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("You have edits that haven't been saved.") },
            confirmButton = {
                DialogTextButton(onClick = {
                    confirmDiscard = false
                    actions.onBack()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                DialogTextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") }
            },
        )
    }

    saveProblem?.let { problem ->
        AlertDialog(
            containerColor = FormsColors.Surface,
            shape = RoundedCornerShape(28.dp),
            onDismissRequest = actions.onDismissProblem,
            title = { Text(if (problem.conflict) "Form changed elsewhere" else "Couldn't save") },
            text = {
                Text(
                    if (problem.conflict) {
                        "This form was edited somewhere else after you opened it. Reload to get the latest version (your unsaved changes will be lost), or keep editing and copy what you need."
                    } else {
                        problem.message
                    },
                )
            },
            confirmButton = {
                if (problem.conflict) {
                    DialogTextButton(onClick = actions.onReload) { Text("Reload form") }
                } else {
                    DialogTextButton(onClick = actions.onDismissProblem) { Text("OK") }
                }
            },
            dismissButton = {
                if (problem.conflict) {
                    DialogTextButton(onClick = actions.onDismissProblem) { Text("Keep editing") }
                }
            },
        )
    }
}

@Composable
private fun HeaderIconButton(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.35f),
    ) { content() }
}

@Composable
private fun DialogTextButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    TextButton(onClick = onClick, colors = ButtonDefaults.textButtonColors(contentColor = FormsColors.Ink)) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.copy(fontWeight = FontWeight.ExtraBold),
        ) { content() }
    }
}
