package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.FormData
import app.formsmanager.api.FormResponse
import app.formsmanager.api.FormSummary
import app.formsmanager.editor.QuestionDraft
import app.formsmanager.editor.QuestionType
import app.formsmanager.editor.ui.FormsColors
import app.formsmanager.editor.ui.label
import kotlin.math.roundToInt

private val Tabs = listOf("Summary", "Questions", "Responses")

class DetailActions(
    val onBack: () -> Unit,
    val onSettings: () -> Unit,
    val onOpenSheets: () -> Unit,
    val onEditHere: () -> Unit,
    val onEditInGoogle: () -> Unit,
    val onOpenResponse: (Int) -> Unit,
    val onOpenQuestion: (String) -> Unit,
    val onToggleStar: () -> Unit,
    val onInsights: () -> Unit,
    val onExport: () -> Unit,
    val onLoadMore: () -> Unit,
    val onRetry: () -> Unit,
)

@Composable
fun FormDetailScreen(
    summary: FormSummary,
    data: FormData?,
    error: String?,
    actions: DetailActions,
) {
    StatusBarStyle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val form = data?.form
    val title = form?.draft?.title?.ifBlank { null } ?: summary.name

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas),
    ) {
        TopBar(onBack = actions.onBack) {
            IconAction(
                icon = if (summary.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                description = if (summary.starred) "Remove star" else "Add star",
                onClick = actions.onToggleStar,
                tint = if (summary.starred) FormsColors.CyanDeep else FormsColors.Ink,
            )
            IconAction(Icons.Outlined.Insights, "Insights", actions.onInsights)
            IconAction(Icons.Outlined.Tune, "Share and settings", actions.onSettings)
        }

        Column(
            modifier = Modifier
                .padding(horizontal = Gutter)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (data != null) StatusChip(data.form.isAcceptingResponses)
            Text(
                title,
                color = FormsColors.Ink,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (data != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = Gutter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile("Responses", data.countLabel(), Modifier.weight(1f))
                StatTile("Today", countToday(data.responses).toString(), Modifier.weight(1f))
                StatTile("Questions", data.form.draft.questions.size.toString(), Modifier.weight(1f))
            }
        }

        UnderlineTabs(Tabs, tab, { tab = it }, Modifier.padding(horizontal = Gutter))

        Box(Modifier.weight(1f)) {
            when {
                data == null && error != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    Text(error, color = FormsColors.Ink, textAlign = TextAlign.Center)
                    PrimaryButton("Try again", actions.onRetry)
                }
                data == null -> FullScreenLoading(FormsColors.Canvas, FormsColors.CyanDeep)
                tab == 0 -> SummaryTab(data, actions.onOpenQuestion)
                tab == 1 -> QuestionsTab(data)
                else -> ResponsesList(data, actions.onOpenResponse, actions.onExport, actions.onLoadMore)
            }
        }

        BottomActionBar {
            if (tab == 1) {
                SecondaryButton("Edit here", actions.onEditHere, Modifier.weight(1f))
                PrimaryButton("Edit in Google Forms", actions.onEditInGoogle, Modifier.weight(1.6f))
            } else {
                PrimaryButton("Open in Sheets", actions.onOpenSheets, Modifier.weight(1f))
            }
        }
    }
}

private val ContentPadding = PaddingValues(start = Gutter, end = Gutter, top = 16.dp, bottom = 24.dp)

@Composable
private fun SummaryTab(data: FormData, onOpenQuestion: (String) -> Unit) {
    val questions = data.form.draft.questions.filter { it.questionId != null }
    LazyColumn(
        contentPadding = ContentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (data.form.isQuiz && data.responses.isNotEmpty()) {
            item { QuizScoreCard(data) }
        }
        if (data.responses.isEmpty() || questions.isEmpty()) {
            item {
                Text(
                    if (questions.isEmpty()) "This form has no questions the app can summarise yet." else "No responses yet. Share the form link to collect some.",
                    color = FormsColors.InkMuted,
                )
            }
        }
        if (data.responses.isNotEmpty()) {
            itemsIndexed(questions, key = { _, q -> q.id }) { _, question ->
                SummaryCard(question, data.responses) { onOpenQuestion(question.id) }
            }
        }
    }
}

