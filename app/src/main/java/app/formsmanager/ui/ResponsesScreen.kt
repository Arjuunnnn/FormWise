package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.FormData
import app.formsmanager.api.FormSummary
import app.formsmanager.editor.ui.FormsColors
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt

private val Ranges = listOf("7 days" to 7, "30 days" to 30, "All time" to null)

private class FormCount(val summary: FormSummary, val count: Int)

@Composable
fun ResponsesScreen(
    forms: List<FormSummary>,
    data: Map<String, FormData>,
    syncing: Boolean,
    onOpenForm: (String) -> Unit,
) {
    StatusBarStyle()

    var range by rememberSaveable { mutableIntStateOf(0) }
    val days = Ranges[range].second

    val stats = remember(data, days) { rangeStats(data.values.flatMap { it.responses }, days) }
    val perForm = remember(forms, data, days) {
        forms.mapNotNull { form ->
            data[form.id]?.let { FormCount(form, rangeStats(it.responses, days).total) }
        }.sortedByDescending { it.count }
    }
    val max = (perForm.firstOrNull()?.count ?: 0).coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { LargeTitle("Responses") }
        item {
            SegmentedControl(
                labels = Ranges.map { it.first },
                selected = range,
                onSelect = { range = it },
                modifier = Modifier.padding(horizontal = Gutter),
            )
        }
        item {
            HeroCard(stats, days, loading = data.isEmpty() && syncing, modifier = Modifier.padding(horizontal = Gutter))
        }
        item { SectionHeader("By form", Modifier.padding(top = 6.dp)) }
        if (perForm.isEmpty()) {
            item {
                Text(
                    if (syncing) "Loading your forms…" else "No data yet.",
                    color = FormsColors.InkMuted,
                    modifier = Modifier.padding(horizontal = Gutter),
                )
            }
        } else {
            item {
                WhiteCard(
                    modifier = Modifier.padding(horizontal = Gutter),
                    padding = PaddingValues(0.dp),
                ) {
                    perForm.forEachIndexed { i, item ->
                        if (i > 0) HorizontalDivider(color = FormsColors.Hairline)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenForm(item.summary.id) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "${i + 1}",
                                color = FormsColors.InkMuted,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                modifier = Modifier.width(18.dp),
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row {
                                    Text(
                                        item.summary.name,
                                        color = FormsColors.Ink,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        NumberFormat.getInstance().format(item.count),
                                        color = FormsColors.Ink,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(start = 12.dp),
                                    )
                                }
                                ProgressBar(item.count.toFloat() / max)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(stats: RangeStats, days: Int?, loading: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(FormsColors.Hero)
            .padding(20.dp),
    ) {
        Text("Across all forms", color = FormsColors.MutedOnDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(
            if (loading) "…" else NumberFormat.getInstance().format(stats.total),
            color = Color.White,
            fontSize = 52.sp,
            lineHeight = 56.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 2.dp),
        )
        deltaText(stats, days)?.let {
            Text(
                it,
                color = FormsColors.Cyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        MiniBars(
            values = stats.chart,
            color = FormsColors.Cyan,
            modifier = Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .height(84.dp),
            gap = 6.dp,
            emphasizeLast = true,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            stats.labels.forEach { label ->
                Text(label, color = FormsColors.MutedOnDark, fontSize = 12.sp)
            }
        }
    }
}

private fun deltaText(stats: RangeStats, days: Int?): String? {
    val previous = stats.previous ?: return null
    if (previous == 0) return null
    val change = ((stats.total - previous) * 100f / previous).roundToInt()
    val period = if (days == 7) "the week before" else "the $days days before"
    return when {
        change == 0 -> "Same as $period"
        change > 0 -> "$change% more than $period"
        else -> "${abs(change)}% fewer than $period"
    }
}
