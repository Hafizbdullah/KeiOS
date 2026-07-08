# KeepAlive Self-Guard Validation

验证日期：2026-07-08
包名：`os.kei.debug`  
入口：`os.kei.debug/os.kei.LauncherAndroidDesigns`

## 结论

KeepAlive 自保链路已聚焦 KeiOS 自身后台能力检查。当前实现保留 Shizuku / secure-settings 读取能力检查、前台自保服务、开机/更新后检查、亮屏检查、手动检查和本地历史导出。

AVD 覆盖 Android 16 / API 36 与 Android 17 / API 37。本轮验收重点为设置页渲染、手动检查历史、receiver 显式检查、debug 覆盖安装兼容和取消协程 toast 回归。

## 构建与测试

| 项目 | 命令 | 结果 |
| --- | --- | --- |
| feature + app unit tests | `./gradlew :feature-keepalive:testDebugUnitTest :app:testDebugUnitTest --continue` | 通过 |
| debug build | `./gradlew :app:assembleDebug` | 通过 |
| AVD install | `adb install -r app/build/outputs/apk/debug/app-debug.apk` | Android 16 / 17 均通过 |

## 本地 SDK 源码基线

平台行为优先参考本地 Android SDK sources：

| SDK | 路径 | 用途 |
| --- | --- | --- |
| API 37.1 | `/Users/voyager/Library/Android/sdk/sources/android-37.1` | target SDK 行为主基线 |
| API 36.1 | `/Users/voyager/Library/Android/sdk/sources/android-36.1` | 用户 Android 16 日常设备对照 |
| API 35 | `/Users/voyager/Library/Android/sdk/sources/android-35` | min SDK 兼容确认 |

已确认的 AOSP 能力点：

- `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` 可作为当前无障碍启用状态读取源。
- `Settings.Secure.ACCESSIBILITY_ENABLED` 仍属于 secure settings 范畴。
- `FOREGROUND_SERVICE_SPECIAL_USE` 与 service-level subtype property 是 specialUse FGS 的平台要求。
- `OPSTR_BIND_ACCESSIBILITY_SERVICE` 属于受保护设置范围，后续 Shizuku / 无障碍增强方案需要单独设计。

## 当前链路

| 链路 | 当前行为 |
| --- | --- |
| Shizuku bridge | 只读 `enabled_accessibility_services` |
| Settings store | `daemonEnabled`、`bootCheckEnabled`、`screenOnCheckEnabled` |
| Manual check | 调用 `AccessibilityGuardCheckRunner.checkAndRecord()` |
| Timeout | 12 秒后写入 `TimedOut` 历史 |
| History | JSONL schema v2，字段为 `checkCount`、`healthyCount`、`warningCount` |
| Legacy history | schema v1 restore 记录会映射为 schema v2 check 记录 |
| WebDAV | 自保历史和运行状态不参与同步 |

## AVD 验收记录

| 设备 | Android | SDK | 结果 | 截图 |
| --- | --- | --- | --- | --- |
| `emulator-5554` | 16 | 36 | 通过：保活页可进入，自保策略渲染正常，手动检查后历史更新为 `已检查` | `build/qa/keepalive-self/api36-settings-keepalive.png`, `build/qa/keepalive-self/api36-settings-keepalive-history-after-check.png` |
| `emulator-5556` | 17 | 37 | 通过：保活页可进入，自保策略渲染正常，手动检查后历史更新为 `已检查` | `build/qa/keepalive-self/api37-settings-keepalive.png`, `build/qa/keepalive-self/api37-settings-keepalive-history-after-check.png` |

## Fork Shizuku 验收记录

验证时间：2026-07-09 00:07 +0800

Android 17 需要第三方 Shizuku fork 时，Manager 包名可能与官方包名不同。本项目 Shizuku 验收基准固定为应用侧运行证据：

- `rikka.shizuku.Shizuku` binder 存活。
- `moe.shizuku.manager.permission.API_V23` 已授权给 KeiOS。
- Shizuku 服务 UID 为 `2000`，命令身份为 `shell`。
- 通过 Shizuku 执行 `id`、`whoami`、`getenforce`。
- 自保链路通过 `settings get secure enabled_accessibility_services` 读取安全设置。
- 自保历史写入 `Healthy`，`shizukuStatus=ready`。

本轮确认自保链路以应用侧 Shizuku binder、授权状态和命令执行能力作为判断依据。包名扫描只适合作为辅助调试信息，实际通过应用侧 binder 和命令执行能力判断。

