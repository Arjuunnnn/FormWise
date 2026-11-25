package app.formsmanager.editor

import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

enum class QuestionType {
    SHORT_ANSWER, PARAGRAPH, MULTIPLE_CHOICE, CHECKBOXES, DROPDOWN, SCALE, DATE, TIME;

    val hasOptions: Boolean
        get() = this == MULTIPLE_CHOICE || this == CHECKBOXES || this == DROPDOWN

    /** Question types Google lets a quiz grade through the API. */
    val canGrade: Boolean
        get() = hasOptions || this == SHORT_ANSWER
}

data class OptionDraft(
    val id: String = newId(),
    val text: String = "",
    val isOther: Boolean = false,
)

data class QuestionDraft(
    val id: String = newId(),
    val serverItemId: String? = null,
    val questionId: String? = null,
    val type: QuestionType = QuestionType.SHORT_ANSWER,
    val title: String = "",
    val required: Boolean = false,
    val options: List<OptionDraft> = emptyList(),
    /** Help text shown under the question. */
    val description: String = "",
    /** Choice questions: show options in a random order. */
    val shuffle: Boolean = false,
    /** Scale questions: Google allows 0 or 1 for [scaleLow] and 2 to 10 for [scaleHigh]. */
    val scaleLow: Int = 1,
    val scaleHigh: Int = 5,
    val scaleLowLabel: String = "",
    val scaleHighLabel: String = "",
    val includeYear: Boolean = true,
    val includeTime: Boolean = false,
    /** Time questions: ask for a length of time instead of a time of day. */
    val duration: Boolean = false,
    /** Quiz grading. Only sent to Google when the form is a quiz. */
    val points: Int = 0,
    val correctAnswers: List<String> = emptyList(),
)

data class FormDraft(
    val title: String = "",
    val description: String = "",
    val questions: List<QuestionDraft> = emptyList(),
)

private fun FormDraft.mapQuestion(id: String, transform: (QuestionDraft) -> QuestionDraft) =
    copy(questions = questions.map { if (it.id == id) transform(it) else it })

private fun defaultOptions(type: QuestionType) =
    if (type.hasOptions) listOf(OptionDraft(text = "Option 1")) else emptyList()

/**
 * Every edit is a command. Commands sharing a non-null [coalesceKey] merge into a single
 * undo step (typing in one field is one step, not one per keystroke).
 */
sealed interface EditCommand {
    val coalesceKey: String? get() = null
    fun apply(draft: FormDraft): FormDraft
}

data class UpdateFormTitle(val text: String) : EditCommand {
    override val coalesceKey = "form-title"
    override fun apply(draft: FormDraft) = draft.copy(title = text)
}

data class UpdateFormDescription(val text: String) : EditCommand {
    override val coalesceKey = "form-description"
    override fun apply(draft: FormDraft) = draft.copy(description = text)
}

data class AddQuestion(
    val question: QuestionDraft,
    val afterId: String? = null,
) : EditCommand {
    override fun apply(draft: FormDraft): FormDraft {
        val q = question.copy(options = question.options.ifEmpty { defaultOptions(question.type) })
        val index = draft.questions.indexOfFirst { it.id == afterId }
        val at = if (index < 0) draft.questions.size else index + 1
        return draft.copy(questions = draft.questions.toMutableList().apply { add(at, q) })
    }
}

data class DeleteQuestion(val id: String) : EditCommand {
    override fun apply(draft: FormDraft) =
        draft.copy(questions = draft.questions.filterNot { it.id == id })
}

data class DuplicateQuestion(val id: String) : EditCommand {
    override fun apply(draft: FormDraft): FormDraft {
        val index = draft.questions.indexOfFirst { it.id == id }
        if (index < 0) return draft
        val source = draft.questions[index]
        val copy = source.copy(
            id = newId(),
            serverItemId = null,
            questionId = null,
            options = source.options.map { it.copy(id = newId()) },
        )
        return draft.copy(questions = draft.questions.toMutableList().apply { add(index + 1, copy) })
    }
}

data class MoveQuestion(val from: Int, val to: Int) : EditCommand {
    override fun apply(draft: FormDraft): FormDraft {
        if (from !in draft.questions.indices || to !in draft.questions.indices) return draft
        val list = draft.questions.toMutableList()
        list.add(to, list.removeAt(from))
        return draft.copy(questions = list)
    }
}

data class UpdateQuestionTitle(val id: String, val text: String) : EditCommand {
    override val coalesceKey = "q-title-$id"
    override fun apply(draft: FormDraft) = draft.mapQuestion(id) { it.copy(title = text) }
}

data class SetRequired(val id: String, val required: Boolean) : EditCommand {
    override fun apply(draft: FormDraft) = draft.mapQuestion(id) { it.copy(required = required) }
}

data class SetQuestionType(val id: String, val type: QuestionType) : EditCommand {
    override fun apply(draft: FormDraft) = draft.mapQuestion(id) { q ->
        val kept = when {
            !type.hasOptions -> emptyList()
            type == QuestionType.DROPDOWN -> q.options.filterNot { it.isOther }
            else -> q.options
        }
        val answers = when {
            !type.canGrade -> emptyList()
            type == QuestionType.MULTIPLE_CHOICE || type == QuestionType.DROPDOWN -> q.correctAnswers.take(1)
            else -> q.correctAnswers
        }
        q.copy(
            type = type,
            options = kept.ifEmpty { defaultOptions(type) },
            points = if (type.canGrade) q.points else 0,
            correctAnswers = answers,
            shuffle = type.hasOptions && q.shuffle,
        )
    }
}

