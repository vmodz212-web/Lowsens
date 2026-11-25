package com.example.dkracik

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class Feat(
    val id: String, val icon: String, val name: String, val desc: String,
    val opts: List<String> = emptyList(), val def: String = "", val unit: String = "",
    val gen: (String, String) -> List<String>, val undo: List<String> = emptyList()
)

val FEATS = listOf(
    Feat("cache", "🧹", "Bersihkan cache", "Hapus cache semua aplikasi",
        gen = { _, _ -> listOf("pm trim-caches 999G") }),
    Feat("anim", "⚡", "Animasi cepat", "Buka tutup aplikasi lebih gesit",
        opts = listOf("0.5", "0"), unit = "x",
        gen = { v, _ -> listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale").map { "settings put global $it $v" } },
        undo = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale").map { "settings put global $it 1" }),
    Feat("hz", "🔄", "Refresh rate", "Sesuaikan dengan layar",
        opts = listOf("60", "90", "120", "144"), def = "120", unit = " Hz",
        gen = { v, _ -> listOf("settings put system min_refresh_rate $v", "settings put system peak_refresh_rate $v") },
        undo = listOf("settings delete system min_refresh_rate", "settings delete system peak_refresh_rate")),
    Feat("dpi", "📐", "DPI layar", "Makin kecil, layar makin luas",
        opts = listOf("320", "360", "420", "480"), def = "420",
        gen = { v, _ -> listOf("wm density $v") }, undo = listOf("wm density reset")),
    Feat("font", "🔤", "Ukuran teks", "1.0 adalah bawaan",
        opts = listOf("0.9", "1.0", "1.1", "1.2"), def = "1.0", unit = "x",
        gen = { v, _ -> listOf("settings put system font_scale $v") }, undo = listOf("settings put system font_scale 1.0")),
    Feat("cmp", "🚀", "Percepat aplikasi ★", "Compile speed-profile",
        gen = { _, p -> listOf("cmd package compile -m speed-profile -f " + p.ifEmpty { "-a" }) },
        undo = listOf("cmd package compile --reset {p}")),
    Feat("gm", "🎯", "Game mode performa ★", "Prioritaskan performa game",
        gen = { _, p -> listOf("cmd game mode performance " + p.ifEmpty { "NAMA.PAKET" }) },
        undo = listOf("cmd game reset {p}")),
    Feat("gpu", "🖥️", "Driver ANGLE ★", "Pakai ANGLE untuk game",
        gen = { _, p -> listOf("settings put global angle_gl_driver_selection_pkgs " + p.ifEmpty { "NAMA.PAKET" }, "settings put global angle_gl_driver_selection_values angle") },
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
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { App() }
            }
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        super.onDestroy()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App() {
    var pkg by remember { mutableStateOf("") }
    val sel = remember { mutableStateMapOf<String, Boolean>() }
    val vals = remember {
        mutableStateMapOf<String, String>().apply {
            FEATS.forEach { if (it.opts.isNotEmpty()) put(it.id, it.def.ifEmpty { it.opts[0] }) }
        }
    }
    var log by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun cmds() = FEATS.filter { sel[it.id] == true }.flatMap { it.gen(vals[it.id] ?: "", pkg.trim()) }
    fun undo() = FEATS.filter { sel[it.id] == true }.flatMap { f ->
        f.undo.map { it.replace("{p}", pkg.trim().ifEmpty { "NAMA.PAKET" }) }
    }

    fun exec(list: List<String>) {
        if (list.isEmpty()) { log = "Pilih kartu dulu."; return }
        if (!ShizukuRunner.active()) { log = "Shizuku belum aktif. Buka aplikasi Shizuku dan mulai layanannya."; return }
        if (!ShizukuRunner.granted()) {
            Shizuku.requestPermission(1)
            log = "Izinkan akses Shizuku, lalu tekan Run lagi."; return
        }
        busy = true
        scope.launch {
            val sb = StringBuilder()
            withContext(Dispatchers.IO) {
                list.forEach { c -> sb.append("> ").append(c).append("\n").append(ShizukuRunner.run(c)).append("\n") }
            }
            log = sb.toString(); busy = false
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Text("Racik Shizuku", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = pkg, onValueChange = { pkg = it }, singleLine = true,
            label = { Text("Nama paket game (untuk kartu ★)") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(FEATS) { f ->
                val on = sel[f.id] == true
                Card(
                    modifier = Modifier.clickable { sel[f.id] = !on },
                    border = BorderStroke(if (on) 2.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(f.icon, fontSize = 24.sp)
                        Text(f.name, fontWeight = FontWeight.SemiBold)
                        Text(f.desc, fontSize = 12.sp)
                        if (f.opts.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                f.opts.forEach { o ->
                                    FilterChip(
                                        selected = vals[f.id] == o,
                                        onClick = { vals[f.id] = o; sel[f.id] = true },
                                        label = { Text(o + f.unit, fontSize = 12.sp) }
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
                log, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp).verticalScroll(rememberScrollState())
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { exec(undo()) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Kembalikan") }
            Button(onClick = { exec(cmds()) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                Text(if (busy) "Berjalan..." else "Run")
            }
        }
    }
}
