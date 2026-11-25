package com.example.dkracik

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameCard(ctl: Ctl, g: AppItem, idx: Int, total: Int) {
    val icon = remember(g.pkg) { iconOf(ctl.ctx, g.pkg) }
    val shape = RoundedCornerShape(24.dp)
    val modeLabel = if (ctl.mode == "performance") "Performance" else "Standard"
    Column(
        Modifier.width(290.dp).clip(shape).background(CardHi)
            .border(1.dp, Gold.copy(alpha = 0.6f), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Image(
                    bitmap = icon, contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    g.label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Txt,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(g.pkg, fontSize = 11.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("%02d/%02d".format(idx + 1, total), fontSize = 11.sp, color = Muted)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("⚡ $modeLabel", fontSize = 12.sp, color = Txt, modifier = Modifier.weight(1f))
            Text("Skala", fontSize = 12.sp, color = Muted)
            Box(
                Modifier.width(64.dp).height(34.dp).clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Gold, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = ctl.scaleOf(g.pkg),
                    onValueChange = { ctl.setScale(g.pkg, it.replace(',', '.')) },
                    singleLine = true,
                    textStyle = TextStyle(color = Txt, fontSize = 14.sp, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(Gold),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
        Button(
            onClick = { ctl.startGame(g) }, enabled = !ctl.busy,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("▶  Mulai Game", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun HomeTab(ctl: Ctl) {
    var pick by remember { mutableStateOf(false) }
    val games = ctl.games()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ball(30.dp)
            Spacer(Modifier.width(10.dp))
            Text("Lowsens", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Txt)
        }
        Panel {
            Text("🎮  Mulai Game", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Txt)
            Spacer(Modifier.height(10.dp))
            Seg(listOf("performance" to "⚡ Performance", "standard" to "🎯 Standard"), ctl.mode) {
                ctl.mode = it
                ctl.save()
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Mulai Game = Game Mode + downscale, lalu game dibuka. Butuh Shizuku.",
                fontSize = 12.sp, color = Muted
            )
        }
        if (ctl.apps == null) {
            Text("Memuat daftar game...", color = Muted, fontSize = 13.sp)
        } else if (games.isEmpty()) {
            Text("Belum ada game terdeteksi. Tekan Tambah game di bawah.", color = Muted, fontSize = 13.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(games) { i, g -> GameCard(ctl, g, i, games.size) }
            }
        }
        Text(
            "Skala = downscale khusus game itu. Isi 1.0 untuk tanpa downscale, atau 0.7 misalnya. Downscale butuh Android 13 ke atas.",
            fontSize = 12.sp, color = Muted
        )
        OutlinedButton(
            onClick = { pick = true },
            border = BorderStroke(1.dp, Line),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("＋  Tambah game", color = Txt) }
    }
    if (pick) PickDialog(ctl, onPick = { ctl.addGame(it); pick = false }, onDismiss = { pick = false })
}
