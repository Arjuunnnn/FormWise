package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.FormData
import app.formsmanager.editor.ui.FormsColors

/** Choose what goes into the CSV, then save it to the device or share it. */
@Composable
fun ExportScreen(
    data: FormData,
    onBack: () -> Unit,
    onSave: (includeEmail: Boolean, includeScore: Boolean) -> Unit,
    onShare: (includeEmail: Boolean, includeScore: Boolean) -> Unit,
    onLoadMore: () -> Unit,
) {
    StatusBarStyle()
    val hasEmail = remember(data.responses) { data.responses.any { it.respondentEmail != null } }
    var includeEmail by rememberSaveable { mutableStateOf(hasEmail) }
    var includeScore by rememberSaveable { mutableStateOf(data.form.isQuiz) }
    val columns = CsvExport.columns(data, includeEmail && hasEmail, includeScore && data.form.isQuiz)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas),
    ) {
        TopBar(onBack = onBack, title = "Export responses")
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WhiteCard {
                    Text(
                        "${data.responses.size} ${if (data.responses.size == 1) "response" else "responses"} · ${columns.size} columns",
                        color = FormsColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "Oldest first, as a CSV file that opens in Sheets, Excel and Numbers.",
                        color = FormsColors.InkMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (data.hasMore) {
                item {
                    WhiteCard {
                        Text("Not everything is loaded yet", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "This form has more responses than the ${data.responses.size} loaded. Load more before exporting to include them.",
                            color = FormsColors.InkMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )
                        SecondaryButton("Load more responses", onLoadMore, Modifier.fillMaxWidth(), height = 46.dp)
                    }
                }
            }
            if (hasEmail || data.form.isQuiz) {
                item {
                    WhiteCard(padding = PaddingValues(0.dp)) {
                        if (hasEmail) {
                            OptionRow("Include email addresses", includeEmail) { includeEmail = it }
                        }
                        if (data.form.isQuiz) {
                            OptionRow("Include quiz scores", includeScore) { includeScore = it }
                        }
                    }
                }
            }
            item {
                WhiteCard {
                    Text("Columns", color = FormsColors.Ink, fontWeight = FontWeight.ExtraBold)
                    columns.forEachIndexed { i, name ->
                        Text(
                            "${i + 1}. $name",
                            color = FormsColors.InkMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
            item {
                Text(
                    "Answers that start with =, +, - or @ get a leading apostrophe so a spreadsheet can't run them as formulas.",
                    color = FormsColors.InkMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
        BottomActionBar {
            SecondaryButton(
                "Share",
                { onShare(includeEmail && hasEmail, includeScore && data.form.isQuiz) },
                Modifier.weight(1f),
            )
            PrimaryButton(
                "Save to device",
                { onSave(includeEmail && hasEmail, includeScore && data.form.isQuiz) },
                Modifier.weight(1.4f),
                enabled = data.responses.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun OptionRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = FormsColors.Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = FormsColors.Cyan,
                checkedThumbColor = FormsColors.OnCyan,
                checkedBorderColor = FormsColors.Cyan,
                uncheckedTrackColor = FormsColors.Track,
                uncheckedThumbColor = FormsColors.Ink,
                uncheckedBorderColor = FormsColors.Track,
            ),
        )
    }
}
