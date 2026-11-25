package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.FormData
import app.formsmanager.api.FormResponse
import app.formsmanager.editor.ui.FormsColors

class ResponseActions(
    val onBack: () -> Unit,
    val onShow: (Int) -> Unit,
    val onCopy: (String) -> Unit,
    val onOpenSheets: () -> Unit,
)

@Composable
fun ResponseViewerScreen(data: FormData, index: Int, actions: ResponseActions) {
    StatusBarStyle()
    val responses = data.responses
    val response = responses.getOrNull(index)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas),
    ) {
        TopBar(onBack = actions.onBack, title = "Response ${responses.size - index} of ${responses.size}") {
            IconAction(
                icon = Icons.Outlined.ChevronLeft,
                description = "Older response",
                onClick = { actions.onShow(index + 1) },
                enabled = index < responses.lastIndex,
            )
            IconAction(
                icon = Icons.Outlined.ChevronRight,
                description = "Newer response",
                onClick = { actions.onShow(index - 1) },
                enabled = index > 0,
            )
        }

        if (response != null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Avatar(response.displayName(), size = 56.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                response.displayName(),
                                color = FormsColors.Ink,
                                fontSize = 24.sp,
                                lineHeight = 29.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                listOfNotNull(response.respondentEmail, formatRelativeTime(response.submittedAt)).joinToString(" · "),
                                color = FormsColors.InkMuted,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            if (data.form.isQuiz) {
                                val possible = data.form.draft.questions.sumOf { it.points }
                                Chip(
                                    text = "Score ${"%.1f".format(response.totalScore ?: 0.0)} / $possible",
                                    container = FormsColors.Tint,
                                    content = FormsColors.CyanDeep,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
                item {
                    WhiteCard(padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                        response.answers.forEachIndexed { i, line ->
                            if (i > 0) HorizontalDivider(color = FormsColors.Hairline)
                            Column(Modifier.padding(vertical = 14.dp)) {
                                Text(
                                    line.question,
                                    color = FormsColors.InkMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    line.values.joinToString(", "),
                                    color = FormsColors.Ink,
                                    fontSize = 17.sp,
                                    lineHeight = 23.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                val correct = line.correct
                                if (data.form.isQuiz && correct != null) {
                                    Text(
                                        (if (correct) "Correct" else "Incorrect") + " · ${"%.1f".format(line.score ?: 0.0)} pts",
                                        color = if (correct) FormsColors.CyanDeep else FormsColors.Error,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                        if (response.answers.isEmpty()) {
                            Text("No answers recorded.", color = FormsColors.InkMuted, modifier = Modifier.padding(vertical = 14.dp))
                        }
                    }
                }
            }
        } else {
            Column(Modifier.weight(1f)) {}
        }

        BottomActionBar {
            SecondaryButton(
                text = "Copy answers",
                onClick = { response?.let { actions.onCopy(it.asText()) } },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton("Open in Sheets", actions.onOpenSheets, Modifier.weight(1f))
        }
    }
}

private fun FormResponse.asText() =
    answers.joinToString("\n\n") { "${it.question}\n${it.values.joinToString(", ")}" }
