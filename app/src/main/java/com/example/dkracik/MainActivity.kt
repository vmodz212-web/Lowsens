package com.example.dkracik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku

private val Navy = Color(0xFF0E1330)
private val CardBg = Color(0xFF1A2248)
private val CardOn = Color(0xFF263172)
private val Yellow = Color(0xFFFFCB05)
private val PokeRed = Color(0xFFEE1515)
private val Muted = Color(0xFF9FAAD8)
private val Line = Color(0xFF39458F)

val KNOWN = mapOf(
    "com.dts.freefireth" to "Free Fire",
    "com.dts.freefiremax" to "Free Fire MAX",
    "com.tencent.ig" to "PUBG Mobile",
    "com.pubg.imobile" to "BGMI",
    "com.pubg.krmobile" to "PUBG Mobile KR",
    "com.mobile.legends" to "Mobile Legends",
    "com.miHoYo.GenshinImpact" to "Genshin Impact",
    "com.HoYoverse.hkrpgoversea" to "Honkai Star Rail",
    "com.activision.callofduty.shooter" to "Call of Duty Mobile",
    "com.mojang.minecraftpe" to "Minecraft",
    "com.roblox.client" to "Roblox",
    "com.riotgames.league.wildrift" to "Wild Rift",
    "com.supercell.clashofclans" to "Clash of Clans",
    "com.supercell.brawlstars" to "Brawl Stars",
    "jp.konami.pesam" to "eFootball"
)

class Feat(
    val id: String, val icon: String, val name: String, val desc: String,
    val opts: List<String> = emptyList(), val def: String = "", val unit: String = "",
    val once: Boolean = false, val needsPkg: Boolean = false,
    val gen: (String, String) -> List<String>, val undo: List<String> = emptyList()
)

val FEATS = listOf(
    Feat("cache", "🧹", "Bersihkan cache", "Ketuk untuk menjalankan sekali", once = true,
        gen = { _, _ -> listOf("pm trim-caches 999G") }),
    Feat("anim", "⚡", "Animasi cepat", "Buka tutup aplikasi lebih gesit",
        opts = listOf("0.5", "0"), unit = "x",
        gen = { v, _ -> listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale").map { "settings put global $it $v" } },
        undo = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale").map { "settings put global $it 1" }),
    Feat("hz", "🔄", "Refresh rate", "Sesuaikan dengan layar",
        opts = listOf("60", "90", "120", "144"), def = "120", unit = "Hz",
        gen = { v, _ -> listOf("settings put system min_refresh_rate $v", "settings put system peak_refresh_rate $v") },
        undo = listOf("settings delete system min_refresh_rate", "settings delete system peak_refresh_rate")),
    Feat("dpi", "📐", "DPI layar", "Makin kecil, layar makin luas",
        opts = listOf("320", "360", "420", "480"), def = "420",
        gen = { v, _ -> listOf("wm density $v") }, undo = listOf("wm density reset")),
    Feat("font", "🔤", "Ukuran teks", "1.0 adalah bawaan",
        opts = listOf("0.9", "1.0", "1.1", "1.2"), def = "1.0", unit = "x",
        gen = { v, _ -> listOf("settings put system font_scale $v") }, undo = listOf("settings put system font_scale 1.0")),
    Feat("cmp", "🚀", "Percepat game ★", "Compile speed-profile", needsPkg = true,
        gen = { _, p -> listOf("cmd package compile -m speed-profile -f $p") },
        undo = listOf("cmd package compile --reset {p}")),
    Feat("gm", "🎯", "Game mode performa ★", "Prioritaskan performa game", needsPkg = true,
        gen = { _, p -> listOf("cmd game mode performance $p") },
        undo = listOf("cmd game reset {p}")),
    Feat("gpu", "🖥️", "Driver ANGLE ★", "Pakai ANGLE untuk game", needsPkg = true,
        gen = { _, p -> listOf("settings put global angle_gl_driver_selection_pkgs $p", "settings put global angle_gl_driver_selection_values angle") },
        undo = listOf("settings delete global angle_gl_driver_selection_pkgs", "settings delete global angle_gl_driver_selection_values")),
    Feat("ph", "👻", "Batas phantom process", "Dilepas, Android 12 ke atas",
        gen = { _, _ -> listOf("device_config set_sync_disabled_for_tests persistent", "device_config put activity_manager max_phantom_processes 2147483647", "settings put global settings_enable_monitor_phantom_procs false") },
        undo = listOf("device_config delete activity_manager max_phantom_processes", "settings put global settings_enable_monitor_phantom_procs true", "device_config set_sync_disabled_for_tests none")),
    Feat("dnd", "🔕", "Jangan ganggu", "Nyalakan DND saat main",
        gen = { _, _ -> listOf("cmd notification set_dnd on") }, undo = listOf("cmd notification set_dnd off"))
)

class MainActivity : ComponentActivity() {
    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permListener)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Yellow, onPrimary = Navy, background = Navy, onBackground = Color.White,
                    surface = Navy, onSurface = Color.White, surfaceVariant = CardBg,
                    onSurfaceVariant = Muted, outline = Line, outlineVariant = Line
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = Navy) { App() }
            }
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        super.onDestroy()
    }
}

