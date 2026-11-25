package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.editor.ui.FormsColors

private class Faq(val question: String, val answer: String)

private val Faqs = listOf(
    Faq(
        "Why don't I see all my responses?",
        "Formwise loads up to 500 responses per form. If a form has more, a note tells you. Open the linked spreadsheet in Google Sheets to see everything.",
    ),
    Faq(
        "How do I link a spreadsheet?",
        "In Google Forms, open the Responses tab and choose Link to Sheets. Once linked, Open in Sheets works from every form and response screen.",
    ),
    Faq(
        "Some of my questions are missing in the editor",
        "The editor handles text and choice questions. Other items, such as sections, grids and dates, stay in your form untouched, and the editor tells you how many it can't show.",
    ),
    Faq(
        "What if someone else edits the form at the same time?",
        "If the form changed on Google's side while you were editing, saving stops and offers to reload the latest version so nothing is overwritten by accident.",
    ),
    Faq(
        "Why isn't the short link a forms.gle link?",
        "Google makes forms.gle links only inside the Forms editor, not in the API Formwise uses. So Copy short link asks the free is.gd service for a short version of your form's public link. To get a forms.gle link, open the form in Google Forms, choose Send, pick the link tab and tick Shorten URL.",
    ),
    Faq(
        "Can I stop collecting responses?",
        "Yes. Open the form, tap the settings icon and turn off Accepting responses. You can also do it from the ⋮ menu on the All forms list.",
    ),
)

@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onFeedback: () -> Unit,
    onOpenGoogleForms: () -> Unit,
    onPrivacy: () -> Unit,
) {
    StatusBarStyle()
    val version = LocalContext.current.let { remember { it.appVersion() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Canvas),
    ) {
        TopBar(onBack = onBack, title = "Help & about")
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WhiteCard(padding = PaddingValues(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        FormwiseMark(size = 56.dp)
                        Column {
                            Text("Formwise", color = FormsColors.Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (version.isNotBlank()) "Version $version" else "Open-source Google Forms manager",
                                color = FormsColors.InkMuted,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    Text(
                        "An open-source app for managing your Google Forms from your phone. It runs on your device and talks straight to Google.",
                        color = FormsColors.InkMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }

            item { SectionLabel("Common questions") }
            item {
                WhiteCard(padding = PaddingValues(0.dp)) {
                    Faqs.forEachIndexed { i, faq ->
                        if (i > 0) HorizontalDivider(color = FormsColors.Hairline)
                        FaqRow(faq)
                    }
                }
            }

            item { SectionLabel("Get in touch") }
            item {
                WhiteCard(padding = PaddingValues(0.dp)) {
                    LinkRow(Icons.Outlined.MailOutline, "Send feedback", onFeedback, "Opens your email app")
                    HorizontalDivider(color = FormsColors.Hairline)
                    LinkRow(Icons.AutoMirrored.Outlined.OpenInNew, "Open Google Forms", onOpenGoogleForms, "For settings Formwise doesn't cover")
                }
            }

            item { SectionLabel("About") }
            item {
                WhiteCard(padding = PaddingValues(0.dp)) {
                    LinkRow(Icons.Outlined.Shield, "Privacy & permissions", onPrivacy, "What Formwise can access")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = FormsColors.InkMuted,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun FaqRow(faq: Faq) {
    var open by rememberSaveable(faq.question) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = !open }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                faq.question,
                color = FormsColors.Ink,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = if (open) "Collapse" else "Expand",
                tint = FormsColors.InkMuted,
                modifier = Modifier.rotate(if (open) 180f else 0f),
            )
        }
        if (open) {
            Text(
                faq.answer,
                color = FormsColors.InkMuted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
