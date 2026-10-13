# 剪贴板+ (ClipHistory)

安卓系统级剪贴板历史。记录、搜索、置顶、收藏你在任意 App 里复制过的内容。

- 包名：`com.seagull.clip`
- 最低版本：Android 7.0 (API 24)
- 目标版本：Android 15 (API 35)
- 纯本地：**没有申请网络权限**，数据不出设备

## 为什么必须开无障碍服务

Android 10 (API 29) 起，系统禁止后台应用读取剪贴板（`ClipboardManager` 返回 `null`）。
唯一稳定的读取途径是**无障碍服务**——它是系统绑定的上下文，不受该限制。

本 App 只做一件事：监听「点击 / 长按 / 文本选中 / 窗口切换」事件，
事件后延迟 160ms 读一次系统剪贴板，然后去重入库。

- ❌ 不采集界面文字（`canRetrieveWindowContent="false"`）
- ❌ 不做轮询（轮询费电，且会让 Android 12+ 反复弹「已读取剪贴板」）
- ❌ 不联网（Manifest 里没有 `INTERNET` 权限）

## 功能

| 功能 | 说明 |
|---|---|
| 自动记录 | 任意 App 复制即入库，同内容自动合并并累计「复制 N 次」 |
| 类型识别 | 链接 / 邮箱 / 电话 / 数字 / 颜色 / 代码，本地正则判定 |
| 搜索 | 搜内容，也能搜来源 App |
| 置顶 / 收藏 | 清理时自动跳过这两类 |
| 来源标注 | 显示是从哪个 App 复制来的 |
| 快捷磁贴 | 下拉通知栏一点暂停/恢复 |
| 常驻通知 | 可选，对抗国产 ROM 的后台清理 |
| 自动裁剪 | 按条数上限清理最旧的，置顶收藏不动 |
| 敏感跳过 | 密码管理器标记的敏感内容不记录 |
| 深色模式 | 跟随系统 |

## 安装后必做

1. **开启无障碍**：系统设置 → 无障碍 → 已下载的服务 → 剪贴板+ → 开启
2. **加自启动白名单**（国产 ROM 必做）：
   - MIUI：设置 → 应用设置 → 剪贴板+ → 自启动 + 省电策略「无限制」
   - EMUI / ColorOS / OriginOS 同理，在「应用启动管理」里放行
3. 可选：把通知栏的「剪贴板+」常驻通知保持开启，存活率更高

> 无障碍服务状态由系统持久化，重启后自动恢复，**不需要每次手动开 App**。

## 构建

推送到 `main` 即触发 GitHub Actions 出包（复用 `Code-xy/video_hex` 的可复用工作流）。

出包后从固定地址取：

```
https://github.com/Code-xy/clip_history/raw/ci-logs/.ci/app-release.apk
https://github.com/Code-xy/clip_history/raw/ci-logs/.ci/build.log
```

### 签名

仓库内置 `signing/dev-release.jks`（alias `clip`），保证每个包都能覆盖升级。

证书指纹（覆盖安装对不上时先比这个，**不要比 `META-INF/CERT.RSA` 字节**）：

```
SHA256: 87:23:40:14:18:03:7A:30:A2:D6:9D:55:FA:79:C9:DB:5E:C9:CA:18:B4:AD:F9:BA:7D:87:22:95:E6:23:61:51
```

优先级：CI secrets (`ANDROID_KEYSTORE_PATH`) > 仓库内置 jks > debug 签名。

## 代码结构

```
app/src/main/java/com/seagull/clip/
├── App.kt                        Application 初始化
├── MainActivity.kt               入口
├── data/
│   ├── Clip.kt                   数据模型
│   ├── Kind.kt                   内容类型识别（正则）
│   ├── ClipDb.kt                 手写 SQLite（不用 Room，省掉 KSP 版本坑）
│   ├── ClipRepo.kt               全局门面，协程 + Mutex 串行化
│   └── Prefs.kt                  设置存储
├── svc/
│   ├── ClipAccessibilityService.kt   核心：无障碍监听 + 读剪贴板
│   ├── ClipMonitor.kt                去重 / 过滤 / 来源标注
│   ├── KeepAliveService.kt           前台服务 + 常驻通知
│   └── ClipTileService.kt            快捷设置磁贴
├── ui/
│   ├── Theme.kt                  Material3 靛蓝主题
│   ├── ClipViewModel.kt          StateFlow 驱动
│   ├── Screens.kt                主界面 / 详情 / 设置
│   └── Components.kt             卡片 / 徽章 / 权限引导
└── util/
    ├── A11y.kt                   无障碍状态检测 + 跳转
    └── TimeText.kt               相对时间
```

## 已知限制

- 部分 App（尤其是自己实现了复制按钮但没触发无障碍事件的）可能漏记。
- 微信、淘宝等 App 的部分页面会主动阻止无障碍读取，属于对方策略，无法绕过。
- Android 13+ 系统自带剪贴板面板（输入法上方），本 App 是它的增强版：**保留更多条数、可搜索、可置顶收藏、显示来源**。
