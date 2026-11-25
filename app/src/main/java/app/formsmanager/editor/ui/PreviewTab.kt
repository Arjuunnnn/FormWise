package app.formsmanager.editor.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.editor.FormDraft
import app.formsmanager.editor.QuestionDraft
import app.formsmanager.editor.QuestionType

@Composable
fun PreviewTab(draft: FormDraft, responderUri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = responderUri.isNotBlank(),
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(responderUri))) },
                        shape = FormsShapes.Pill,
                        colors = ButtonDefaults.buttonColors(containerColor = FormsColors.Cyan, contentColor = FormsColors.OnCyan),
                    ) {
                        Text("Open published form", fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        enabled = responderUri.isNotBlank(),
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, responderUri)
                            }
                            context.startActivity(Intent.createChooser(send, "Share form link"))
                        },
                        shape = FormsShapes.Pill,
                        colors = ButtonDefaults.buttonColors(containerColor = FormsColors.Surface, contentColor = FormsColors.Ink),
                    ) {
                        Text("Share link", fontWeight = FontWeight.ExtraBold)
                    }
                }
                Text(
                    "Preview shows your current edits. Open form shows the last saved version.",
                    color = FormsColors.InkMuted,
                )
            }
        }
        item {
            PreviewCard {
                Text(draft.title.ifBlank { "Untitled form" }, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = FormsColors.Ink)
                if (draft.description.isNotBlank()) {
                    Text(draft.description, color = FormsColors.Ink)
                }
            }
        }
        items(draft.questions, key = { it.id }) { question ->
            PreviewCard {
                Text(
                    text = question.title.ifBlank { "Untitled question" } + if (question.required) " *" else "",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FormsColors.Ink,
                )
                if (question.description.isNotBlank()) {
                    Text(question.description, color = FormsColors.InkMuted, fontSize = 14.sp)
                }
                PreviewAnswer(question)
            }
        }
    }
}

@Composable
private fun PreviewCard(content: @Composable () -> Unit) {
    Surface(shape = FormsShapes.Card, color = FormsColors.Surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun PreviewAnswer(question: QuestionDraft) {
    when (question.type) {
        QuestionType.SHORT_ANSWER, QuestionType.PARAGRAPH -> {
            val hint = if (question.type == QuestionType.PARAGRAPH) "Your long answer" else "Your answer"
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(hint, color = FormsColors.InkMuted)
                HorizontalDivider(color = FormsColors.Outline)
            }
        }
        QuestionType.DROPDOWN -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Choose", color = FormsColors.InkMuted, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = FormsColors.InkMuted)
            }
            question.options.forEach { Text(it.text, color = FormsColors.Ink) }
        }
        QuestionType.SCALE -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    for (n in question.scaleLow..question.scaleHigh) {
                        Text(n.toString(), color = FormsColors.Ink, fontWeight = FontWeight.Bold)
                    }
                }
                if (question.scaleLowLabel.isNotBlank() || question.scaleHighLabel.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(question.scaleLowLabel, color = FormsColors.InkMuted, fontSize = 13.sp)
                        Text(question.scaleHighLabel, color = FormsColors.InkMuted, fontSize = 13.sp)
                    }
                }
            }
        }
        QuestionType.DATE -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val hint = buildString {
                    append(if (question.includeYear) "Month, day, year" else "Month, day")
                    if (question.includeTime) append(", time")
                }
                Text(hint, color = FormsColors.InkMuted)
                HorizontalDivider(color = FormsColors.Outline)
            }
        }
        QuestionType.TIME -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (question.duration) "Hours, minutes, seconds" else "Hour and minute", color = FormsColors.InkMuted)
                HorizontalDivider(color = FormsColors.Outline)
            }
        }
        QuestionType.MULTIPLE_CHOICE, QuestionType.CHECKBOXES -> {
            val marker = if (question.type == QuestionType.MULTIPLE_CHOICE) {
                Icons.Outlined.RadioButtonUnchecked
            } else {
                Icons.Outlined.CheckBoxOutlineBlank
            }
            question.options.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(marker, contentDescription = null, tint = FormsColors.Ink)
                    Spacer(Modifier.width(12.dp))
                    Text(if (option.isOther) "Other:" else option.text, color = FormsColors.Ink)
                }
            }
        }
    }
}
