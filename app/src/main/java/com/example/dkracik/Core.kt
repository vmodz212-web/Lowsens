package com.example.dkracik

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku

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

fun iconOf(ctx: Context, pkg: String): ImageBitmap? = try {
    val d = ctx.packageManager.getApplicationIcon(pkg)
    val bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
    d.setBounds(0, 0, 128, 128)
    d.draw(AndroidCanvas(bmp))
    bmp.asImageBitmap()
} catch (e: Throwable) {
    null
}

@Suppress("DEPRECATION")
fun detectHz(ctx: Context): List<String> = try {
    val modes = (ctx as? Activity)?.windowManager?.defaultDisplay?.supportedModes
    val l = modes?.map { Math.round(it.refreshRate) }?.distinct()?.sorted() ?: emptyList()
    if (l.isEmpty()) listOf("60") else l.map { it.toString() }
} catch (e: Throwable) {
    listOf("60")
}

class Feat(
    val id: String, val tab: Int, val icon: String, val name: String, val desc: String,
    val opts: List<String> = emptyList(), val def: String = "", val unit: String = "",
    val once: Boolean = false, val needsPkg: Boolean = false,
    val gen: (String, String) -> List<String>, val undo: List<String> = emptyList()
)

fun makeFeats(hz: List<String>): List<Feat> = listOf(
    Feat("hz", 1, "🔄", "Refresh rate", "", opts = hz, def = hz.last(), unit = "Hz",
        gen = { v, _ -> listOf("settings put system min_refresh_rate $v.0", "settings put system peak_refresh_rate $v.0") },
        undo = listOf("settings delete system min_refresh_rate", "settings delete system peak_refresh_rate")),
    Feat("font", 1, "🔤", "Ukuran teks", "", opts = listOf("0.9", "1.0", "1.1", "1.2"), def = "1.0", unit = "x",
        gen = { v, _ -> listOf("settings put system font_scale $v") },
        undo = listOf("settings put system font_scale 1.0")),
    Feat("cache", 2, "🧹", "Bersihkan cache", "Ketuk untuk menjalankan sekali", once = true,
        gen = { _, _ -> listOf("pm trim-caches 999G") }),
    Feat("ph", 2, "👻", "Phantom process", "Batas dilepas, Android 12+",
        gen = { _, _ -> listOf("device_config set_sync_disabled_for_tests persistent", "device_config put activity_manager max_phantom_processes 2147483647", "settings put global settings_enable_monitor_phantom_procs false") },
        undo = listOf("device_config delete activity_manager max_phantom_processes", "settings put global settings_enable_monitor_phantom_procs true", "device_config set_sync_disabled_for_tests none")),
    Feat("dnd", 2, "🔕", "Jangan ganggu", "Mode prioritas, volume tetap bisa diubah",
        gen = { _, _ -> listOf("cmd notification set_dnd priority") },
        undo = listOf("cmd notification set_dnd off")),
    Feat("cmp", 3, "🚀", "Percepat game", "Compile speed-profile", needsPkg = true,
        gen = { _, p -> listOf("cmd package compile -m speed-profile -f $p") },
        undo = listOf("cmd package compile --reset {p}")),
    Feat("gm", 3, "🎯", "Game mode", "Prioritaskan performa", needsPkg = true,
        gen = { _, p -> listOf("cmd game mode performance $p") },
        undo = listOf("cmd game reset {p}")),
    Feat("gpu", 3, "🖥️", "Driver ANGLE", "Pakai ANGLE untuk game", needsPkg = true,
        gen = { _, p -> listOf("settings put global angle_gl_driver_selection_pkgs $p", "settings put global angle_gl_driver_selection_values angle") },
        undo = listOf("settings delete global angle_gl_driver_selection_pkgs", "settings delete global angle_gl_driver_selection_values"))
)

class Ctl(val ctx: Context, val scope: CoroutineScope, hz: List<String>) {
    private val prefs = ctx.getSharedPreferences("lowsens", 0)
    val feats = makeFeats(hz)
    var pkg by mutableStateOf(prefs.getString("pkg", "") ?: "")
    var showMsg by mutableStateOf(prefs.getBoolean("msg", true))
    var mode by mutableStateOf(prefs.getString("mode", "performance") ?: "performance")
    var result by mutableStateOf("Hasil aksi terakhir muncul di sini.")
    var busy by mutableStateOf(false)
    var apps by mutableStateOf<List<AppItem>?>(null)
    val sel = mutableStateMapOf<String, Boolean>()
    val vals = mutableStateMapOf<String, String>()
    val used = mutableStateMapOf<String, String>()
    val scales = mutableStateMapOf<String, String>()
    val added = mutableStateListOf<String>()

