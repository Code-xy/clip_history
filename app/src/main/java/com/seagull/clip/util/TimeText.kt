package com.seagull.clip.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeText {
    private val HM = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val MDHM = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    private val FULL = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun rel(ts: Long): String {
        val d = System.currentTimeMillis() - ts
        return when {
            d < 0 -> HM.format(Date(ts))
            d < 60_000L -> "刚刚"
            d < 3_600_000L -> "${d / 60_000L} 分钟前"
            d < 86_400_000L -> "${d / 3_600_000L} 小时前"
            d < 604_800_000L -> "${d / 86_400_000L} 天前"
            else -> MDHM.format(Date(ts))
        }
    }

    fun full(ts: Long): String = FULL.format(Date(ts))
}
