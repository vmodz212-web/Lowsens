package com.example.dkracik

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku

private val Bg = Color(0xFF0B0F14)
private val Tile = Color(0xFF151B24)
private val TileOn = Color(0xFF0E2B2E)
private val Accent = Color(0xFF2DD4BF)
private val Line = Color(0xFF263040)
private val Muted = Color(0xFF8A97A8)
private val Txt = Color(0xFFE8EEF5)
private val PokeRed = Color(0xFFEE1515)

class AppItem(val pkg: String, val label: String, val game: Boolean)

@Suppress("DEPRECATION")
fun loadApps(ctx: Context): List<AppItem> = try {
    val pm = ctx.packageManager
    pm.getInstalledApplications(0)
        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != ctx.packageName }
        .map { a ->
            val g = a.category == ApplicationInfo.CATEGORY_GAME || (a.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            AppItem(a.packageName, a.loadLabel(pm).toString(), g)
        }
} catch (e: Throwable) {
    emptyList()
}

fun labelOf(ctx: Context, pkg: String): String = try {
    val pm = ctx.packageManager
    pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
} catch (e: Throwable) {
    pkg
}

class Feat(
    val id: String, val icon: String, val name: String, val desc: String,
    val opts: List<String> = emptyList(), val def: String = "", val unit: String = "",
    val once: Boolean = false, val needsPkg: Boolean = false,
    val gen: (String, String) -> List<String>, val undo: List<String> = emptyList()
)

fun makeFeats(hz: List<String>): List<Feat> = listOf(
    Feat("cache", "🧹", "Bersihkan cache", "Ketuk untuk menjalankan sekali", once = true,
        gen = { _, _ -> listOf("pm trim-caches 999G") }),
    Feat("hz", "🔄", "Refresh rate", "",
        opts = hz, def = hz.last(), unit = "Hz",
        gen = { v, _ -> listOf("settings put system min_refresh_rate $v.0", "settings put system peak_refresh_rate $v.0") },
        undo = listOf("settings delete system min_refresh_rate", "settings delete system peak_refresh_rate")),
    Feat("font", "🔤", "Ukuran teks", "",
        opts = listOf("0.9", "1.0", "1.1", "1.2"), def = "1.0", unit = "x",
        gen = { v, _ -> listOf("settings put system font_scale $v") }, undo = listOf("settings put system font_scale 1.0")),
    Feat("cmp", "🚀", "Percepat game ★", "Compile speed-profile", needsPkg = true,
        gen = { _, p -> listOf("cmd package compile -m speed-profile -f $p") },
        undo = listOf("cmd package compile --reset {p}")),
    Feat("gm", "🎯", "Game mode ★", "Prioritaskan performa", needsPkg = true,
        gen = { _, p -> listOf("cmd game mode performance $p") },
        undo = listOf("cmd game reset {p}")),
    Feat("gpu", "🖥️", "Driver ANGLE ★", "Pakai ANGLE untuk game", needsPkg = true,
        gen = { _, p -> listOf("settings put global angle_gl_driver_selection_pkgs $p", "settings put global angle_gl_driver_selection_values angle") },
        undo = listOf("settings delete global angle_gl_driver_selection_pkgs", "settings delete global angle_gl_driver_selection_values")),
    Feat("ph", "👻", "Phantom process", "Batas dilepas, Android 12+",
        gen = { _, _ -> listOf("device_config set_sync_disabled_for_tests persistent", "device_config put activity_manager max_phantom_processes 2147483647", "settings put global settings_enable_monitor_phantom_procs false") },
        undo = listOf("device_config delete activity_manager max_phantom_processes", "settings put global settings_enable_monitor_phantom_procs true", "device_config set_sync_disabled_for_tests none")),
    Feat("dnd", "🔕", "Jangan ganggu", "Mode prioritas, volume tetap bisa diubah",
        gen = { _, _ -> listOf("cmd notification set_dnd priority") }, undo = listOf("cmd notification set_dnd off"))
)

@Suppress("DEPRECATION")
fun detectHz(ctx: Context): List<String> = try {
    val modes = (ctx as? Activity)?.windowManager?.defaultDisplay?.supportedModes
    val l = modes?.map { Math.round(it.refreshRate) }?.distinct()?.sorted() ?: emptyList()
    if (l.isEmpty()) listOf("60") else l.map { it.toString() }
} catch (e: Throwable) {
    listOf("60")
}

class MainActivity : ComponentActivity() {
    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permListener)
        val hz = detectHz(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent, onPrimary = Bg, background = Bg, onBackground = Txt,
                    surface = Bg, onSurface = Txt, surfaceVariant = Tile,
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
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Txt, unfocusedTextColor = Txt,
    focusedBorderColor = Accent, unfocusedBorderColor = Line,
    focusedLabelColor = Accent, unfocusedLabelColor = Muted, cursorColor = Accent
)

