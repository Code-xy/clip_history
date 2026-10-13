package com.seagull.clip.data

data class Clip(
    val id: Long = 0L,
    val text: String,
    val kind: Kind,
    val hash: String,
    val pkg: String,
    val appLabel: String,
    val createdAt: Long,
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val hits: Int = 1
) {
    val lines: Int get() = text.count { it == '\n' } + 1
    val chars: Int get() = text.length

    /** 列表里显示的短预览：压掉多余空白 */
    fun preview(max: Int = 400): String {
        val flat = text.replace('\r', ' ').trim()
        return if (flat.length <= max) flat else flat.take(max) + " …"
    }
}
