package app.formsmanager.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.api.FormResponse
import app.formsmanager.api.ResponsesState
import app.formsmanager.editor.QuestionDraft
import app.formsmanager.ui.SegmentedControl
import app.formsmanager.ui.formatDateTime

@Composable
fun ResponsesTab(
    questions: List<QuestionDraft>,
    state: ResponsesState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        ResponsesState.Idle -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                onClick = onRetry,
                shape = FormsShapes.Pill,
                colors = ButtonDefaults.buttonColors(containerColor = FormsColors.Cyan, contentColor = FormsColors.OnCyan),
            ) {
                Text("Load responses", fontWeight = FontWeight.ExtraBold)
            }
        }
        ResponsesState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FormsColors.Ink)
        }
        is ResponsesState.Failed -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Text(state.message, color = FormsColors.Ink)
            Button(
                onClick = onRetry,
                shape = FormsShapes.Pill,
                colors = ButtonDefaults.buttonColors(containerColor = FormsColors.Cyan, contentColor = FormsColors.OnCyan),
            ) {
                Text("Try again", fontWeight = FontWeight.ExtraBold)
            }
        }
        is ResponsesState.Loaded -> ResponsesContent(questions, state.responses, state.hasMore, modifier)
    }
}

@Composable
private fun ResponsesContent(
    questions: List<QuestionDraft>,
    responses: List<FormResponse>,
    hasMore: Boolean,
    modifier: Modifier,
) {
    var view by rememberSaveable { mutableIntStateOf(0) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (hasMore) {
            item {
                Surface(
                    shape = FormsShapes.Card,
                    color = FormsColors.Hero,
                    contentColor = FormsColors.Cyan,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Showing the first 500 responses",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (responses.size == 1) "1 response" else "${responses.size} responses",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FormsColors.Ink,
                )
                SegmentedControl(listOf("Summary", "Individual"), view, { view = it })
            }
        }
        if (responses.isEmpty()) {
            item { Text("No responses yet. Share the form link to collect some.", color = FormsColors.InkMuted) }
        } else if (view == 0) {
            items(questions.filter { it.questionId != null }, key = { it.id }) { question ->
                SummaryCard(question, responses)
            }
        } else {
            items(responses, key = { it.id }) { response -> ResponseCard(response) }
        }
    }
}

@Composable
private fun SummaryCard(question: QuestionDraft, responses: List<FormResponse>) {
    val values = responses.flatMap { response ->
        response.answers.filter { it.questionKey == question.id }.flatMap { it.values }
    }
    CardSurface {
        Text(
            question.title.ifBlank { "Untitled question" },
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FormsColors.Ink,
        )
        if (question.type.hasOptions) {
            val counts = values.groupingBy { it }.eachCount()
            val names = (question.options.filterNot { it.isOther }.map { it.text } + counts.keys).distinct()
            names.forEach { name -> CountBar(name, counts[name] ?: 0, responses.size) }
        } else if (values.isEmpty()) {
            Text("No answers", color = FormsColors.InkMuted)
        } else {
            values.take(5).forEach { Text(it, color = FormsColors.Ink) }
            if (values.size > 5) {
                Text("+${values.size - 5} more in Individual", color = FormsColors.InkMuted)
            }
        }
    }
}

@Composable
private fun CountBar(name: String, count: Int, total: Int) {
    val fraction = if (total == 0) 0f else count.toFloat() / total
    val percent = (fraction * 100).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(name, color = FormsColors.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("$count · $percent%", color = FormsColors.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(FormsColors.Track),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(FormsColors.CyanDeep),
            )
        }
    }
}

@Composable
private fun ResponseCard(response: FormResponse) {
    CardSurface {
        Text(
            formatDateTime(response.submittedAt),
            color = FormsColors.InkMuted,
            fontSize = 13.sp,
        )
        response.answers.forEach { line ->
            Column {
                Text(line.question, color = FormsColors.InkMuted)
                Text(line.values.joinToString(", "), color = FormsColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CardSurface(content: @Composable () -> Unit) {
    Surface(shape = FormsShapes.Card, color = FormsColors.Surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}
