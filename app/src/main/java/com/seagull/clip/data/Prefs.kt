package com.seagull.clip.data

import android.content.Context

/**
 * 极简设置存储。剪贴板数据全部留在本机，这里只存开关。
 */
object Prefs {
    private const val FILE = "clip_prefs"

    private const val K_PAUSED = "paused"
    private const val K_MAX_KEEP = "max_keep"
    private const val K_MAX_DAYS = "max_days"
    private const val K_SKIP_SENSITIVE = "skip_sensitive"
    private const val K_NOTIFY = "notify"
    private const val K_SKIP_SELF = "skip_self"
    private const val K_FIRST_RUN = "first_run"

    private var sp: android.content.SharedPreferences? = null

    fun init(ctx: Context) {
        if (sp == null) sp = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    private fun p(): android.content.SharedPreferences = sp!!

    /** 暂停记录 */
    var paused: Boolean
        get() = p().getBoolean(K_PAUSED, false)
        set(v) = p().edit().putBoolean(K_PAUSED, v).apply()

    /** 最多保留多少条 */
    var maxKeep: Int
        get() = p().getInt(K_MAX_KEEP, 1000)
        set(v) = p().edit().putInt(K_MAX_KEEP, v).apply()

    /** 最多保留多少天（0 = 不限） */
    var maxDays: Int
        get() = p().getInt(K_MAX_DAYS, 0)
        set(v) = p().edit().putInt(K_MAX_DAYS, v).apply()

    /** 跳过密码管理器等标记为敏感的内容 */
    var skipSensitive: Boolean
        get() = p().getBoolean(K_SKIP_SENSITIVE, true)
        set(v) = p().edit().putBoolean(K_SKIP_SENSITIVE, v).apply()

    /** 常驻通知保活 */
    var notify: Boolean
        get() = p().getBoolean(K_NOTIFY, true)
        set(v) = p().edit().putBoolean(K_NOTIFY, v).apply()

    /** 自己复制的内容不再重复入库 */
    var skipSelf: Boolean
        get() = p().getBoolean(K_SKIP_SELF, true)
        set(v) = p().edit().putBoolean(K_SKIP_SELF, v).apply()

    var firstRun: Boolean
        get() = p().getBoolean(K_FIRST_RUN, true)
        set(v) = p().edit().putBoolean(K_FIRST_RUN, v).apply()
}
