package app.formsmanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.editor.ui.FormsColors
import kotlinx.coroutines.launch

private class IntroPage(val icon: ImageVector, val title: String, val body: String)

private val IntroPages = listOf(
    IntroPage(
        Icons.Outlined.BarChart,
        "See responses as they arrive",
        "Check today's numbers at a glance and read every answer without opening a spreadsheet.",
    ),
    IntroPage(
        Icons.Outlined.EditNote,
        "Edit forms from your phone",
        "Add, reorder and change questions, then save straight back to Google Forms.",
    ),
    IntroPage(
        Icons.Outlined.QrCode2,
        "Share in a tap",
        "Copy the link, show a QR code and invite people to help run the form.",
    ),
)

/** Three-page intro shown once, before the first sign-in. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    StatusBarStyle(darkIcons = false)
    val pager = rememberPagerState(pageCount = { IntroPages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == IntroPages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Navy),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = Gutter),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!last) {
                Text(
                    "Skip",
                    color = FormsColors.MutedOnDark,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDone)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = IntroPages[index]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .clip(CircleShape)
                        .background(FormsColors.OnDarkSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(page.icon, contentDescription = null, tint = FormsColors.Cyan, modifier = Modifier.size(76.dp))
                }
                Text(
                    page.title,
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 40.dp),
                )
                Text(
                    page.body,
                    color = FormsColors.MutedOnDark,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = Gutter, end = Gutter, top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                IntroPages.indices.forEach { i ->
                    val on = i == pager.currentPage
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (on) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (on) FormsColors.Cyan else FormsColors.OnDarkSoft),
                    )
                }
            }
            PrimaryButton(
                text = if (last) "Get started" else "Next",
                onClick = {
                    if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
            )
        }
    }
}