data class UpdateQuestionDescription(val id: String, val text: String) : EditCommand {
    override val coalesceKey = "q-desc-$id"
    override fun apply(draft: FormDraft) = draft.mapQuestion(id) { it.copy(description = text) }
}

data class SetShuffle(val id: String, val shuffle: Boolean) : EditCommand {
    override fun apply(draft: FormDraft) = draft.mapQuestion(id) { it.copy(shuffle = shuffle) }
}

data class SetScaleRange(val id: String, val low: Int, val high: Int) : EditCommand {
    override fun apply(draft: FormDraft) =
        draft.mapQuestion(id) { it.copy(scaleLow = low.coerceIn(0, 1), scaleHigh = high.coerceIn(2, 10)) }
}

data class UpdateScaleLabel(val id: String, val high: Boolean, val text: String) : EditCommand {
    override val coalesceKey = "q-scale-${if (high) "high" else "low"}-$id"
    override fun apply(draft: FormDraft) = draft.mapQuestion(id) {
        if (high) it.copy(scaleHighLabel = text) else it.copy(scaleLowLabel = text)
    }
}

data class SetDateOptions(val id: String, val includeYear: Boolean, val includeTime: Boolean) : EditCommand {
    override fun apply(draft: FormDraft) =
        draft.mapQuestion(id) { it.copy(includeYear = includeYear, includeTime = includeTime) }
}

data class SetDuration(val id: String, val duration: Boolean) : EditCommand {
    override fun apply(draft: FormDraft) = draft.mapQuestion(id) { it.copy(duration = duration) }
}

data class SetPoints(val id: String, val points: Int) : EditCommand {
    override fun apply(draft: FormDraft) = draft.mapQuestion(id) { it.copy(points = points.coerceIn(0, 100)) }
}

/** Marks or unmarks an option as correct. Single-answer types keep only one correct option. */
data class ToggleCorrectAnswer(val id: String, val value: String) : EditCommand {
    override fun apply(draft: FormDraft) = draft.mapQuestion(id) { q ->
        val single = q.type != QuestionType.CHECKBOXES
        val next = when {
            value in q.correctAnswers -> q.correctAnswers - value
            single -> listOf(value)
            else -> q.correctAnswers + value
        }
        q.copy(correctAnswers = next, points = if (next.isNotEmpty() && q.points == 0) 1 else q.points)
    }
}

/** Short-answer quizzes: the one accepted answer. */
data class SetCorrectText(val id: String, val text: String) : EditCommand {
    override val coalesceKey = "q-correct-$id"
    override fun apply(draft: FormDraft) = draft.mapQuestion(id) { q ->
        q.copy(
            correctAnswers = if (text.isBlank()) emptyList() else listOf(text),
            points = if (text.isNotBlank() && q.points == 0) 1 else q.points,
        )
    }
}

data class AddOption(val questionId: String) : EditCommand {
    override fun apply(draft: FormDraft) = draft.mapQuestion(questionId) { q ->
        val regular = q.options.filterNot { it.isOther }
        val other = q.options.filter { it.isOther }
        q.copy(options = regular + OptionDraft(text = "Option ${regular.size + 1}") + other)
    }
}

data class AddOtherOption(val questionId: String) : EditCommand {
    override fun apply(draft: FormDraft) = draft.mapQuestion(questionId) { q ->
        if (q.type == QuestionType.DROPDOWN || q.options.any { it.isOther }) q
        else q.copy(options = q.options + OptionDraft(text = "Other", isOther = true))
    }
}

data class UpdateOption(val questionId: String, val optionId: String, val text: String) : EditCommand {
    override val coalesceKey = "option-$optionId"
    override fun apply(draft: FormDraft) = draft.mapQuestion(questionId) { q ->
        val old = q.options.firstOrNull { it.id == optionId }?.text
        q.copy(
            options = q.options.map { if (it.id == optionId) it.copy(text = text) else it },
            correctAnswers = q.correctAnswers.map { if (it == old) text else it },
        )
    }
}

data class RemoveOption(val questionId: String, val optionId: String) : EditCommand {
    override fun apply(draft: FormDraft) = draft.mapQuestion(questionId) { q ->
        if (q.options.size <= 1) q else {
            val gone = q.options.firstOrNull { it.id == optionId }?.text
            q.copy(
                options = q.options.filterNot { it.id == optionId },
                correctAnswers = q.correctAnswers.filterNot { it == gone },
            )
        }
    }
}

data class MoveOption(val questionId: String, val from: Int, val to: Int) : EditCommand {
    override fun apply(draft: FormDraft) = draft.mapQuestion(questionId) { q ->
        if (from !in q.options.indices || to !in q.options.indices) q
        else q.copy(options = q.options.toMutableList().apply { add(to, removeAt(from)) })
    }
}

/** Snapshot-based history. Drafts are immutable, so snapshots share structure and stay cheap. */
class UndoStack(initial: FormDraft, private val limit: Int = 100) {
    var current: FormDraft = initial
        private set

    private val past = ArrayDeque<FormDraft>()
    private val future = ArrayDeque<FormDraft>()
    private var lastKey: String? = null

    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()

    fun push(next: FormDraft, key: String?) {
        if (next == current) return
        if (key == null || key != lastKey) {
            past.addLast(current)
            if (past.size > limit) past.removeFirst()
        }
        lastKey = key
        future.clear()
        current = next
    }

    fun undo(): Boolean {
        val previous = past.removeLastOrNull() ?: return false
        future.addLast(current)
        current = previous
        lastKey = null
        return true
    }

    fun redo(): Boolean {
        val next = future.removeLastOrNull() ?: return false
        past.addLast(current)
        current = next
        lastKey = null
        return true
    }
}
