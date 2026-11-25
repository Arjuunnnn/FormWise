package app.formsmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.editor.QuestionDraft
import app.formsmanager.editor.ui.FormsColors
import kotlin.math.roundToInt

/** Average plus one bar per step for a linear-scale question. [answers] are the raw answer strings. */
@Composable
fun ScaleBars(question: QuestionDraft, answers: List<String>) {
    val numbers = answers.mapNotNull { it.trim().toIntOrNull() }
    if (numbers.isEmpty()) {
        Text("No answers", color = FormsColors.InkMuted)
        return
    }
    val counts = numbers.groupingBy { it }.eachCount()
    Text(
        "Average ${"%.1f".format(numbers.average())}",
        color = FormsColors.Ink,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    if (question.scaleLowLabel.isNotBlank() || question.scaleHighLabel.isNotBlank()) {
        Text(
            "${question.scaleLow} = ${question.scaleLowLabel.ifBlank { "low" }}, ${question.scaleHigh} = ${question.scaleHighLabel.ifBlank { "high" }}",
            color = FormsColors.InkMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (step in question.scaleLow..question.scaleHigh) {
            val count = counts[step] ?: 0
            val share = count.toFloat() / numbers.size
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(step.toString(), color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 2.dp))
                Column(Modifier.weight(1f)) { ProgressBar(share, height = 10.dp) }
                Text("$count · ${(share * 100).roundToInt()}%", color = FormsColors.InkMuted, fontSize = 13.sp)
            }
        }
    }
}
