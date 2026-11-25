package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.FormData
import app.formsmanager.api.FormResponse
import app.formsmanager.api.FormSummary
import app.formsmanager.editor.ui.FormsColors
import java.text.NumberFormat
import java.time.LocalTime

private class FormCard(val summary: FormSummary, val data: FormData) {
    val today = countToday(data.responses)
    val total = data.countLabel()
    val count = data.responses.size
    val spark = rangeStats(data.responses, 7).chart
}

private class LatestItem(val card: FormCard, val response: FormResponse, val index: Int)

@Composable
fun TodayScreen(
    userName: String?,
    forms: List<FormSummary>,
    data: Map<String, FormData>,
    syncing: Boolean,
    onSeeAll: () -> Unit,
    onOpenForm: (String) -> Unit,
    onOpenResponse: (String, Int) -> Unit,
    onRefresh: () -> Unit,
    onNewForm: () -> Unit,
) {
    StatusBarStyle()

    val cards = remember(forms, data) {
        forms.mapNotNull { form -> data[form.id]?.let { FormCard(form, it) } }
    }
    val openCards = remember(cards) {
        cards.filter { it.data.form.isAcceptingResponses }
            .sortedWith(compareByDescending<FormCard> { it.today }.thenByDescending { it.count })
    }
    val latest = remember(cards) {
        cards.flatMap { card -> card.data.responses.take(6).mapIndexed { i, r -> LatestItem(card, r, i) } }
            .sortedByDescending { it.response.submittedAt }
            .take(6)
    }
    val todayTotal = remember(cards) { cards.sumOf { it.today } }
    val week = remember(cards) { rangeStats(cards.flatMap { it.data.responses }, 7) }
    val hour = remember { LocalTime.now().hour }
    val loading = data.isEmpty() && (syncing || forms.isNotEmpty())
    val noForms = forms.isEmpty() && !syncing

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            val name = userName?.substringBefore(' ')?.takeIf { it.isNotBlank() }
            LargeTitle(
                title = "Today",
                eyebrow = if (name != null) "${greeting(hour)}, $name" else greeting(hour),
                trailing = {
                    if (syncing) {
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = FormsColors.CyanDeep,
                            )
                        }
                    } else {
                        CircleIconButton(Icons.Outlined.Refresh, "Refresh", onRefresh)
                    }
                },
            )
        }
        if (noForms) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Description,
                    title = "No forms yet",
                    body = "Create your first form here, or make one in Google Forms and pull down to refresh.",
                    modifier = Modifier.padding(horizontal = Gutter),
                    actionLabel = "Create a form",
                    onAction = onNewForm,
                )
            }
            return@LazyColumn
        }
        item {
            TodayHero(
                total = if (loading) "…" else NumberFormat.getInstance().format(todayTotal),
                openForms = openCards.size,
                bars = week.chart,
                labels = week.labels,
                modifier = Modifier.padding(horizontal = Gutter),
            )
        }
        item {
            SectionHeader("Open forms", Modifier.padding(top = 6.dp), actionLabel = "See all", onAction = onSeeAll)
        }
        item {
            if (openCards.isEmpty()) {
                Text(
                    if (loading) "Loading your forms…" else "No forms are accepting responses right now.",
                    color = FormsColors.InkMuted,
                    modifier = Modifier.padding(horizontal = Gutter),
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Gutter),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(openCards, key = { it.summary.id }) { card ->
                        OpenFormCard(card) { onOpenForm(card.summary.id) }
                    }
                }
            }
        }
        item {
            SectionHeader("Latest responses", Modifier.padding(top = 6.dp))
        }
        if (latest.isEmpty()) {
            item {
                Text(
                    "New responses will show up here.",
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
                    latest.forEachIndexed { i, item ->
                        if (i > 0) HorizontalDivider(color = FormsColors.Hairline, modifier = Modifier.padding(start = 68.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenResponse(item.card.summary.id, item.index) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Avatar(item.response.displayName())
                            Column(Modifier.weight(1f)) {
                                Text(item.response.displayName(), color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                                Text(item.card.summary.name, color = FormsColors.InkMuted, fontSize = 13.sp, maxLines = 1)
                            }
                            Text(
                                item.response.agoShort(),
                                color = FormsColors.InkMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayHero(
    total: String,
    openForms: Int,
    bars: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(FormsColors.Hero)
            .padding(20.dp),
    ) {
        Text("New responses today", color = FormsColors.MutedOnDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(total, color = Color.White, fontSize = 52.sp, lineHeight = 56.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                if (openForms == 1) "across 1 open form" else "across $openForms open forms",
                color = FormsColors.Cyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        MiniBars(
            values = bars,
            color = FormsColors.Cyan,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            gap = 6.dp,
            emphasizeLast = true,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach { label ->
                Text(label, color = FormsColors.MutedOnDark, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun OpenFormCard(card: FormCard, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .width(236.dp)
            .height(172.dp)
            .clip(shape)
            .background(FormsColors.Surface)
            .border(1.dp, FormsColors.Hairline, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(accepting = true, small = true)
            Spacer(Modifier.weight(1f))
            Text("${card.total} total", color = FormsColors.InkMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            card.summary.name,
            color = FormsColors.Ink,
            fontSize = 17.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text("+${card.today}", color = FormsColors.Ink, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    " today",
                    color = FormsColors.InkMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            MiniBars(
                values = card.spark,
                color = FormsColors.CyanDeep,
                modifier = Modifier.size(width = 76.dp, height = 34.dp),
                gap = 3.dp,
                emphasizeLast = true,
            )
        }
    }
}
