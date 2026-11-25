package app.formsmanager.api

import app.formsmanager.editor.FormDraft
import app.formsmanager.editor.OptionDraft
import app.formsmanager.editor.QuestionDraft
import app.formsmanager.editor.QuestionType
import org.json.JSONArray
import org.json.JSONObject

/** Returns a message for the first problem Google would reject, or null when the draft is fine. */
fun validateDraft(draft: FormDraft, isQuiz: Boolean = false): String? {
    if (draft.title.isBlank()) return "Give the form a title before saving."
    for (question in draft.questions) {
        val name = question.title.ifBlank { "Untitled question" }
        if (question.type.hasOptions) {
            val values = question.options.filterNot { it.isOther }.map { it.text.trim() }
            if (values.any { it.isEmpty() }) return "“$name” has an empty option. Fill it in or remove it."
            if (values.toSet().size != values.size) return "“$name” has duplicate options."
        }
        if (isQuiz && question.type.canGrade && question.points > 0 && question.correctAnswers.isEmpty()) {
            return "“$name” is worth points but has no correct answer. Mark one or set points to 0."
        }
    }
    return null
}

/**
 * Turns the difference between the loaded form and the edited draft into one ordered list of
 * batchUpdate requests: delete removed items, create and move to reach the new order, then update
 * changed fields. Items the editor can't show stay where they are. Indexes are computed against a
 * simulated copy of the server list because requests in one batch apply in sequence.
 */
fun buildSaveRequests(base: LoadedForm, draft: FormDraft): List<JSONObject> {
    val baseline = base.draft
    val requests = mutableListOf<JSONObject>()

    val infoFields = buildList {
        if (draft.title != baseline.title) add("title")
        if (draft.description != baseline.description) add("description")
    }
    if (infoFields.isNotEmpty()) {
        val info = JSONObject().put("title", draft.title).put("description", draft.description)
        requests += JSONObject().put(
            "updateFormInfo",
            JSONObject().put("info", info).put("updateMask", infoFields.joinToString(",")),
        )
    }

    val keptIds = draft.questions.mapNotNull { it.serverItemId }.toSet()
    val loadedIds = baseline.questions.mapNotNull { it.serverItemId }.toSet()
    val hidden = base.itemIds.toSet() - loadedIds
    val current = base.itemIds.toMutableList()

    for (id in loadedIds - keptIds) {
        requests += JSONObject().put("deleteItem", JSONObject().put("location", location(current.indexOf(id))))
        current.remove(id)
    }

    draft.questions.forEachIndexed { i, question ->
        val target = if (i == 0) 0 else current.indexOf(keyOf(draft.questions[i - 1])) + 1
        val id = question.serverItemId
        if (id == null) {
            requests += JSONObject().put(
                "createItem",
                JSONObject().put("item", itemJson(question, base.isQuiz)).put("location", location(target)),
            )
            current.add(target, question.id)
        } else {
            val from = current.indexOf(id)
            val inPlace = from >= target && current.subList(target, from).all { it in hidden }
            if (!inPlace) {
                requests += JSONObject().put(
                    "moveItem",
                    JSONObject().put("originalLocation", location(from)).put("newLocation", location(target)),
                )
                current.removeAt(from)
                current.add(target, id)
            }
        }
    }

    val baselineById = baseline.questions.associateBy { it.serverItemId }
    for (question in draft.questions) {
        val id = question.serverItemId ?: continue
        val old = baselineById[id] ?: continue
        val (item, mask) = diffItem(old, question, base.isQuiz) ?: continue
        requests += JSONObject().put(
            "updateItem",
            JSONObject().put("item", item).put("location", location(current.indexOf(id))).put("updateMask", mask),
        )
    }
    return requests
}

private fun keyOf(question: QuestionDraft) = question.serverItemId ?: question.id

private fun location(index: Int) = JSONObject().put("index", index)

