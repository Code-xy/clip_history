package com.seagull.clip.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 手写 SQLite —— 不用 Room，省掉注解处理器和 KSP 版本坑，
 * 几千条量级下性能反而更可控。
 */
class ClipDb(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $T (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                text       TEXT    NOT NULL,
                kind       TEXT    NOT NULL,
                hash       TEXT    NOT NULL,
                pkg        TEXT    NOT NULL DEFAULT '',
                app_label  TEXT    NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                pinned     INTEGER NOT NULL DEFAULT 0,
                favorite   INTEGER NOT NULL DEFAULT 0,
                hits       INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_clip_hash ON $T(hash)")
        db.execSQL("CREATE INDEX idx_clip_created ON $T(created_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 起步，后续版本迁移写这里
    }

    // ── 写 ────────────────────────────────────────────────────────────

    /** 同 hash 已存在则删掉旧的，保证同内容只留最新一条 */
    fun upsertFresh(text: String, kind: Kind, hash: String, pkg: String, label: String, now: Long): Int {
        val db = writableDatabase
        var hits = 1
        db.rawQuery("SELECT id, hits FROM $T WHERE hash = ? LIMIT 1", arrayOf(hash)).use { c ->
            if (c.moveToFirst()) {
                hits = c.getInt(1) + 1
                db.delete(T, "hash = ?", arrayOf(hash))
            }
        }
        val v = ContentValues().apply {
            put("text", text)
            put("kind", kind.id)
            put("hash", hash)
            put("pkg", pkg)
            put("app_label", label)
            put("created_at", now)
            put("pinned", 0)
            put("favorite", 0)
            put("hits", hits)
        }
        db.insert(T, null, v)
        return hits
    }

    fun togglePin(id: Long, on: Boolean) = setFlag(id, "pinned", on)
    fun toggleFav(id: Long, on: Boolean) = setFlag(id, "favorite", on)

    private fun setFlag(id: Long, col: String, on: Boolean) {
        writableDatabase.execSQL("UPDATE $T SET $col = ? WHERE id = ?", arrayOf(if (on) 1 else 0, id))
    }

    fun updateText(id: Long, text: String, kind: Kind, hash: String) {
        writableDatabase.execSQL(
            "UPDATE $T SET text = ?, kind = ?, hash = ? WHERE id = ?",
            arrayOf(text, kind.id, hash, id)
        )
    }

    fun delete(id: Long) {
        writableDatabase.delete(T, "id = ?", arrayOf(id.toString()))
    }

    fun clearAll() {
        writableDatabase.delete(T, null, null)
    }

    /** 清空但保留置顶/收藏 */
    fun clearExceptKept() {
        writableDatabase.delete(T, "pinned = 0 AND favorite = 0", null)
    }

    /** 按条数上限裁剪（不动置顶和收藏） */
    fun trimByCount(max: Int) {
        if (max <= 0) return
        writableDatabase.execSQL(
            """
            DELETE FROM $T WHERE pinned = 0 AND favorite = 0 AND id NOT IN (
                SELECT id FROM $T WHERE pinned = 0 AND favorite = 0
                ORDER BY created_at DESC LIMIT ?
            )
            """.trimIndent(),
            arrayOf(max)
        )
    }

    /** 按天数裁剪（不动置顶和收藏） */
    fun trimByAge(days: Int) {
        if (days <= 0) return
        val edge = System.currentTimeMillis() - days * 24L * 3600_000L
        writableDatabase.delete(T, "pinned = 0 AND favorite = 0 AND created_at < ?", arrayOf(edge.toString()))
    }

    // ── 读 ────────────────────────────────────────────────────────────

    fun list(query: String, filter: Filter, limit: Int): List<Clip> {
        val where = StringBuilder()
        val args = ArrayList<String>()

        val q = query.trim()
        if (q.isNotEmpty()) {
            where.append("(text LIKE ? OR app_label LIKE ?)")
            args.add("%$q%")
            args.add("%$q%")
        }

        val f = when (filter) {
            Filter.ALL -> null
            Filter.PINNED -> "pinned = 1"
            Filter.FAV -> "(favorite = 1 OR pinned = 1)"
            Filter.LINK -> "kind = '${Kind.URL.id}'"
            Filter.TEXT -> "kind IN ('${Kind.TEXT.id}','${Kind.CODE.id}')"
        }
        if (f != null) {
            if (where.isNotEmpty()) where.append(" AND ")
            where.append(f)
        }

        val sql = buildString {
            append("SELECT * FROM $T")
            if (where.isNotEmpty()) append(" WHERE ").append(where)
            append(" ORDER BY pinned DESC, created_at DESC LIMIT ?")
        }
        args.add(limit.toString())

        val out = ArrayList<Clip>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            while (c.moveToNext()) out.add(c.toClip())
        }
        return out
    }

    fun count(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $T", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun nextAfter(id: Long, direction: Int): Clip? {
        val sql = if (direction > 0)
            "SELECT * FROM $T WHERE id <> ? ORDER BY created_at DESC LIMIT 1"
        else
            "SELECT * FROM $T WHERE id <> ? ORDER BY created_at ASC LIMIT 1"
        readableDatabase.rawQuery(sql, arrayOf(id.toString())).use {
            return if (it.moveToFirst()) it.toClip() else null
        }
    }

    private fun Cursor.toClip(): Clip = Clip(
        id = getLong(getColumnIndexOrThrow("id")),
        text = getString(getColumnIndexOrThrow("text")),
        kind = Kind.fromId(getString(getColumnIndexOrThrow("kind"))),
        hash = getString(getColumnIndexOrThrow("hash")),
        pkg = getString(getColumnIndexOrThrow("pkg")),
        appLabel = getString(getColumnIndexOrThrow("app_label")),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        pinned = getInt(getColumnIndexOrThrow("pinned")) == 1,
        favorite = getInt(getColumnIndexOrThrow("favorite")) == 1,
        hits = getInt(getColumnIndexOrThrow("hits"))
    )

    companion object {
        const val NAME = "clips.db"
        const val VERSION = 1
        const val T = "clip"
    }
}

enum class Filter(val label: String) {
    ALL("全部"),
    PINNED("置顶"),
    FAV("收藏"),
    LINK("链接"),
    TEXT("文字")
}
