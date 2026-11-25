package app.formsmanager.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class EditorTab(val label: String) {
    Questions("Questions"),
    Preview("Preview"),
    Responses("Responses"),
}

/**
 * Tab / filter pills from the design.
 * onDark = true: inside the navy header (selected = cyan pill, others transparent).
 * onDark = false: on the light page (selected = navy pill with cyan text, others white).
 */
@Composable
fun PillSelector(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(if (onDark) 6.dp else 8.dp)) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val background = when {
                onDark && selected -> FormsColors.Cyan
                onDark -> Color.Transparent
                selected -> FormsColors.Navy
                else -> Color.White
            }
            val textColor = when {
                onDark && selected -> FormsColors.Navy
                onDark -> FormsColors.Ice
                selected -> FormsColors.Cyan
                else -> FormsColors.Navy
            }
            Box(
                modifier = Modifier
                    .height(if (onDark) 38.dp else 36.dp)
                    .clip(FormsShapes.Pill)
                    .background(background)
                    .clickable { onSelect(index) }
                    .padding(horizontal = if (onDark) 18.dp else 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                )
            }
        }
    }
}