private fun diffItem(old: QuestionDraft, new: QuestionDraft, quiz: Boolean): Pair<JSONObject, String>? {
    val item = JSONObject()
    val masks = mutableListOf<String>()

    if (old.title != new.title) {
        item.put("title", new.title)
        masks += "title"
    }
    if (old.description != new.description) {
        item.put("description", new.description)
        masks += "description"
    }

    if (old.type != new.type) {
        item.put("questionItem", JSONObject().put("question", questionJson(new, quiz)))
        masks += "questionItem.question"
    } else {
        // Same type: update only the fields that changed, so Google keeps the question's id
        // and therefore the link to every existing response.
        val question = JSONObject()
        if (old.required != new.required) {
            question.put("required", new.required)
            masks += "questionItem.question.required"
        }
        if (new.type.hasOptions) {
            val choice = JSONObject()
            if (optionKey(old) != optionKey(new)) {
                choice.put("options", optionsJson(new.options))
                masks += "questionItem.question.choiceQuestion.options"
            }
            if (old.shuffle != new.shuffle) {
                choice.put("shuffle", new.shuffle)
                masks += "questionItem.question.choiceQuestion.shuffle"
            }
            if (choice.length() > 0) question.put("choiceQuestion", choice)
        }
        if (new.type == QuestionType.SCALE && scaleKey(old) != scaleKey(new)) {
            question.put("scaleQuestion", scaleJson(new))
            masks += "questionItem.question.scaleQuestion"
        }
        if (new.type == QuestionType.DATE && (old.includeYear != new.includeYear || old.includeTime != new.includeTime)) {
            question.put("dateQuestion", dateJson(new))
            masks += "questionItem.question.dateQuestion"
        }
        if (new.type == QuestionType.TIME && old.duration != new.duration) {
            question.put("timeQuestion", timeJson(new))
            masks += "questionItem.question.timeQuestion"
        }
        if (quiz && new.type.canGrade && gradingKey(old) != gradingKey(new)) {
            // A grading object with 0 points and no answers clears the answer key.
            question.put("grading", gradingJson(new) ?: JSONObject().put("pointValue", 0))
            masks += "questionItem.question.grading"
        }
        if (question.length() > 0) {
            item.put("questionItem", JSONObject().put("question", question))
        }
    }
    return if (masks.isEmpty()) null else item to masks.joinToString(",")
}

private fun optionKey(question: QuestionDraft) = question.options.map { it.text to it.isOther }

private fun scaleKey(q: QuestionDraft) = listOf(q.scaleLow, q.scaleHigh, q.scaleLowLabel, q.scaleHighLabel)

private fun gradingKey(q: QuestionDraft) = q.points to q.correctAnswers

private fun itemJson(question: QuestionDraft, quiz: Boolean): JSONObject {
    val item = JSONObject().put("title", question.title)
    if (question.description.isNotBlank()) item.put("description", question.description)
    return item.put("questionItem", JSONObject().put("question", questionJson(question, quiz)))
}

private fun questionJson(question: QuestionDraft, quiz: Boolean): JSONObject {
    val json = JSONObject().put("required", question.required)
    when (question.type) {
        QuestionType.SHORT_ANSWER -> json.put("textQuestion", JSONObject().put("paragraph", false))
        QuestionType.PARAGRAPH -> json.put("textQuestion", JSONObject().put("paragraph", true))
        QuestionType.MULTIPLE_CHOICE,
        QuestionType.CHECKBOXES,
        QuestionType.DROPDOWN -> json.put(
            "choiceQuestion",
            JSONObject()
                .put("type", question.type.apiName())
                .put("shuffle", question.shuffle)
                .put("options", optionsJson(question.options)),
        )
        QuestionType.SCALE -> json.put("scaleQuestion", scaleJson(question))
        QuestionType.DATE -> json.put("dateQuestion", dateJson(question))
        QuestionType.TIME -> json.put("timeQuestion", timeJson(question))
    }
    if (quiz && question.type.canGrade) gradingJson(question)?.let { json.put("grading", it) }
    return json
}

private fun scaleJson(q: QuestionDraft): JSONObject {
    val json = JSONObject().put("low", q.scaleLow).put("high", q.scaleHigh)
    if (q.scaleLowLabel.isNotBlank()) json.put("lowLabel", q.scaleLowLabel)
    if (q.scaleHighLabel.isNotBlank()) json.put("highLabel", q.scaleHighLabel)
    return json
}

private fun dateJson(q: QuestionDraft) =
    JSONObject().put("includeTime", q.includeTime).put("includeYear", q.includeYear)

private fun timeJson(q: QuestionDraft) = JSONObject().put("duration", q.duration)

/** The answer key for a quiz question, or null when it has neither points nor correct answers. */
private fun gradingJson(q: QuestionDraft): JSONObject? {
    if (q.points <= 0 && q.correctAnswers.isEmpty()) return null
    val grading = JSONObject().put("pointValue", q.points)
    if (q.correctAnswers.isNotEmpty()) {
        val answers = JSONArray()
        q.correctAnswers.forEach { answers.put(JSONObject().put("value", it)) }
        grading.put("correctAnswers", JSONObject().put("answers", answers))
    }
    return grading
}

private fun optionsJson(options: List<OptionDraft>): JSONArray {
    val array = JSONArray()
    for (option in options) {
        array.put(if (option.isOther) JSONObject().put("isOther", true) else JSONObject().put("value", option.text))
    }
    return array
}

private fun QuestionType.apiName() = when (this) {
    QuestionType.MULTIPLE_CHOICE -> "RADIO"
    QuestionType.CHECKBOXES -> "CHECKBOX"
    else -> "DROP_DOWN"
}
