package com.seagull.clip.svc

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.seagull.clip.data.ClipRepo
import com.seagull.clip.data.Prefs

/**
 * 核心监听器。
 *
 * Android 10 (API 29) 起，后台应用直接读剪贴板会被系统拦截（返回 null）。
 * 无障碍服务是系统绑定的特殊上下文，是唯一稳定可行的读取途径。
 *
 * 工作方式：监听点击 / 长按 / 文本选中 / 窗口切换事件，事件后延迟 160ms 再读一次剪贴板。
 * 不做轮询 —— 轮询既费电，又会让 Android 12+ 反复弹「已读取剪贴板」提示。
 */
class ClipAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastEventAt = 0L
    private var pendingPkg = ""

    private val readTask = Runnable { readClip(pendingPkg) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ClipRepo.init(this)
        alive = true
        Log.i(TAG, "无障碍服务已连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        if (Prefs.paused) return

        when (e.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> Unit
            else -> return
        }

        val now = SystemClock.uptimeMillis()
        if (now - lastEventAt < 250L) return
        lastEventAt = now

        pendingPkg = e.packageName?.toString().orEmpty()
        handler.removeCallbacks(readTask)
        handler.postDelayed(readTask, 160L)
    }

    private fun readClip(pkg: String) {
        if (Prefs.paused) return
        val cm = getSystemService(ClipboardManager::class.java) ?: return
        if (!cm.hasPrimaryClip()) return
        val clip = cm.primaryClip ?: return
        if (clip.itemCount <= 0) return

        val sensitive = try {
            clip.description?.extras?.getBoolean(EXTRA_IS_SENSITIVE) ?: false
        } catch (t: Throwable) {
            false
        }

        val text = try {
            clip.getItemAt(0).coerceToText(this)
        } catch (t: Throwable) {
            null
        }

        val ok = ClipMonitor.record(this, text, pkg, sensitive)
        if (ok) Log.d(TAG, "记录一条，来源=$pkg")
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        alive = false
        handler.removeCallbacks(readTask)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ClipA11y"
        private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

        @Volatile var alive: Boolean = false
            private set
    }
}