| 设备 | Android | SDK | Shizuku 证据 | 自保证据 | 截图 |
| --- | --- | --- | --- | --- | --- |
| `emulator-5554` | 16 | 36 | `Binder Alive=true`，`Permission Granted=true`，`Command Identity=shell`，`Service UID=2000`，`SELinux Context=u:r:shell:s0` | `ScreenOn` 追加 `Healthy`，`checkCount=4`，`healthyCount=4`，`warningCount=0` | `build/qa/keepalive-self/api36-shizuku-keepalive-final.png` |
| `emulator-5556` | 17 | 37 | `Binder Alive=true`，`Permission Granted=true`，`Command Identity=shell`，`Service UID=2000`，`SELinux Context=u:r:shell:s0` | `ScreenOn` 追加 `Healthy`，`checkCount=4`，`healthyCount=4`，`warningCount=0` | `build/qa/keepalive-self/api37-shizuku-keepalive-final.png` |

运行证据：

```text
API 36:
PASS Shizuku init: Shizuku Binder Alive=true | Shizuku Permission Granted=true | Shizuku Activated=true | Shizuku Command Identity=shell | Shizuku Service UID=2000 | Shizuku Service Version=13 | Shizuku Server Patch Version=6 | Shizuku SELinux Context=u:r:shell:s0 | Shizuku whoami=shell | Shizuku getenforce=Enforcing
history: reason=ScreenOn status=Healthy triggerAction=android.intent.action.SCREEN_ON checkCount=4 healthyCount=4 warningCount=0 shizukuStatus=ready

API 37:
PASS Shizuku init: Shizuku Binder Alive=true | Shizuku Permission Granted=true | Shizuku Activated=true | Shizuku Command Identity=shell | Shizuku Service UID=2000 | Shizuku Service Version=13 | Shizuku Server Patch Version=6 | Shizuku SELinux Context=u:r:shell:s0 | Shizuku whoami=shell | Shizuku getenforce=Enforcing
history: reason=ScreenOn status=Healthy triggerAction=android.intent.action.SCREEN_ON checkCount=4 healthyCount=4 warningCount=0 shizukuStatus=ready
```

前台服务证据：

```text
API 36: AccessibilityGuardForegroundService isForeground=true foregroundId=39887 types=0x40000000
API 37: AccessibilityGuardForegroundService isForeground=true foregroundId=39887 types=0x40000000
notification title=KeiOS 自保服务 channel=accessibility_guard_service_channel_v1 channelName=自保服务
```

应验证 UI：

- 保活底栏可进入。
- 存在“KeiOS 自保 / 自保策略”卡片。
- 存在“自保历史”卡片。
- 策略卡含“自保状态 / 前台服务 / 开机与更新后检查 / 亮屏检查 / 检查范围 / 检查与导出”。
- 页面仅保留 KeiOS 自保策略和自保历史。
- 当前 AVD 命令通道返回 `ready`，自保状态显示“安全设置可读”。
- 历史中保留旧的 `MissingPrivilege` 记录，手动检查后最新记录更新为 `Checked`。
- logcat 最近窗口内没有 `JobCancellationException` 或自保失败 toast。

## Receiver 验证

显式检查命令：

```bash
adb -s emulator-5556 shell am broadcast \
  -n os.kei.debug/os.kei.feature.keepalive.receiver.AccessibilityGuardEventReceiver \
  -a os.kei.keepalive.action.CHECK_ACCESSIBILITY_GUARD
```

期望结果：

- broadcast 返回 `result=0`。
- `files/keepalive/accessibility-guard-history.jsonl` 追加记录。
- 新记录使用 schema v2 字段：`status`、`checkCount`、`healthyCount`、`warningCount`。
- API 36 / 37 最新显式 receiver 记录均为 `Checked`，`checkCount=1`、`healthyCount=1`、`warningCount=0`。

## 风险与后续

| 风险 | 状态 | 后续动作 |
| --- | --- | --- |
| 实体机 Shizuku 已授权路径 | AVD 无法覆盖 | 在用户设备验证 secure-settings 可读状态和历史记录 |
| Boot / app update 系统广播 | AVD 可通过安装和显式 receiver 覆盖基础路径 | 实体机覆盖安装后观察历史 |
| OEM 保活策略 | AVD 只代表 AOSP 原生行为 | 后续按无障碍 / Shizuku 自保方案继续扩展 |
| 高级自保 | P3 | 独立设计后再落地 |
