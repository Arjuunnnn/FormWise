package app.formsmanager.api

import app.formsmanager.editor.QuestionDraft
import org.json.JSONObject

data class AnswerLine(
    val questionKey: String,
    val question: String,
    val values: List<String>,
    /** Points awarded, for answers Google graded in a quiz. */
    val score: Double? = null,
    /** Whether a graded answer was right. Null when the answer wasn't graded. */
    val correct: Boolean? = null,
)

data class FormResponse(
    val id: String,
    val submittedAt: String,
    val answers: List<AnswerLine>,
    val respondentEmail: String? = null,
    /** Quiz total. Google leaves it out when it is zero, so read null as 0 on a quiz form. */
    val totalScore: Double? = null,
)

data class FormResponsesPayload(
    val responses: List<FormResponse>,
    val hasMore: Boolean,
    /** Pass to the next request to continue where this page stopped. */
    val nextPageToken: String? = null,
)

sealed interface ResponsesState {
    data object Idle : ResponsesState
    data object Loading : ResponsesState
    data class Loaded(val responses: List<FormResponse>, val hasMore: Boolean = false) : ResponsesState
    data class Failed(val message: String) : ResponsesState
}

fun parseResponses(json: JSONObject, questions: List<QuestionDraft>): FormResponsesPayload {
    val token = json.optString("nextPageToken").ifBlank { null }
    val array = json.optJSONArray("responses") ?: return FormResponsesPayload(emptyList(), token != null, token)
    val hasMore = token != null
    val responses = List(array.length()) { i ->
        val response = array.getJSONObject(i)
        val answers = response.optJSONObject("answers")
        val lines = questions.mapNotNull { question ->
            val questionId = question.questionId ?: return@mapNotNull null
            val answer = answers?.optJSONObject(questionId) ?: return@mapNotNull null
            val given = answer.optJSONObject("textAnswers")?.optJSONArray("answers") ?: return@mapNotNull null
            // Google omits false and 0 in graded answers, so a present grade object with no fields is a wrong answer.
            val grade = answer.optJSONObject("grade")
            AnswerLine(
                questionKey = question.id,
                question = question.title.ifBlank { "Untitled question" },
                values = List(given.length()) { j -> given.getJSONObject(j).optString("value") },
                score = grade?.optDouble("score", 0.0),
                correct = grade?.optBoolean("correct", false),
            )
        }
        FormResponse(
            id = response.optString("responseId"),
            submittedAt = response.optString("lastSubmittedTime").ifEmpty { response.optString("createTime") },
            answers = lines,
            respondentEmail = response.optString("respondentEmail").ifEmpty { null },
            totalScore = if (response.has("totalScore")) response.optDouble("totalScore") else null,
        )
    }
    return FormResponsesPayload(responses.sortedByDescending { it.submittedAt }, hasMore, token)
}
