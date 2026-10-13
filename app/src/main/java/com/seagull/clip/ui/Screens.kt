package com.seagull.clip.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seagull.clip.data.Clip
import com.seagull.clip.data.Filter
import com.seagull.clip.data.Prefs
import com.seagull.clip.svc.KeepAliveService
import com.seagull.clip.util.A11y
import com.seagull.clip.util.TimeText
import kotlinx.coroutines.launch

@Composable
fun MainScreen(vm: ClipViewModel) {
    val ctx = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycle()
    val count by vm.count.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()

    var a11y by remember { mutableStateOf(A11y.enabled(ctx)) }
    var paused by remember { mutableStateOf(Prefs.paused) }
    var searching by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var hideTip by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Clip?>(null) }

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 从系统设置返回时刷新权限状态
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        a11y = A11y.enabled(ctx)
        paused = Prefs.paused
        vm.refresh()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snack) { data -> Snackbar(data, containerColor = MaterialTheme.colorScheme.surfaceVariant) }
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Column {
                        Text("剪贴板+", fontWeight = FontWeight.SemiBold)
                        Text(
                            "$count 条 · " + if (paused) "已暂停" else "记录中",
                            fontSize = 11.sp,
                            color = if (paused) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searching = !searching }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            AnimatedVisibility(visible = searching) {
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text("搜索内容或来源 App", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { vm.setQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "清空")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            if (!a11y && !hideTip) {
                PermissionCard(
                    onOpen = { A11y.openSettings(ctx) },
                    onDismiss = { hideTip = true }
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Filter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { vm.setFilter(f) },
                        label = { Text(f.label, fontSize = 13.sp) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                EmptyState(hasQuery = query.isNotBlank(), filterLabel = filter.label)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { clip ->
                        ClipItem(
                            clip = clip,
                            onCopy = {
                                vm.copy(clip)
                                scope.launch { snack.showSnackbar("已复制到剪贴板") }
                            },
                            onExpand = { detail = clip },
                            onPin = { vm.pin(clip) },
                            onFav = { vm.fav(clip) },
                            onShare = { A11y.share(ctx, clip.text) },
                            onDelete = {
                                vm.remove(clip)
                                scope.launch { snack.showSnackbar("已删除") }
                            }
                        )
                    }
                }
            }
        }
    }

    detail?.let { clip ->
        DetailDialog(
            clip = clip,
            onDismiss = { detail = null },
            onCopy = { vm.copy(clip); scope.launch { snack.showSnackbar("已复制") } },
            onShare = { A11y.share(ctx, clip.text) }
        )
    }

    if (showSettings) {
        SettingsSheet(
            count = count,
            onDismiss = { showSettings = false },
            onChanged = { paused = Prefs.paused; vm.refresh() },
            onClear = { vm.clearAll(); scope.launch { snack.showSnackbar("已清空") } },
            onClearKeep = { vm.clearKeepStarred(); scope.launch { snack.showSnackbar("已清空（保留置顶/收藏）") } }
        )
    }
}

@Composable
private fun DetailDialog(clip: Clip, onDismiss: () -> Unit, onCopy: () -> Unit, onShare: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${clip.kind.label} · ${clip.chars} 字", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    (clip.appLabel.ifEmpty { "未知来源" }) + " · " + TimeText.full(clip.createdAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
                Text(
                    clip.text,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onShare) { Text("分享") }
                TextButton(onClick = { onCopy(); onDismiss() }) { Text("复制") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun SettingsSheet(
    count: Int,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    onClear: () -> Unit,
    onClearKeep: () -> Unit
) {
    val ctx = LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var paused by remember { mutableStateOf(Prefs.paused) }
    var notify by remember { mutableStateOf(Prefs.notify) }
    var sensitive by remember { mutableStateOf(Prefs.skipSensitive) }
    var skipSelf by remember { mutableStateOf(Prefs.skipSelf) }
    var keep by remember { mutableStateOf(Prefs.maxKeep) }
    var confirmClear by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {

            Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(12.dp))

            SwitchRow(
                "记录中",
                if (paused) "已暂停，不再记录新内容" else "正常记录剪贴板变化",
                enabled = !paused
            ) {
                paused = !it
                Prefs.paused = paused
                if (paused) KeepAliveService.stop(ctx) else KeepAliveService.refresh(ctx)
                onChanged()
            }

            SwitchRow(
                "常驻通知",
                "提高被国产 ROM 后台清理后的存活率",
                enabled = notify
            ) {
                notify = it
                Prefs.notify = it
                KeepAliveService.refresh(ctx)
            }

            SwitchRow(
                "跳过敏感内容",
                "密码管理器标记的敏感字段不记录",
                enabled = sensitive
            ) {
                sensitive = it
                Prefs.skipSensitive = it
            }

            SwitchRow(
                "复制时不重复入库",
                "在本 App 里点复制，不产生新的记录",
                enabled = skipSelf
            ) {
                skipSelf = it
                Prefs.skipSelf = it
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(Modifier.height(14.dp))

            Text("保留上限", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "超出后自动清理最旧的记录（置顶和收藏不动）",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(300, 1000, 5000).forEach { n ->
                    FilterChip(
                        selected = keep == n,
                        onClick = { keep = n; Prefs.maxKeep = n },
                        label = { Text(if (n >= 1000) "${n / 1000}k" else "$n", fontSize = 13.sp) }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(Modifier.height(14.dp))

            Text("数据", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClearKeep) { Text("清空（留置顶收藏）") }
                TextButton(onClick = { confirmClear = true }) {
                    Text("全部清空", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "共 $count 条 · 数据全部存本机，App 没有申请网络权限",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "无障碍服务仅用于读取剪贴板，不采集界面内容、不联网上传。",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("确认清空全部记录？") },
            text = { Text("包括置顶和收藏，清空后无法恢复。") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClear() }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    sub: String,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}
