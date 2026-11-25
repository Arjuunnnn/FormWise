package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.api.FormTemplate
import app.formsmanager.api.FormTemplates
import app.formsmanager.editor.ui.FormsColors
import app.formsmanager.editor.ui.PlainField

private fun FormTemplate.icon(): ImageVector = when (id) {
    "event" -> Icons.Outlined.CalendarMonth
    "feedback" -> Icons.Outlined.BarChart
    "quiz" -> Icons.Outlined.Quiz
    else -> Icons.Outlined.Add
}

@Composable
fun NewFormScreen(onBack: () -> Unit, onCreate: (String, FormTemplate) -> Unit) {
    StatusBarStyle()
    var name by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf(FormTemplates.first().id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas)
            .imePadding(),
    ) {
        TopBar(onBack = onBack, title = "New form")

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Name your form",
                    color = FormsColors.Ink,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            item {
                val shape = RoundedCornerShape(18.dp)
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, bottom = 14.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(shape)
                        .background(FormsColors.Surface)
                        .border(2.dp, if (name.isBlank()) FormsColors.Hairline else FormsColors.Cyan, shape)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    PlainField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "For example, Workshop sign-up",
                        textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FormsColors.Ink),
                    )
                }
                Text(
                    "Start from",
                    color = FormsColors.Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            items(FormTemplates, key = { it.id }) { template ->
                val on = template.id == selectedId
                val shape = RoundedCornerShape(20.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(if (on) FormsColors.Tint else FormsColors.Surface)
                        .border(if (on) 2.dp else 1.dp, if (on) FormsColors.Cyan else FormsColors.Hairline, shape)
                        .clickable { selectedId = template.id }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IconTile(
                        icon = template.icon(),
                        container = if (on) FormsColors.Surface else FormsColors.Tint,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(template.name, color = FormsColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(template.subtitle, color = FormsColors.InkMuted, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (on) FormsColors.Cyan else Color.Transparent)
                            .border(2.dp, if (on) FormsColors.Cyan else FormsColors.Hairline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = FormsColors.Ink, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        BottomActionBar {
            PrimaryButton(
                text = "Create form",
                onClick = { onCreate(name.trim(), FormTemplates.first { it.id == selectedId }) },
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
