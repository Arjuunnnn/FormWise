package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.FormData
import app.formsmanager.editor.ui.FormsColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private class Insight(
    val total: Int,
    val last7: Int,
    val last30: Int,
    /** Oldest day first, 30 entries ending today. */
    val daily: List<Int>,
    /** Monday first. */
    val weekday: List<Int>,
    val hours: List<Int>,
    /** Question title to the share of responses that answered it, lowest first. */
    val answerRates: List<Pair<String, Float>>,
    /** Quiz only: how many responses scored in each fifth of the possible points. */
    val scoreBuckets: List<Int>?,
    val averageScore: Double?,
    val possiblePoints: Int,
)

private fun computeInsight(data: FormData): Insight {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val daily = IntArray(30)
    val weekday = IntArray(7)
    val hours = IntArray(24)
    for (response in data.responses) {
        val moment = response.instant()?.atZone(zone) ?: continue
        val offset = ChronoUnit.DAYS.between(moment.toLocalDate(), today).toInt()
        if (offset in 0..29) daily[29 - offset]++
        weekday[moment.dayOfWeek.value - 1]++
        hours[moment.hour]++
    }

    val questions = data.form.draft.questions.filter { it.questionId != null }
    val total = data.responses.size
    val rates = questions.map { question ->
        val answered = data.responses.count { r ->
            r.answers.any { it.questionKey == question.id && it.values.any { v -> v.isNotBlank() } }
        }
        question.title.ifBlank { "Untitled question" } to if (total == 0) 0f else answered.toFloat() / total
    }.sortedBy { it.second }

    val possible = data.form.draft.questions.sumOf { it.points }
    val scores = data.responses.map { it.totalScore ?: 0.0 }
    val buckets = if (data.form.isQuiz && possible > 0) {
        val counts = IntArray(5)
        scores.forEach { counts[((it / possible) * 5).toInt().coerceIn(0, 4)]++ }
        counts.toList()
    } else {
        null
    }

    return Insight(
        total = total,
        last7 = daily.takeLast(7).sum(),
        last30 = daily.sum(),
        daily = daily.toList(),
        weekday = weekday.toList(),
        hours = hours.toList(),
        answerRates = rates,
        scoreBuckets = buckets,
        averageScore = if (data.form.isQuiz && scores.isNotEmpty()) scores.average() else null,
        possiblePoints = possible,
    )
}

/** Trends worked out from the responses already loaded: when people answer, what they skip, how a quiz went. */
@Composable
fun InsightsScreen(data: FormData, onBack: () -> Unit, onLoadMore: () -> Unit) {
    StatusBarStyle()
    val insight = remember(data.responses, data.form.isQuiz) { computeInsight(data) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas),
    ) {
        TopBar(onBack = onBack, title = "Insights")
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (insight.total == 0) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Insights,
                        title = "Nothing to show yet",
                        body = "Insights appear once people start responding.",
                    )
                }
                return@LazyColumn
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Total", data.countLabel(), Modifier.weight(1f))
                    StatTile("Last 7 days", insight.last7.toString(), Modifier.weight(1f))
                    StatTile("Last 30 days", insight.last30.toString(), Modifier.weight(1f))
                }
            }

            item {
                ChartCard("Last 30 days", "Responses per day") {
                    MiniBars(
                        values = insight.daily,
                        color = FormsColors.CyanDeep,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        gap = 2.dp,
                        emphasizeLast = true,
                    )
                    val today = LocalDate.now()
                    LabelRow(listOf(shortDay(today.minusDays(29)), shortDay(today)))
                }
            }

            item {
                val best = insight.weekday.indices.maxByOrNull { insight.weekday[it] } ?: 0
                ChartCard(
                    "Busiest day",
                    if (insight.weekday[best] == 0) "Not enough data" else "Most responses come in on ${DayOfWeek.of(best + 1).getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                ) {
                    MiniBars(
                        values = insight.weekday,
                        color = FormsColors.CyanDeep,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        gap = 6.dp,
                    )
                    LabelRow((1..7).map { DayOfWeek.of(it).getDisplayName(TextStyle.NARROW, Locale.getDefault()) })
                }
            }

            item {
                val peak = insight.hours.indices.maxByOrNull { insight.hours[it] } ?: 0
                val hourFormat = DateTimeFormatter.ofPattern("h a", Locale.getDefault())
                ChartCard(
                    "Time of day",
                    "Busiest around ${java.time.LocalTime.of(peak, 0).format(hourFormat)} (your time zone)",
                ) {
                    MiniBars(
                        values = insight.hours,
                        color = FormsColors.CyanDeep,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        gap = 2.dp,
                    )
                    LabelRow(listOf("12 am", "6 am", "12 pm", "6 pm", "11 pm"))
                }
            }

            if (insight.answerRates.isNotEmpty()) {
                item {
                    WhiteCard {
                        Text("Most skipped questions", color = FormsColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Share of people who answered",
                            color = FormsColors.InkMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
                        )
                        insight.answerRates.take(5).forEach { (title, rate) ->
                            Column(Modifier.padding(bottom = 12.dp)) {
                                Row(Modifier.padding(bottom = 6.dp)) {
                                    Text(
                                        title,
                                        color = FormsColors.Ink,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text("${(rate * 100).roundToInt()}%", color = FormsColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                ProgressBar(rate, height = 10.dp)
                            }
                        }
                    }
                }
            }

            insight.scoreBuckets?.let { buckets ->
                item {
                    ChartCard(
                        "Quiz scores",
                        "Average ${"%.1f".format(insight.averageScore ?: 0.0)} of ${insight.possiblePoints} points",
                    ) {
                        MiniBars(
                            values = buckets,
                            color = FormsColors.CyanDeep,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            gap = 8.dp,
                        )
                        LabelRow(listOf("0–20%", "20–40%", "40–60%", "60–80%", "80–100%"))
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (data.hasMore) {
                            "Based on the ${data.responses.size} responses loaded so far. This form has more."
                        } else {
                            "Based on all ${data.responses.size} responses."
                        },
                        color = FormsColors.InkMuted,
                        fontSize = 13.sp,
                    )
                    if (data.hasMore) {
                        SecondaryButton("Load more responses", onLoadMore, Modifier.fillMaxWidth(), height = 46.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    WhiteCard {
        Text(title, color = FormsColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            subtitle,
            color = FormsColors.InkMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
        )
        content()
    }
}

@Composable
private fun LabelRow(labels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEach { Text(it, color = FormsColors.InkMuted, fontSize = 12.sp) }
    }
}

private fun shortDay(date: LocalDate) =
    "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