    init {
        feats.forEach { f ->
            sel[f.id] = prefs.getBoolean("on_" + f.id, false)
            used[f.id] = prefs.getString("p_" + f.id, "") ?: ""
            if (f.opts.isNotEmpty()) {
                val s = prefs.getString("v_" + f.id, null)
                vals[f.id] = if (s != null && s in f.opts) s else f.def.ifEmpty { f.opts[0] }
            }
        }
        added.addAll(prefs.getStringSet("added", emptySet()) ?: emptySet())
        scope.launch { apps = withContext(Dispatchers.IO) { loadApps(ctx) } }
    }

    fun games(): List<AppItem> {
        val all = apps ?: return emptyList()
        return all.filter { it.game || it.pkg in added }.sortedBy { it.label.lowercase() }
    }

    fun addGame(p: String) {
        if (p !in added) {
            added.add(p)
            prefs.edit().putStringSet("added", added.toSet()).apply()
        }
    }

    fun scaleOf(p: String): String = scales[p] ?: prefs.getString("scale_$p", "1.0") ?: "1.0"

    fun setScale(p: String, s: String) {
        scales[p] = s
        prefs.edit().putString("scale_$p", s).apply()
    }

    fun setPkg(p: String) { pkg = p; save() }

    fun setMsg(v: Boolean) { showMsg = v; prefs.edit().putBoolean("msg", v).apply() }

    fun save() {
        val e = prefs.edit()
        e.putString("pkg", pkg)
        e.putString("mode", mode)
        feats.forEach { f ->
            e.putBoolean("on_" + f.id, sel[f.id] == true)
            e.putString("p_" + f.id, used[f.id] ?: "")
            vals[f.id]?.let { e.putString("v_" + f.id, it) }
        }
        e.apply()
    }

    private fun say(m: String) { Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show() }

    fun note(m: String) { result = m; if (showMsg) say(m) }

    fun fail(m: String) { result = m; say(m) }

    private fun bad(r: String) = r.startsWith("Gagal") || r.contains("Exception") || r.contains("Error")

    fun ready(): Boolean {
        if (!ShizukuRunner.active()) { fail("Shizuku belum aktif. Buka aplikasi Shizuku dan mulai layanannya."); return false }
        if (!ShizukuRunner.granted()) { Shizuku.requestPermission(1); fail("Izinkan akses Shizuku, lalu ketuk lagi."); return false }
        return true
    }

    fun exec(list: List<String>, okMsg: String, done: () -> Unit = {}) {
        if (list.isEmpty()) { note(okMsg); done(); return }
        scope.launch {
            busy = true
            var err = ""
            withContext(Dispatchers.IO) {
                list.forEach { c ->
                    val r = ShizukuRunner.run(c)
                    if (err.isEmpty() && bad(r)) err = r.take(90)
                }
            }
            busy = false
            if (err.isNotEmpty()) fail(err) else note(okMsg)
            done()
        }
    }

    fun pkgOf(f: Feat): String = (used[f.id] ?: "").ifEmpty { pkg.trim() }

    private fun undoOf(f: Feat): List<String> =
        f.undo.map { it.replace("{p}", pkgOf(f).ifEmpty { "NAMA.PAKET" }) }

    fun toggle(f: Feat) {
        val on = sel[f.id] != true
        if (!ready()) return
        val p = pkg.trim()
        if (on && f.needsPkg && p.isEmpty()) { fail("Pilih game dulu di tab Game."); return }
        if (on) {
            sel[f.id] = true
            used[f.id] = p
            exec(f.gen(vals[f.id] ?: "", p), if (f.once) f.name + " dijalankan" else f.name + " diaktifkan") {
                if (f.once) sel[f.id] = false
                save()
            }
        } else {
            sel[f.id] = false
            exec(undoOf(f), f.name + " dimatikan") { save() }
        }
        save()
    }

    fun choose(f: Feat, o: String) {
        vals[f.id] = o
        if (sel[f.id] == true && ready()) exec(f.gen(o, pkgOf(f)), f.name + " diubah ke " + o + f.unit) { save() } else save()
    }

    fun startGame(g: AppItem) {
        if (!ready()) return
        pkg = g.pkg
        val sc = scaleOf(g.pkg).trim()
        val down = if (sc.isEmpty() || sc == "1" || sc == "1.0") "disable" else sc
        scope.launch {
            busy = true
            val r = withContext(Dispatchers.IO) {
                var out = ShizukuRunner.run("cmd game set --mode $mode --downscale $down ${g.pkg}")
                if (bad(out)) out = ShizukuRunner.run("cmd game mode $mode ${g.pkg}")
                out
            }
            busy = false
            if (bad(r)) fail(r.take(90)) else note("Game mode $mode aktif untuk ${g.label}")
            val intent = ctx.packageManager.getLaunchIntentForPackage(g.pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } else {
                fail("Game tidak bisa dibuka.")
            }
            save()
        }
    }
}