@Composable
fun PickDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppItem>?>(null) }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { loadApps(ctx) } }
    val all = apps
    val list = (all ?: emptyList())
        .sortedWith(compareBy<AppItem>({ !it.game }, { it.label.lowercase() }))
        .filter { q.isBlank() || it.label.contains(q, true) || it.pkg.contains(q, true) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Tile,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup", color = Accent, fontWeight = FontWeight.Bold) } },
        title = { Text("Pilih game", color = Txt) },
        text = {
            Column {
                Text(
                    when {
                        all == null -> "Memuat..."
                        all.isEmpty() -> "Daftar aplikasi tidak terbaca. Ketik nama paket secara manual."
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

@Composable
fun App(hz: List<String>) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("lowsens", 0) }
    val feats = remember { makeFeats(hz) }
    var pkg by remember { mutableStateOf(prefs.getString("pkg", "") ?: "") }
    val sel = remember {
        mutableStateMapOf<String, Boolean>().apply { feats.forEach { put(it.id, prefs.getBoolean("on_" + it.id, false)) } }
    }
    val vals = remember {
        mutableStateMapOf<String, String>().apply {
            feats.forEach {
                if (it.opts.isNotEmpty()) {
                    val s = prefs.getString("v_" + it.id, null)
                    put(it.id, if (s != null && s in it.opts) s else it.def.ifEmpty { it.opts[0] })
                }
            }
        }
    }
    val used = remember {
        mutableStateMapOf<String, String>().apply { feats.forEach { put(it.id, prefs.getString("p_" + it.id, "") ?: "") } }
    }
    var pick by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun say(m: String) { Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show() }

    fun save() {
        val e = prefs.edit()
        e.putString("pkg", pkg)
        feats.forEach { f ->
            e.putBoolean("on_" + f.id, sel[f.id] == true)
            e.putString("p_" + f.id, used[f.id] ?: "")
            vals[f.id]?.let { e.putString("v_" + f.id, it) }
        }
        e.apply()
    }

    fun ready(): Boolean {
        if (!ShizukuRunner.active()) { say("Shizuku belum aktif. Buka aplikasi Shizuku dan mulai layanannya."); return false }
        if (!ShizukuRunner.granted()) { Shizuku.requestPermission(1); say("Izinkan akses Shizuku, lalu ketuk lagi."); return false }
        return true
    }

    fun exec(list: List<String>, done: () -> Unit = {}) {
        if (list.isEmpty()) { done(); return }
        scope.launch {
            var fail = ""
            withContext(Dispatchers.IO) {
                list.forEach { c ->
                    val r = ShizukuRunner.run(c)
                    if (fail.isEmpty() && (r.startsWith("Gagal") || r.contains("Exception") || r.contains("Error"))) fail = r.take(90)
                }
            }
            if (fail.isNotEmpty()) say(fail)
            done()
        }
    }

    fun pkgOf(f: Feat): String = (used[f.id] ?: "").ifEmpty { pkg.trim() }
    fun undoOf(f: Feat): List<String> = f.undo.map { it.replace("{p}", pkgOf(f).ifEmpty { "NAMA.PAKET" }) }

    fun toggle(f: Feat) {
        val on = sel[f.id] != true
        if (!ready()) return
        val p = pkg.trim()
        if (on && f.needsPkg && p.isEmpty()) { say("Pilih game dulu di kolom atas."); return }
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

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Ball(32.dp)
            Spacer(Modifier.width(10.dp))
            Text("Lowsens", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Txt)
        }
        OutlinedTextField(
            value = pkg, onValueChange = { pkg = it; save() }, singleLine = true,
            label = { Text("Nama paket game") },
            trailingIcon = { TextButton(onClick = { pick = true }) { Text("Pilih", color = Accent, fontWeight = FontWeight.Bold) } },
            colors = fieldColors(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(feats) { f ->
                val on = sel[f.id] == true
                val shape = RoundedCornerShape(14.dp)
                Column(
                    Modifier.fillMaxWidth().height(172.dp).clip(shape)
                        .background(if (on) TileOn else Tile)
                        .border(if (on) 1.5.dp else 1.dp, if (on) Accent else Line, shape)
                        .clickable { toggle(f) }
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
                                if (on) "AKTIF" else "MATI", fontSize = 10.sp,
                                fontWeight = FontWeight.Bold, color = if (on) Bg else Muted
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
                                    .background(Accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "🎮 " + labelOf(ctx, pkgOf(f)), fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold, color = Accent, maxLines = 2
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                        val rows = f.opts.chunked(2)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rows.forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { o ->
                                        val s = vals[f.id] == o
                                        val cs = RoundedCornerShape(8.dp)
                                        Box(
                                            Modifier.weight(1f).height(28.dp).clip(cs)
                                                .background(if (s) Accent else Color.Transparent)
                                                .border(1.dp, if (s) Accent else Line, cs)
                                                .clickable { choose(f, o) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                o + f.unit, fontSize = 12.sp,
                                                fontWeight = if (s) FontWeight.Bold else FontWeight.Normal,
                                                color = if (s) Bg else Txt
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
        }
    }

    if (pick) PickDialog(onPick = { pkg = it; pick = false; save() }, onDismiss = { pick = false })
}