@Composable
fun Ball(sz: Dp) {
    Canvas(Modifier.size(sz)) {
        val r = size.minDimension / 2f
        drawArc(PokeRed, 180f, 180f, true)
        drawArc(Color.White, 0f, 180f, true)
        drawCircle(Color.Black, r, style = Stroke(width = r * 0.12f))
        drawLine(Color.Black, Offset(0f, r), Offset(size.width, r), strokeWidth = r * 0.14f)
        drawCircle(Color.Black, r * 0.3f)
        drawCircle(Color.White, r * 0.17f)
    }
}

@Composable
fun PickDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var installed by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(Unit) {
        installed = withContext(Dispatchers.IO) {
            if (ShizukuRunner.active() && ShizukuRunner.granted())
                ShizukuRunner.run("pm list packages -3").lines()
                    .filter { it.startsWith("package:") }.map { it.removePrefix("package:").trim() }
            else emptyList()
        }
    }
    val base = installed?.takeIf { it.isNotEmpty() } ?: KNOWN.keys.toList()
    val list = base.sortedWith(compareBy<String>({ !KNOWN.containsKey(it) }, { (KNOWN[it] ?: it).lowercase() }))
        .filter { q.isBlank() || it.contains(q, true) || (KNOWN[it] ?: "").contains(q, true) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardBg,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup", color = Yellow) } },
        title = { Text("Pilih game", color = Color.White) },
        text = {
            Column {
                Text(
                    when {
                        installed == null -> "Memuat..."
                        installed.isNullOrEmpty() -> "Daftar game umum (aktifkan Shizuku untuk melihat aplikasi terpasang)"
                        else -> "Aplikasi terpasang di HP"
                    }, fontSize = 12.sp, color = Muted
                )
                OutlinedTextField(
                    value = q, onValueChange = { q = it }, singleLine = true,
                    label = { Text("Cari") }, modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(list) { p ->
                        Column(Modifier.fillMaxWidth().clickable { onPick(p) }.padding(vertical = 8.dp)) {
                            Text(KNOWN[p] ?: p, fontWeight = FontWeight.SemiBold, color = Color.White)
                            if (KNOWN.containsKey(p)) Text(p, fontSize = 11.sp, color = Muted)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("lowsens", 0) }
    var pkg by remember { mutableStateOf(prefs.getString("pkg", "") ?: "") }
    val sel = remember {
        mutableStateMapOf<String, Boolean>().apply { FEATS.forEach { put(it.id, prefs.getBoolean("on_" + it.id, false)) } }
    }
    val vals = remember {
        mutableStateMapOf<String, String>().apply {
            FEATS.forEach { if (it.opts.isNotEmpty()) put(it.id, prefs.getString("v_" + it.id, null) ?: it.def.ifEmpty { it.opts[0] }) }
        }
    }
    val used = remember {
        mutableStateMapOf<String, String>().apply { FEATS.forEach { put(it.id, prefs.getString("p_" + it.id, "") ?: "") } }
    }
    var log by remember { mutableStateOf("") }
    var pick by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun save() {
        val e = prefs.edit()
        e.putString("pkg", pkg)
        FEATS.forEach { f ->
            e.putBoolean("on_" + f.id, sel[f.id] == true)
            e.putString("p_" + f.id, used[f.id] ?: "")
            vals[f.id]?.let { e.putString("v_" + f.id, it) }
        }
        e.apply()
    }

    fun ready(): Boolean {
        if (!ShizukuRunner.active()) { log = "Shizuku belum aktif. Buka aplikasi Shizuku dan mulai layanannya."; return false }
        if (!ShizukuRunner.granted()) { Shizuku.requestPermission(1); log = "Izinkan akses Shizuku, lalu ketuk lagi."; return false }
        return true
    }

    fun exec(list: List<String>, done: () -> Unit = {}) {
        if (list.isEmpty()) { done(); return }
        scope.launch {
            val sb = StringBuilder()
            withContext(Dispatchers.IO) {
                list.forEach { c -> sb.append("> ").append(c).append("\n").append(ShizukuRunner.run(c)).append("\n") }
            }
            log = sb.toString(); done()
        }
    }

    fun pkgOf(f: Feat): String = (used[f.id] ?: "").ifEmpty { pkg.trim() }
    fun undoOf(f: Feat): List<String> = f.undo.map { it.replace("{p}", pkgOf(f).ifEmpty { "NAMA.PAKET" }) }

    fun toggle(f: Feat, on: Boolean) {
        if (!ready()) return
        val p = pkg.trim()
        if (on && f.needsPkg && p.isEmpty()) { log = "Pilih game dulu di kolom atas."; return }
        if (on) {
            sel[f.id] = true; used[f.id] = p
            exec(f.gen(vals[f.id] ?: "", p)) { if (f.once) sel[f.id] = false; save() }
        } else {
            sel[f.id] = false
            exec(undoOf(f)) { save() }
        }
        save()
    }

    fun choose(f: Feat, o: String) {
        vals[f.id] = o
        if (sel[f.id] == true && ready()) exec(f.gen(o, pkgOf(f))) { save() } else save()
    }

    fun offAll() {
        val on = FEATS.filter { sel[it.id] == true }
        if (on.isEmpty()) { log = "Tidak ada yang aktif."; return }
        if (!ready()) return
        val list = on.flatMap { undoOf(it) }
        FEATS.forEach { sel[it.id] = false }
        exec(list) { save() }
        save()
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Ball(34.dp)
            Spacer(Modifier.width(10.dp))
            Text("Lowsens", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Yellow)
        }
        OutlinedTextField(
            value = pkg, onValueChange = { pkg = it; save() }, singleLine = true,
            label = { Text("Nama paket game") },
            trailingIcon = { TextButton(onClick = { pick = true }) { Text("Pilih", color = Yellow, fontWeight = FontWeight.Bold) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Yellow, focusedLabelColor = Yellow, cursorColor = Yellow,
                unfocusedBorderColor = Line
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(FEATS) { f ->
                val on = sel[f.id] == true
                val shape = RoundedCornerShape(18.dp)
                Column(
                    Modifier.fillMaxWidth().clip(shape)
                        .background(if (on) CardOn else CardBg)
                        .border(if (on) 2.dp else 1.dp, if (on) Yellow else Line, shape)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.icon, fontSize = 26.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text(f.desc, fontSize = 12.sp, color = Muted)
                        }
                        Switch(
                            checked = on, onCheckedChange = { toggle(f, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Navy, checkedTrackColor = Yellow,
                                uncheckedThumbColor = Muted, uncheckedTrackColor = Navy, uncheckedBorderColor = Line
                            )
                        )
                    }
                    if (f.opts.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            f.opts.forEach { o ->
                                val s = vals[f.id] == o
                                val cs = RoundedCornerShape(10.dp)
                                Box(
                                    Modifier.weight(1f).height(36.dp).clip(cs)
                                        .background(if (s) Yellow else Color.Transparent)
                                        .border(1.dp, if (s) Yellow else Line, cs)
                                        .clickable { choose(f, o) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        o + f.unit, fontSize = 13.sp,
                                        fontWeight = if (s) FontWeight.Bold else FontWeight.Normal,
                                        color = if (s) Navy else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (log.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                log, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Muted,
                modifier = Modifier.fillMaxWidth().heightIn(max = 90.dp).verticalScroll(rememberScrollState())
            )
        }
        Button(
            onClick = { offAll() },
            colors = ButtonDefaults.buttonColors(containerColor = PokeRed, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).height(48.dp)
        ) { Text("Matikan semua", fontWeight = FontWeight.Bold) }
    }

    if (pick) PickDialog(onPick = { pkg = it; pick = false; save() }, onDismiss = { pick = false })
}
