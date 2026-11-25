package app.formsmanager.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardShape = FormsShapes.Card

@Composable
fun FormHeaderCard(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(FormsColors.Surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(FormsColors.Cyan),
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlainField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = "Form title",
                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = FormsColors.Ink),
            )
            PlainField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = "Form description",
                textStyle = TextStyle(fontSize = 15.sp, color = FormsColors.Ink),
            )
        }
    }
}
