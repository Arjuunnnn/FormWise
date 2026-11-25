package app.formsmanager.api

import app.formsmanager.editor.OptionDraft
import app.formsmanager.editor.QuestionDraft
import app.formsmanager.editor.QuestionType

class FormTemplate(
    val id: String,
    val name: String,
    val subtitle: String,
    val isQuiz: Boolean = false,
    val questions: () -> List<QuestionDraft>,
)

private fun question(
    title: String,
    type: QuestionType,
    required: Boolean = false,
    vararg options: String,
) = QuestionDraft(
    type = type,
    title = title,
    required = required,
    options = options.map { OptionDraft(text = it) },
)

val FormTemplates = listOf(
    FormTemplate("blank", "Blank form", "Add your own questions") { emptyList() },
    FormTemplate("event", "Event sign-up", "5 questions") {
        listOf(
            question("Full name", QuestionType.SHORT_ANSWER, true),
            question("Email address", QuestionType.SHORT_ANSWER, true),
            question("Will you attend?", QuestionType.MULTIPLE_CHOICE, true, "Yes", "No", "Maybe"),
            question("How many guests are you bringing?", QuestionType.DROPDOWN, false, "0", "1", "2", "3 or more"),
            question("Anything we should know?", QuestionType.PARAGRAPH),
        )
    },
    FormTemplate("feedback", "Feedback survey", "8 questions") {
        listOf(
            question("How would you rate the event overall?", QuestionType.MULTIPLE_CHOICE, true, "Excellent", "Good", "Average", "Poor"),
            question("How relevant was the content?", QuestionType.MULTIPLE_CHOICE, false, "Very relevant", "Somewhat relevant", "Not relevant"),
            question("How was the pace?", QuestionType.MULTIPLE_CHOICE, false, "Too slow", "Just right", "Too fast"),
            question("What did you like most?", QuestionType.PARAGRAPH),
            question("What could be improved?", QuestionType.PARAGRAPH),
            question("Which topics should we cover next?", QuestionType.CHECKBOXES, false, "Technical talks", "Workshops", "Networking", "Career advice"),
            question("Would you recommend this to a friend?", QuestionType.MULTIPLE_CHOICE, false, "Yes", "No", "Maybe"),
            question("Any other comments?", QuestionType.PARAGRAPH),
        )
    },
    FormTemplate("quiz", "Quiz", "Scored answers", isQuiz = true) {
        listOf(
            question("Your name", QuestionType.SHORT_ANSWER, true),
            question("Sample question 1", QuestionType.MULTIPLE_CHOICE, true, "Option A", "Option B", "Option C", "Option D"),
            question("Sample question 2", QuestionType.MULTIPLE_CHOICE, true, "Option A", "Option B", "Option C", "Option D"),
        )
    },
)
