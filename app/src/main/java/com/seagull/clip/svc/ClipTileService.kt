package com.seagull.clip.svc

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.seagull.clip.R
import com.seagull.clip.data.ClipRepo
import com.seagull.clip.data.Prefs

/**
 * 快捷设置磁贴：下拉通知栏一点即暂停/恢复记录。
 */
@RequiresApi(Build.VERSION_CODES.N)
class ClipTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        Prefs.init(this)
        Prefs.paused = !Prefs.paused
        ClipMonitor.resetDedup()
        refresh()
    }

    private fun refresh() {
        Prefs.init(this)
        ClipRepo.init(this)
        val tile = qsTile ?: return
        val paused = Prefs.paused
        tile.state = if (paused) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = if (paused) "已暂停" else "记录中"
        }
        if (Build.VERSION.SDK_INT >= 29) {
            tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_clip)
        }
        tile.updateTile()
    }
}
