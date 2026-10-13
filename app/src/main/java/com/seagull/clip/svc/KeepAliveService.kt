package com.seagull.clip.svc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.seagull.clip.MainActivity
import com.seagull.clip.R
import com.seagull.clip.data.ClipRepo
import com.seagull.clip.data.Prefs

/**
 * 常驻通知 / 前台服务。
 *
 * 对无障碍服务本身不是必需（系统会自己托管它），
 * 但国产 ROM（MIUI / EMUI / ColorOS / OriginOS）的后台清理会连无障碍一起掐，
 * 挂个前台服务能显著提高存活率。用户可在设置里关掉。
 */
class KeepAliveService : Service() {

    override fun onCreate() {
        super.onCreate()
        ClipRepo.init(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE -> {
                Prefs.paused = !Prefs.paused
                ClipMonitor.resetDedup()
            }
        }
        pushForeground()
        return START_STICKY
    }

    private fun pushForeground() {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        try {
            ServiceCompat.startForeground(this, NOTI_ID, buildNotification(), type)
        } catch (t: Throwable) {
            stopSelf()
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val toggle = PendingIntent.getService(
            this, 2,
            Intent(this, KeepAliveService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val paused = Prefs.paused
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_clip)
            .setContentTitle(if (paused) "剪贴板+ 已暂停" else "剪贴板+ 记录中")
            .setContentText(if (paused) "点「恢复」继续记录" else "已保存 ${ClipRepo.count.value} 条")
            .setOngoing(!paused)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(open)
            .addAction(0, if (paused) "恢复" else "暂停", toggle)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val ch = NotificationChannel(CHANNEL, "剪贴板记录", NotificationManager.IMPORTANCE_MIN).apply {
            description = "后台记录剪贴板内容时显示"
            setShowBadge(false)
        }
        nm.createNotificationChannel(ch)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL = "clip_keepalive"
        const val NOTI_ID = 1001
        const val ACTION_STOP = "com.seagull.clip.STOP"
        const val ACTION_TOGGLE = "com.seagull.clip.TOGGLE"

        fun start(ctx: Context) {
            val i = Intent(ctx, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, KeepAliveService::class.java))
        }

        fun refresh(ctx: Context) {
            if (Prefs.notify) start(ctx) else stop(ctx)
        }
    }
}
