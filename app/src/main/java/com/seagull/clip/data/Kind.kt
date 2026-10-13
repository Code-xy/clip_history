package com.seagull.clip.data

import java.util.regex.Pattern

/**
 * 剪贴板内容类型识别。纯本地正则，不走网络、不上传。
 */
enum class Kind(val id: String, val label: String) {
    TEXT("text", "文本"),
    URL("url", "链接"),
    EMAIL("email", "邮箱"),
    PHONE("phone", "电话"),
    NUMBER("number", "数字"),
    COLOR("color", "颜色"),
    CODE("code", "代码");

    companion object {
        private val P_URL = Pattern.compile("^(https?://|ftp://|www\\.)\\S+$", Pattern.CASE_INSENSITIVE)
        private val P_EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$")
        private val P_PHONE_CN = Pattern.compile("^1[3-9]\\d{9}$")
        private val P_NUM = Pattern.compile("^-?\\d+(\\.\\d+)?$")
        private val P_COLOR = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

        fun fromId(id: String): Kind = entries.firstOrNull { it.id == id } ?: TEXT

        fun of(raw: String): Kind {
            val t = raw.trim()
            if (t.isEmpty()) return TEXT
            if (P_COLOR.matcher(t).matches()) return COLOR
            if (P_URL.matcher(t).matches()) return URL
            if (P_EMAIL.matcher(t).matches()) return EMAIL
            val digits = t.replace(" ", "").replace("-", "").removePrefix("+86")
            if (P_PHONE_CN.matcher(digits).matches()) return PHONE
            if (P_NUM.matcher(t).matches()) return NUMBER
            if (looksLikeCode(t)) return CODE
            return TEXT
        }

        private val CODE_MARKS = listOf(
            "fun ", "def ", "class ", "function ", "import ", "public ", "private ",
            "const ", "var ", "let ", "=>", "};", "<?php", "SELECT ", "<html", "#!/"
        )

        private fun looksLikeCode(t: String): Boolean {
            if (t.length < 24) return false
            var hits = 0
            for (m in CODE_MARKS) if (t.contains(m)) hits++
            return hits >= 2
        }
    }
}
