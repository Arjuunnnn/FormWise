package app.formsmanager.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.formsmanager.editor.QuestionType

/** Flat bottom bar with a hairline on top: a short label, then one icon per question type. */
@Composable
fun QuestionToolbar(onAdd: (QuestionType) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = FormsColors.Surface,
    ) {
        Column {
            HorizontalDivider(color = FormsColors.Hairline)
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(60.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Add", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
                QuestionType.entries.forEach { type ->
                    IconButton(
                        onClick = { onAdd(type) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(FormsColors.Tint, CircleShape),
                    ) {
                        Icon(type.icon(), contentDescription = "Add ${type.label().lowercase()} question", tint = FormsColors.CyanDeep)
                    }
                }
            }
        }
    }
}
