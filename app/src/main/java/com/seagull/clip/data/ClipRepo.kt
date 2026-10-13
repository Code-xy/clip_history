package com.seagull.clip.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * 全局仓库门面：唯一入口，内部串行化，避免无障碍线程 / UI 线程并发写库。
 */
object ClipRepo {
    const val PAGE = 400

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()

    @Volatile private var dbRef: ClipDb? = null

    private val _rev = MutableStateFlow(0L)
    val rev: StateFlow<Long> = _rev.asStateFlow()

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    /** 自己写入剪贴板后的静默窗口，避免「复制→又被记一条」 */
    @Volatile var suppressUntil: Long = 0L

    fun init(ctx: Context) {
        if (dbRef == null) {
            Prefs.init(ctx)
            dbRef = ClipDb(ctx)
        }
    }

    private val db: ClipDb get() = dbRef!!

    fun hashOf(text: String): String {
        val d = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(32)
        for (b in d) sb.append("%02x".format(b))
        return sb.toString()
    }

    private fun bump() {
        _rev.value = _rev.value + 1
        _count.value = runCatching { db.count() }.getOrDefault(0)
    }

    fun refreshCount() {
        scope.launch { _count.value = runCatching { db.count() }.getOrDefault(0) }
    }

    // ── 写 ────────────────────────────────────────────────────────────

    /** 记一条。返回 true 表示确实入库 */
    fun insertAsync(text: String, kind: Kind, pkg: String, label: String) {
        scope.launch {
            lock.withLock {
                runCatching {
                    val now = System.currentTimeMillis()
                    db.upsertFresh(text, kind, hashOf(text), pkg, label, now)
                    db.trimByCount(Prefs.maxKeep)
                    db.trimByAge(Prefs.maxDays)
                }
                bump()
            }
        }
    }

    fun togglePin(id: Long, on: Boolean) = scope.launch {
        lock.withLock { runCatching { db.togglePin(id, on) } }
        bump()
    }

    fun toggleFav(id: Long, on: Boolean) = scope.launch {
        lock.withLock { runCatching { db.toggleFav(id, on) } }
        bump()
    }

    fun updateText(id: Long, text: String) = scope.launch {
        lock.withLock { runCatching { db.updateText(id, text, Kind.of(text), hashOf(text)) } }
        bump()
    }

    fun delete(id: Long) = scope.launch {
        lock.withLock { runCatching { db.delete(id) } }
        bump()
    }

    fun clearAll() = scope.launch {
        lock.withLock { runCatching { db.clearAll() } }
        bump()
    }

    fun clearKeepStarred() = scope.launch {
        lock.withLock { runCatching { db.clearExceptKept() } }
        bump()
    }

    // ── 读 ────────────────────────────────────────────────────────────

    suspend fun list(query: String, filter: Filter): List<Clip> = lock.withLock {
        runCatching { db.list(query, filter, PAGE) }.getOrDefault(emptyList())
    }

    suspend fun countSync(): Int = lock.withLock {
        runCatching { db.count() }.getOrDefault(0)
    }

    // ── 剪贴板写入 ────────────────────────────────────────────────────

    fun copyToClipboard(ctx: Context, text: String) {
        if (Prefs.skipSelf) suppressUntil = System.currentTimeMillis() + 2000L
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        runCatching { cm.setPrimaryClip(ClipData.newPlainText("clip", text)) }
    }
}
