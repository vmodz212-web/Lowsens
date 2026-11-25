package com.example.dkracik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permListener)
        val hz = detectHz(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent, onPrimary = Color.White, background = Bg, onBackground = Txt,
                    surface = Bg, onSurface = Txt, surfaceVariant = CardBg,
                    onSurfaceVariant = Muted, outline = Line, outlineVariant = Line
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = Bg) { App(hz) }
            }
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        super.onDestroy()
    }
}

@Composable
fun ResultBar(ctl: Ctl) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(shape).background(CardBg).border(1.dp, Line, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Accent).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text("📌 HASIL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(ctl.result, fontSize = 12.sp, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun App(hz: List<String>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ctl = remember { Ctl(ctx, scope, hz) }
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("🏠" to "Beranda", "📱" to "Layar", "⚡" to "Performa", "🎮" to "Game", "⚙️" to "Setelan")
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        ResultBar(ctl)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> HomeTab(ctl)
                1, 2, 3 -> FeatureTab(ctl, tab)
                else -> SettingsTab(ctl)
            }
        }
        NavigationBar(containerColor = CardBg, windowInsets = WindowInsets(0, 0, 0, 0)) {
            tabs.forEachIndexed { i, t ->
                NavigationBarItem(
                    selected = tab == i, onClick = { tab = i },
                    icon = { Text(t.first, fontSize = 20.sp) },
                    label = { Text(t.second, fontSize = 11.sp, maxLines = 1) },
                    colors = navColors()
                )
            }
        }
    }
}
