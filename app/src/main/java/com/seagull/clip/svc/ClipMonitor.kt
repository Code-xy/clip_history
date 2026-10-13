package com.seagull.clip.svc

import android.content.Context
import android.content.pm.PackageManager
import com.seagull.clip.data.ClipRepo
import com.seagull.clip.data.Kind
import com.seagull.clip.data.Prefs

/**
 * 剪贴板记录大脑：去重 / 过滤 / 来源标注。
 * 被无障碍服务、前台服务、App 自身共同调用。
 */
object ClipMonitor {

    /** 超过这个长度的内容不入库（防止几 MB 的巨型文本撑爆 db） */
    private const val MAX_LEN = 100_000

    /** 同一内容在这个时间内重复出现，视为同一次复制 */
    private const val DEDUP_WINDOW = 3_000L

    private var lastHash = ""
    private var lastAt = 0L

    private val labelCache = HashMap<String, String>()

    /**
     * @param sensitive 系统标记的敏感内容（密码管理器写入时会打这个标）
     * @return true 表示已入库
     */
    fun record(ctx: Context, raw: CharSequence?, pkg: String, sensitive: Boolean): Boolean {
        if (Prefs.paused) return false
        if (sensitive && Prefs.skipSensitive) return false

        val text = raw?.toString() ?: return false
        if (text.isBlank()) return false
        if (System.currentTimeMillis() < ClipRepo.suppressUntil) return false

        val body = if (text.length > MAX_LEN) text.substring(0, MAX_LEN) else text

        val hash = ClipRepo.hashOf(body)
        val now = System.currentTimeMillis()
        if (hash == lastHash && now - lastAt < DEDUP_WINDOW) return false
        lastHash = hash
        lastAt = now

        ClipRepo.insertAsync(body, Kind.of(body), pkg, appLabel(ctx, pkg))
        return true
    }

    fun appLabel(ctx: Context, pkg: String): String {
        if (pkg.isEmpty()) return ""
        labelCache[pkg]?.let { return it }
        val label = try {
            val pm = ctx.packageManager
            val info = if (android.os.Build.VERSION.SDK_INT >= 33)
                pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
            else
                @Suppress("DEPRECATION") pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            ""
        }
        if (label.isNotEmpty()) labelCache[pkg] = label
        return label
    }

    /** 手动清一次去重状态（比如用户切换了暂停开关） */
    fun resetDedup() {
        lastHash = ""
        lastAt = 0L
    }
}
