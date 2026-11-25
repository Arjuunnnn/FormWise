package app.formsmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.formsmanager.editor.ui.FormsColors

@Composable
fun SignInScreen(message: String?, onSignIn: () -> Unit) {
    StatusBarStyle(darkIcons = false)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FormsColors.Navy),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = Gutter, end = Gutter, top = 24.dp, bottom = 190.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormwiseMark(size = 40.dp, outlined = true)
                Text("Formwise", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(20.dp))
            StackedCards()
            Text(
                "Your forms and their responses, in one place",
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Sign in with Google to see the forms you own or can edit.",
                color = FormsColors.MutedOnDark,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )
            Feature(Icons.Outlined.BarChart, "Watch responses arrive and see how they trend")
            Feature(Icons.Outlined.EditNote, "Edit questions and options from your phone")
            Feature(Icons.Outlined.QrCode2, "Share with a link or a QR code")
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(FormsColors.Navy)
                .navigationBarsPadding()
                .padding(start = Gutter, end = Gutter, top = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (message != null) {
                Text(
                    message,
                    color = FormsColors.Cyan,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Button(
                onClick = onSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = FormsColors.OnCyan),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(FormsColors.Navy),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("G", color = FormsColors.Cyan, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(12.dp))
                Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(
                "Formwise never changes your forms without your say",
                color = FormsColors.MutedOnDark,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(FormsColors.OnDarkSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = FormsColors.Cyan)
        }
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StackedCards() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(40.dp, 36.dp)
                .size(230.dp, 150.dp)
                .rotate(-9f)
                .clip(RoundedCornerShape(28.dp))
                .background(FormsColors.Cyan),
        )
        Box(
            modifier = Modifier
                .offset(70.dp, 24.dp)
                .size(230.dp, 150.dp)
                .rotate(5f)
                .clip(RoundedCornerShape(28.dp))
                .background(FormsColors.Ice),
        )
        Column(
            modifier = Modifier
                .offset(54.dp, 50.dp)
                .size(240.dp, 170.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(FormsColors.Surface)
                .padding(18.dp),
        ) {
            StatusChip(accepting = true, small = true)
            Text(
                "Hackathon registration",
                color = FormsColors.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
            )
            MiniBars(
                values = listOf(14, 22, 18, 30, 26, 36, 50),
                color = FormsColors.CyanDeep,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                emphasizeLast = true,
            )
        }
    }
}
