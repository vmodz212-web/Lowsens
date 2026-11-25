package com.example.dkracik

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rikka.shizuku.Shizuku

@Composable
fun FeatTile(ctl: Ctl, f: Feat) {
    val on = ctl.sel[f.id] == true
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().height(172.dp).clip(shape)
            .background(if (on) CardHi else CardBg)
            .border(if (on) 1.5.dp else 1.dp, if (on) Accent else Line, shape)
            .clickable { ctl.toggle(f) }
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(f.icon, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (on) Accent else Line)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    if (on) "AKTIF" else "MATI", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = if (on) Color.White else Muted
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(f.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Txt, maxLines = 2)
        if (f.opts.isEmpty()) {
            Text(f.desc, fontSize = 11.sp, color = Muted, maxLines = 2)
            if (on && f.needsPkg) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Accent.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        "🎮 " + labelOf(ctl.ctx, ctl.pkgOf(f)), fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, color = Txt, maxLines = 2
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                f.opts.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { o ->
                            val s = ctl.vals[f.id] == o
                            val cs = RoundedCornerShape(8.dp)
                            Box(
                                Modifier.weight(1f).height(28.dp).clip(cs)
                                    .background(if (s) Accent else Color.Transparent)
                                    .border(1.dp, if (s) Accent else Line, cs)
                                    .clickable { ctl.choose(f, o) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    o + f.unit, fontSize = 12.sp,
                                    fontWeight = if (s) FontWeight.Bold else FontWeight.Normal,
                                    color = if (s) Color.White else Txt
                                )
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureTab(ctl: Ctl, tab: Int) {
    val title = when (tab) { 1 -> "Layar"; 2 -> "Performa"; else -> "Game" }
    val list = ctl.feats.filter { it.tab == tab }
    var pick by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Txt,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        if (tab == 3) {
            OutlinedTextField(
                value = ctl.pkg, onValueChange = { ctl.setPkg(it) }, singleLine = true,
                label = { Text("Nama paket game") },
                trailingIcon = { TextButton(onClick = { pick = true }) { Text("Pilih", color = Gold, fontWeight = FontWeight.Bold) } },
                colors = fieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(list) { f -> FeatTile(ctl, f) }
        }
    }
    if (pick) PickDialog(ctl, onPick = { ctl.setPkg(it); pick = false }, onDismiss = { pick = false })
}

@Composable
fun SettingsTab(ctl: Ctl) {
    var tick by remember { mutableStateOf(0) }
    val st = remember(tick) { if (!ShizukuRunner.active()) 0 else if (!ShizukuRunner.granted()) 1 else 2 }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Setelan", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Txt)
        Panel {
            Text("Status Shizuku", fontWeight = FontWeight.SemiBold, color = Txt)
            Spacer(Modifier.height(4.dp))
            Text(
                when (st) {
                    0 -> "Belum aktif. Buka aplikasi Shizuku dan mulai layanannya."
                    1 -> "Aktif, tapi izin belum diberikan."
                    else -> "Siap dipakai."
                },
                fontSize = 13.sp, color = if (st == 2) Gold else Muted
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { tick++ }, border = BorderStroke(1.dp, Line),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Txt)
                ) { Text("Periksa lagi") }
                Button(
                    onClick = { Shizuku.requestPermission(1); tick++ }, enabled = st == 1,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
                ) { Text("Minta izin") }
            }
        }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pesan saat fitur diubah", fontWeight = FontWeight.SemiBold, color = Txt)
                    Text("Contoh: Percepat game diaktifkan", fontSize = 12.sp, color = Muted)
                }
                Switch(
                    checked = ctl.showMsg, onCheckedChange = { ctl.setMsg(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = Accent,
                        uncheckedThumbColor = Muted, uncheckedTrackColor = CardBg, uncheckedBorderColor = Line
                    )
                )
            }
        }
        Panel {
            Text("Tentang", fontWeight = FontWeight.SemiBold, color = Txt)
            Spacer(Modifier.height(4.dp))
            Text(
                "Lowsens 1.0. Aplikasi ini hanya mengubah pengaturan sistem Android lewat Shizuku. Tidak mengubah file atau data game.",
                fontSize = 12.sp, color = Muted
            )
        }
    }
}

@Composable
fun PickDialog(ctl: Ctl, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val all = ctl.apps
    val list = (all ?: emptyList())
        .sortedWith(compareBy<AppItem>({ !it.game }, { it.label.lowercase() }))
        .filter { q.isBlank() || it.label.contains(q, true) || it.pkg.contains(q, true) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardBg,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup", color = Gold, fontWeight = FontWeight.Bold) } },
        title = { Text("Pilih game", color = Txt) },
        text = {
            Column {
                Text(
                    when {
                        all == null -> "Memuat..."
                        all.isEmpty() -> "Daftar aplikasi tidak terbaca."
                        else -> "Aplikasi terpasang di HP (game di paling atas)"
                    }, fontSize = 12.sp, color = Muted
                )
                OutlinedTextField(
                    value = q, onValueChange = { q = it }, singleLine = true,
                    label = { Text("Cari") }, colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(list) { a ->
                        Column(Modifier.fillMaxWidth().clickable { onPick(a.pkg) }.padding(vertical = 8.dp)) {
                            Text(a.label + if (a.game) "  🎮" else "", fontWeight = FontWeight.SemiBold, color = Txt)
                            Text(a.pkg, fontSize = 11.sp, color = Muted)
                        }
                    }
                }
            }
        }
    )
}
