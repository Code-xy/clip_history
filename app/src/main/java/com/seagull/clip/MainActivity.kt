package com.seagull.clip

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seagull.clip.data.ClipRepo
import com.seagull.clip.data.Prefs
import com.seagull.clip.svc.KeepAliveService
import com.seagull.clip.ui.ClipTheme
import com.seagull.clip.ui.ClipViewModel
import com.seagull.clip.ui.MainScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        ClipRepo.init(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTI)
        }

        if (Prefs.notify) runCatching { KeepAliveService.refresh(this) }

        setContent {
            ClipTheme {
                val vm: ClipViewModel = viewModel()
                MainScreen(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Prefs.notify) runCatching { KeepAliveService.refresh(this) }
    }

    companion object {
        private const val REQ_NOTI = 1001
    }
}