@Composable
private fun SummaryCard(question: QuestionDraft, responses: List<FormResponse>, onOpen: () -> Unit) {
    val values = responses.mapNotNull { response ->
        response.answers.firstOrNull { it.questionKey == question.id }?.values
    }
    val answered = values.size
    WhiteCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                question.title.ifBlank { "Untitled question" },
                color = FormsColors.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Outlined.ChevronRight, contentDescription = "See all answers", tint = FormsColors.InkMuted)
        }
        Text(
            "$answered of ${responses.size} answered",
            color = FormsColors.InkMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
        )
        if (question.type.hasOptions) {
            val counts = values.flatten().groupingBy { it }.eachCount()
            val names = (question.options.filterNot { it.isOther }.map { it.text } + counts.keys).distinct()
            names.forEach { name ->
                val share = if (answered == 0) 0f else (counts[name] ?: 0).toFloat() / answered
                Column(Modifier.padding(bottom = 12.dp)) {
                    Row(Modifier.padding(bottom = 6.dp)) {
                        Text(
                            name,
                            color = FormsColors.Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${(share * 100).roundToInt()}%", color = FormsColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    ProgressBar(share, height = 10.dp)
                }
            }
        } else if (question.type == QuestionType.SCALE) {
            ScaleBars(question, values.flatten())
        } else {
            val recent = values.flatten().filter { it.isNotBlank() }.take(3)
            if (recent.isEmpty()) {
                Text("No answers", color = FormsColors.InkMuted)
            }
            recent.forEach {
                Text(
                    it,
                    color = FormsColors.Ink,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(FormsColors.Canvas, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun QuizScoreCard(data: FormData) {
    val possible = data.form.draft.questions.sumOf { it.points }
    val scores = data.responses.map { it.totalScore ?: 0.0 }
    val average = if (scores.isEmpty()) 0.0 else scores.average()
    WhiteCard {
        Text("Quiz scores", color = FormsColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile("Average", "${"%.1f".format(average)} / $possible", Modifier.weight(1f))
            StatTile("Highest", "${"%.1f".format(scores.maxOrNull() ?: 0.0)}", Modifier.weight(1f))
            StatTile("Lowest", "${"%.1f".format(scores.minOrNull() ?: 0.0)}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuestionsTab(data: FormData) {
    val questions = data.form.draft.questions
    val total = data.responses.size
    LazyColumn(
        contentPadding = ContentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (questions.isEmpty()) {
            item { Text("No questions yet. Tap Edit here to add some.", color = FormsColors.InkMuted) }
        }
        itemsIndexed(questions, key = { _, q -> q.id }) { index, question ->
            val answered = data.responses.count { r -> r.answers.any { it.questionKey == question.id } }
            val rate = if (total == 0) 0f else answered.toFloat() / total
            WhiteCard(padding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${index + 1}",
                        color = FormsColors.CyanDeep,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.width(20.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            question.title.ifBlank { "Untitled question" },
                            color = FormsColors.Ink,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            "${question.type.label()} · ${if (question.required) "Required" else "Optional"}",
                            color = FormsColors.InkMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Column(Modifier.width(64.dp), horizontalAlignment = Alignment.End) {
                        Text(
                            "${(rate * 100).roundToInt()}%",
                            color = FormsColors.Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        ProgressBar(rate)
                    }
                }
            }
        }
        if (data.form.skippedItems > 0) {
            item {
                Text(
                    "${data.form.skippedItems} other items (sections, grids, dates) aren't shown here.",
                    color = FormsColors.InkMuted,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun ResponsesList(data: FormData, onOpen: (Int) -> Unit, onExport: () -> Unit, onLoadMore: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(data.responses, query) {
        val q = query.trim()
        data.responses.withIndex().filter { (_, r) ->
            q.isEmpty() ||
                r.displayName().contains(q, ignoreCase = true) ||
                r.respondentEmail?.contains(q, ignoreCase = true) == true ||
                r.answers.any { line -> line.values.any { it.contains(q, ignoreCase = true) } }
        }
    }
    LazyColumn(
        contentPadding = ContentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (data.responses.isEmpty()) {
            item { Text("No responses yet.", color = FormsColors.InkMuted) }
        } else {
            item { SearchField(query, { query = it }, "Search responses") }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (shown.size == data.responses.size) "${data.countLabel()} responses" else "${shown.size} of ${data.responses.size}",
                        color = FormsColors.InkMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Export CSV",
                        color = FormsColors.CyanDeep,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .clickable(onClick = onExport)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
            if (shown.isEmpty()) {
                item { Text("No responses match.", color = FormsColors.InkMuted) }
            }
        }
        items(shown, key = { it.value.id }) { (index, response) ->
            WhiteCard(
                onClick = { onOpen(index) },
                padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(response.displayName())
                    Column(Modifier.weight(1f)) {
                        Text(response.displayName(), color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text(
                            listOfNotNull(response.respondentEmail, formatRelativeTime(response.submittedAt)).joinToString(" · "),
                            color = FormsColors.InkMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (data.form.isQuiz) {
                        Text(
                            "${"%.1f".format(response.totalScore ?: 0.0)} pts",
                            color = FormsColors.CyanDeep,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                        )
                    } else {
                        Text("#${data.responses.size - index}", color = FormsColors.InkMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = FormsColors.InkMuted)
                }
            }
        }
        if (data.hasMore) {
            item {
                SecondaryButton("Load more responses", onLoadMore, Modifier.fillMaxWidth())
            }
        }
    }
}
