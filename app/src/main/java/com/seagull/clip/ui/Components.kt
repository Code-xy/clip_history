package com.seagull.clip.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seagull.clip.R
import com.seagull.clip.data.Clip
import com.seagull.clip.data.Kind
import com.seagull.clip.util.TimeText

@Composable
fun KindBadge(kind: Kind) {
    val (bg, fg) = when (kind) {
        Kind.URL -> Color(0xFFDCE3FF) to Color(0xFF27408E)
        Kind.EMAIL -> Color(0xFFD3F2EC) to Color(0xFF0F5C52)
        Kind.PHONE -> Color(0xFFFFE9CC) to Color(0xFF8A5200)
        Kind.NUMBER -> Color(0xFFE7E9F2) to Color(0xFF4A4F5C)
        Kind.COLOR -> Color(0xFFF3DDFF) to Color(0xFF6B21A8)
        Kind.CODE -> Color(0xFFFFDDE0) to Color(0xFF8C1D2E)
        Kind.TEXT -> Color(0xFFE7E9F2) to Color(0xFF4A4F5C)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(kind.label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ClipItem(
    clip: Clip,
    onCopy: () -> Unit,
    onExpand: () -> Unit,
    onPin: () -> Unit,
    onFav: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onCopy, onLongClick = onExpand),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KindBadge(clip.kind)
                if (clip.pinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_pin),
                        contentDescription = "置顶",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (clip.favorite) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "收藏",
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFFE8A33D)
                    )
                }
                Spacer(Modifier.weight(1f))

                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "更多",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (clip.pinned) "取消置顶" else "置顶") },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_pin), null, Modifier.size(18.dp)) },
                            onClick = { menu = false; onPin() }
                        )
                        DropdownMenuItem(
                            text = { Text(if (clip.favorite) "取消收藏" else "收藏") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_star),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (clip.favorite) Color(0xFFE8A33D)
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { menu = false; onFav() }
                        )
                        DropdownMenuItem(
                            text = { Text("复制") },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_copy), null, Modifier.size(18.dp)) },
                            onClick = { menu = false; onCopy() }
                        )
                        DropdownMenuItem(
                            text = { Text("分享") },
                            leadingIcon = { Icon(Icons.Filled.Share, null, Modifier.size(18.dp)) },
                            onClick = { menu = false; onShare() }
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = { menu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = clip.preview(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (clip.kind == Kind.CODE) 4 else 3,
                overflow = TextOverflow.Ellipsis,
                fontFamily = if (clip.kind == Kind.CODE) FontFamily.Monospace else FontFamily.Default,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val src = clip.appLabel.ifEmpty { "未知来源" }
                Text(
                    src,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text("  ·  ${TimeText.rel(clip.createdAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (clip.hits > 1) {
                    Text("  ·  复制 ${clip.hits} 次", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                Text("${clip.chars} 字", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PermissionCard(onOpen: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "还没开启无障碍服务",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7A4E00),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Android 10 以后，系统只允许无障碍服务在后台读取剪贴板。" +
                    "不开这个开关，App 收不到任何内容。",
                fontSize = 12.sp,
                color = Color(0xFF7A4E00),
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpen) { Text("去开启", fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onDismiss) { Text("稍后") }
            }
            Text(
                "路径：系统设置 → 无障碍 → 已下载的服务 → 剪贴板+",
                fontSize = 11.sp,
                color = Color(0xFF9A7420)
            )
        }
    }
}

@Composable
fun EmptyState(hasQuery: Boolean, filterLabel: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (hasQuery) "没有匹配的内容" else "还没有记录",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasQuery) "换个关键词试试" else "在任意 App 里复制一段文字，「$filterLabel」里就会出现",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
