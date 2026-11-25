package app.formsmanager.api

import app.formsmanager.editor.FormDraft
import app.formsmanager.editor.OptionDraft
import app.formsmanager.editor.QuestionDraft
import app.formsmanager.editor.QuestionType
import org.json.JSONArray
import org.json.JSONObject

data class LoadedForm(
    val formId: String,
    val draft: FormDraft,
    val revisionId: String,
    val skippedItems: Int,
    /** Every item id on the server in order, including items this editor can't show. */
    val itemIds: List<String>,
    val responderUri: String,
    val isPublished: Boolean = true,
    val isAcceptingResponses: Boolean = true,
    val linkedSheetId: String? = null,
    val isQuiz: Boolean = false,
    /** DO_NOT_COLLECT, VERIFIED or RESPONDER_INPUT. Empty when Google didn't say. */
    val emailCollection: String = "",
)

fun parseForm(json: JSONObject): LoadedForm {
    val info = json.getJSONObject("info")
    val items = json.optJSONArray("items") ?: JSONArray()
    val publishState = json.optJSONObject("publishSettings")?.optJSONObject("publishState")
    val itemIds = mutableListOf<String>()
    var skipped = 0
    val questions = buildList {
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            itemIds += item.getString("itemId")
            val question = parseItem(item)
            if (question == null) skipped++ else add(question)
        }
    }
    return LoadedForm(
        formId = json.getString("formId"),
        draft = FormDraft(
            title = info.optString("title"),
            description = info.optString("description"),
            questions = questions,
        ),
        revisionId = json.optString("revisionId"),
        skippedItems = skipped,
        itemIds = itemIds,
        responderUri = json.optString("responderUri"),
        isPublished = publishState?.optBoolean("isPublished", true) ?: true,
        isAcceptingResponses = publishState?.optBoolean("isAcceptingResponses", true) ?: true,
        linkedSheetId = json.optString("linkedSheetId").ifEmpty { null },
        isQuiz = json.optJSONObject("settings")?.optJSONObject("quizSettings")?.optBoolean("isQuiz") ?: false,
        emailCollection = json.optJSONObject("settings")?.optString("emailCollectionType").orEmpty(),
    )
}

private fun parseItem(item: JSONObject): QuestionDraft? {
    val question = item.optJSONObject("questionItem")?.optJSONObject("question") ?: return null
    val choice = question.optJSONObject("choiceQuestion")
    val text = question.optJSONObject("textQuestion")
    val scale = question.optJSONObject("scaleQuestion")
    val date = question.optJSONObject("dateQuestion")
    val time = question.optJSONObject("timeQuestion")

    var type: QuestionType
    var options = emptyList<OptionDraft>()
    when {
        choice != null -> {
            type = when (choice.optString("type")) {
                "RADIO" -> QuestionType.MULTIPLE_CHOICE
                "CHECKBOX" -> QuestionType.CHECKBOXES
                "DROP_DOWN" -> QuestionType.DROPDOWN
                else -> return null
            }
            options = parseOptions(choice)
        }
        text != null -> {
            type = if (text.optBoolean("paragraph")) QuestionType.PARAGRAPH else QuestionType.SHORT_ANSWER
        }
        scale != null -> type = QuestionType.SCALE
        date != null -> type = QuestionType.DATE
        time != null -> type = QuestionType.TIME
        else -> return null
    }

    val grading = question.optJSONObject("grading")
    val correct = grading?.optJSONObject("correctAnswers")?.optJSONArray("answers")
    val correctAnswers = if (correct == null) emptyList() else List(correct.length()) { i ->
        correct.getJSONObject(i).optString("value")
    }

    return QuestionDraft(
        serverItemId = item.optString("itemId").ifEmpty { null },
        questionId = question.optString("questionId").ifEmpty { null },
        type = type,
        title = item.optString("title"),
        required = question.optBoolean("required"),
        options = options,
        description = item.optString("description"),
        shuffle = choice?.optBoolean("shuffle") ?: false,
        scaleLow = scale?.optInt("low", 1) ?: 1,
        scaleHigh = scale?.optInt("high", 5) ?: 5,
        scaleLowLabel = scale?.optString("lowLabel").orEmpty(),
        scaleHighLabel = scale?.optString("highLabel").orEmpty(),
        includeYear = date?.optBoolean("includeYear", true) ?: true,
        includeTime = date?.optBoolean("includeTime") ?: false,
        duration = time?.optBoolean("duration") ?: false,
        points = grading?.optInt("pointValue") ?: 0,
        correctAnswers = correctAnswers,
    )
}

private fun parseOptions(choice: JSONObject): List<OptionDraft> {
    val array = choice.optJSONArray("options") ?: return emptyList()
    return List(array.length()) { i ->
        val option = array.getJSONObject(i)
        val isOther = option.optBoolean("isOther")
        OptionDraft(text = if (isOther) "Other" else option.optString("value"), isOther = isOther)
    }
}
