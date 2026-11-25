package app.formsmanager.ui

import app.formsmanager.FormData
import java.util.Locale

/** Builds a CSV of the loaded responses, oldest first. */
object CsvExport {

    fun columns(data: FormData, includeEmail: Boolean, includeScore: Boolean): List<String> = buildList {
        add("Submitted")
        if (includeEmail) add("Email")
        if (includeScore) add("Score")
        data.form.draft.questions.filter { it.questionId != null }.forEach { add(it.title.ifBlank { "Untitled question" }) }
    }

    fun build(data: FormData, includeEmail: Boolean, includeScore: Boolean): String {
        val questions = data.form.draft.questions.filter { it.questionId != null }
        // A byte-order mark makes Excel read the file as UTF-8.
        val out = StringBuilder("\uFEFF")
        out.append(row(columns(data, includeEmail, includeScore)))
        for (response in data.responses.asReversed()) {
            val cells = buildList {
                add(response.submittedAt)
                if (includeEmail) add(response.respondentEmail.orEmpty())
                if (includeScore) add(String.format(Locale.ROOT, "%.1f", response.totalScore ?: 0.0))
                for (question in questions) {
                    add(response.answers.firstOrNull { it.questionKey == question.id }?.values?.joinToString("; ").orEmpty())
                }
            }
            out.append(row(cells))
        }
        return out.toString()
    }

    private fun row(cells: List<String>) = cells.joinToString(",") { cell(it) } + "\r\n"

    /**
     * Quotes a value. Answers come from strangers, so a cell that begins with = + - or @ is prefixed
     * with an apostrophe to stop a spreadsheet from running it as a formula.
     */
    private fun cell(raw: String): String {
        val safe = if (raw.isNotEmpty() && raw[0] in "=+-@\t\r") "'$raw" else raw
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
