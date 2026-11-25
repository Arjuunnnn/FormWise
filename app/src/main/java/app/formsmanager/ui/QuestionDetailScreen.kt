package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.FormData
import app.formsmanager.api.FormResponse
import app.formsmanager.editor.QuestionDraft
import app.formsmanager.editor.QuestionType
import app.formsmanager.editor.ui.FormsColors
import app.formsmanager.editor.ui.label
import kotlin.math.roundToInt

private class QuestionAnswer(val response: FormResponse, val values: List<String>)

/** Everything people answered to one question: the full breakdown for choices, every answer for text. */
@Composable
fun QuestionDetailScreen(data: FormData, question: QuestionDraft, onBack: () -> Unit) {
    StatusBarStyle()
    var query by rememberSaveable { mutableStateOf("") }

    val total = data.responses.size
    val answers = remember(data.responses, question.id) {
        data.responses.mapNotNull { response ->
            response.answers.firstOrNull { it.questionKey == question.id }
                ?.takeIf { line -> line.values.any { it.isNotBlank() } }
                ?.let { QuestionAnswer(response, it.values) }
        }
    }
    val answered = answers.size
    val rate = if (total == 0) 0 else (answered * 100f / total).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas),
    ) {
        TopBar(onBack = onBack, title = "Question")
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        question.title.ifBlank { "Untitled question" },
                        color = FormsColors.Ink,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "${question.type.label()} · ${if (question.required) "Required" else "Optional"}",
                        color = FormsColors.InkMuted,
                        fontSize = 14.sp,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Answered", answered.toString(), Modifier.weight(1f))
                    StatTile("Skipped", (total - answered).toString(), Modifier.weight(1f))
                    StatTile("Rate", "$rate%", Modifier.weight(1f))
                }
            }

            if (total == 0) {
                item { Text("No responses yet.", color = FormsColors.InkMuted) }
            } else if (question.type.hasOptions) {
                val counts = answers.flatMap { it.values }.groupingBy { it }.eachCount()
                val names = (question.options.filterNot { it.isOther }.map { it.text } + counts.keys).distinct()
                item {
                    WhiteCard {
                        Text("Breakdown", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(
                            "Share of the $answered people who answered",
                            color = FormsColors.InkMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
                        )
                        names.forEach { name ->
                            val count = counts[name] ?: 0
                            val share = if (answered == 0) 0f else count.toFloat() / answered
                            Column(Modifier.padding(bottom = 14.dp)) {
                                Row(Modifier.padding(bottom = 6.dp)) {
                                    Text(
                                        name,
                                        color = FormsColors.Ink,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "$count · ${(share * 100).roundToInt()}%",
                                        color = FormsColors.Ink,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                                ProgressBar(share, height = 10.dp)
                            }
                        }
                    }
                }
            } else if (question.type == QuestionType.SCALE) {
                item {
                    WhiteCard {
                        Text("Distribution", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Column(Modifier.padding(top = 10.dp)) {
                            ScaleBars(question, answers.flatMap { it.values })
                        }
                    }
                }
            } else {
                val shown = answers.filter { a ->
                    query.isBlank() ||
                        a.values.any { it.contains(query.trim(), ignoreCase = true) } ||
                        a.response.displayName().contains(query.trim(), ignoreCase = true)
                }
                item { SearchField(query, { query = it }, "Search answers") }
                item {
                    Text(
                        if (shown.size == 1) "1 answer" else "${shown.size} answers",
                        color = FormsColors.InkMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (shown.isEmpty()) {
                    item { Text("No answers match.", color = FormsColors.InkMuted) }
                }
                items(shown, key = { it.response.id }) { answer ->
                    WhiteCard(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            answer.values.joinToString(", "),
                            color = FormsColors.Ink,
                            fontSize = 16.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${answer.response.displayName()} · ${formatRelativeTime(answer.response.submittedAt)}",
                            color = FormsColors.InkMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
