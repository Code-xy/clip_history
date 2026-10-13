package com.seagull.clip.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.seagull.clip.svc.ClipAccessibilityService

object A11y {

    fun enabled(ctx: Context): Boolean {
        val flat = runCatching {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull() ?: return false
        if (flat.isEmpty()) return false

        val want = ComponentName(ctx, ClipAccessibilityService::class.java)
        val full = want.flattenToString()
        val short = want.flattenToShortString()
        return flat.split(':').any { it.equals(full, true) || it.equals(short, true) }
    }

    fun openSettings(ctx: Context) {
        val candidates = listOf(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Intent("android.settings.ACCESSIBILITY_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (i in candidates) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (ctx.packageManager.resolveActivity(i, 0) == null) continue
            if (runCatching { ctx.startActivity(i) }.isSuccess) return
        }
        // 全失败 → 退到应用详情页
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", ctx.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun share(ctx: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            ctx.startActivity(
                Intent.createChooser(send, "分享到").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
