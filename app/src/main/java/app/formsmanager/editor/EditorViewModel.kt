package app.formsmanager.editor

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EditorState(
    val draft: FormDraft,
    val focusedId: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isDirty: Boolean = false,
)

class EditorViewModel(initial: FormDraft) : ViewModel() {

    private val history = UndoStack(initial)
    private var savedSnapshot = initial

    private val _state = MutableStateFlow(EditorState(initial))
    val state: StateFlow<EditorState> = _state.asStateFlow()

    fun dispatch(command: EditCommand) {
        history.push(command.apply(history.current), command.coalesceKey)
        publish()
    }

    fun addQuestion(type: QuestionType = QuestionType.SHORT_ANSWER) {
        val question = QuestionDraft(type = type)
        dispatch(AddQuestion(question, afterId = _state.value.focusedId))
        focus(question.id)
    }

    fun undo() {
        if (history.undo()) publish()
    }

    fun redo() {
        if (history.redo()) publish()
    }

    fun focus(id: String?) {
        _state.update { it.copy(focusedId = id) }
    }

    fun markSaved() {
        savedSnapshot = history.current
        publish()
    }

    private fun publish() {
        val draft = history.current
        _state.update {
            it.copy(
                draft = draft,
                focusedId = it.focusedId?.takeIf { id -> draft.questions.any { q -> q.id == id } },
                canUndo = history.canUndo,
                canRedo = history.canRedo,
                isDirty = draft != savedSnapshot,
            )
        }
    }
}
