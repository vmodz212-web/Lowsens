package com.example.dkracik

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuRunner {

    fun active(): Boolean = try { Shizuku.pingBinder() } catch (e: Throwable) { false }

    fun granted(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) { false }

    /** Jalankan satu perintah shell lewat Shizuku. Panggil dari thread latar belakang. */
    fun run(cmd: String): String = try {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java, Array<String>::class.java, String::class.java
        )
        m.isAccessible = true
        val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        val code = p.waitFor()
        (out + err).ifBlank { "selesai" } + if (code != 0) "\n(kode keluar $code)" else ""
    } catch (e: Throwable) {
        "Gagal: ${e.message ?: e.javaClass.simpleName}"
    }
}
