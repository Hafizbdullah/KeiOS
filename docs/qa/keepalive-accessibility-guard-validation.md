# KeepAlive Accessibility Guard Validation

验证时间：2026-07-08  
验证提交：`a1d7a1b70`  
包名：`os.kei.debug`  
入口：`os.kei.debug/os.kei.LauncherAndroidDesigns`

## 结论

无障碍守护在 Android 16 / 17 AVD 上完成基础验收：设置页保活板块可打开，能力卡、策略卡、服务列表、手动检查和历史卡均可渲染；系统无障碍服务列表可读取；无 Shizuku 环境下的手动检查会记录 `SkippedMissingPrivilege`，历史卡可显示缺少权限、目标数、跳过数、失败原因和触发来源。

Shizuku 写入成功路径需要在已授权实体设备继续验证。本轮 AVD 覆盖了无权限兜底、UI 展示、历史落盘和 receiver 显式检查路径。

## 构建与测试

| 项目 | 命令 | 结果 |
| --- | --- | --- |
| feature unit tests | `./gradlew :feature-keepalive:testDebugUnitTest` | 通过 |
| app unit tests | `./gradlew :app:testDebugUnitTest` | 通过 |
| debug build | `./gradlew :feature-keepalive:testDebugUnitTest :app:assembleDebug` | 通过 |
| AVD install | `adb install -r app/build/outputs/apk/debug/app-debug.apk` | API 36 / API 37 均保留数据安装成功 |

## 本地 SDK 源码确认

本项目目标 SDK 37，平台行为优先参考本地 SDK sources。以下常量在 API 35、36.1、37.1 均能定位到：

| 常量 | API 37.1 源码位置 | API 36.1 源码位置 | API 35 源码位置 |
| --- | --- | --- | --- |
| `Settings.Secure.ACCESSIBILITY_ENABLED` | `android/provider/Settings.java:9091` | `android/provider/Settings.java:9024` | `android/provider/Settings.java:8451` |
| `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` | `android/provider/Settings.java:9242` | `android/provider/Settings.java:9155` | `android/provider/Settings.java:8569` |
| `Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE` | `android/content/pm/ServiceInfo.java:584` | `android/content/pm/ServiceInfo.java:555` | `android/content/pm/ServiceInfo.java:548` |
| `PackageManager.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` | `android/content/pm/PackageManager.java:271` | `android/content/pm/PackageManager.java:264` | `android/content/pm/PackageManager.java:219` |
| `AppOpsManager.OPSTR_BIND_ACCESSIBILITY_SERVICE` | `android/app/AppOpsManager.java:2454` | `android/app/AppOpsManager.java:2144` | `android/app/AppOpsManager.java:1989` |

相关确认点：

- `EnhancedConfirmationManager` 在 API 35 / 36.1 / 37.1 都把 `OPSTR_BIND_ACCESSIBILITY_SERVICE` 纳入 protected settings。
- `ForegroundServiceTypePolicy` 在 API 35 / 36.1 / 37.1 都包含 `FOREGROUND_SERVICE_SPECIAL_USE` 权限检查。
- `ServiceInfo` 文档在 API 35 / 36.1 / 37.1 都要求 specialUse FGS 搭配 service-level subtype property。

## AVD 设备

| 设备 | Android | SDK | Model | 截图 |
| --- | --- | --- | --- | --- |
| `emulator-5554` | 16 | 36 | `sdk_gphone64_arm64` | `build/qa/keepalive/api36-settings-keepalive-top.png` |
| `emulator-5556` | 17 | 37 | `sdk_gphone16k_arm64` | `build/qa/keepalive/api37-settings-keepalive-top.png` |

基础截图：

- `build/qa/keepalive/api36-home.png`
- `build/qa/keepalive/api37-home.png`
- `build/qa/keepalive/api36-settings-keepalive-top.png`
- `build/qa/keepalive/api37-settings-keepalive-top.png`

## UI 验收

| 区域 | API 36 结果 | API 37 结果 |
| --- | --- | --- |
| 设置页保活入口 | 底栏“保活”可进入 | 底栏“保活”可进入 |
| Android 后台状态 | 保活主卡可收纳显示 | Android 后台状态、后台调度恢复、电池优化、OEM 自启动可显示 |
| 无障碍守护策略 | 守护能力、前台守护、开机与更新后恢复、亮屏检查、说明、检查与导出可显示 | 同 API 36 |
| 无障碍服务列表 | 可读取 6 个服务，服务状态和系统标记可显示 | 可读取 6 个服务，服务状态和系统标记可显示 |
| 守护目标选择 | 开关可加入守护目标，目标计数更新 | 开关可加入守护目标，目标计数更新 |
| 守护历史 | 历史卡可展开，显示最新结果、失败原因和触发来源 | 历史卡可展开，显示最新结果、失败原因和触发来源 |

截图证据：

- `build/qa/keepalive/api36-settings-keepalive-services.png`
- `build/qa/keepalive/api36-settings-keepalive-service-list.png`
- `build/qa/keepalive/api36-settings-keepalive-service-selected.png`
- `build/qa/keepalive/api36-settings-keepalive-manual-check.png`
- `build/qa/keepalive/api36-settings-keepalive-history.png`
- `build/qa/keepalive/api37-settings-keepalive-services.png`
- `build/qa/keepalive/api37-settings-keepalive-service-list.png`
- `build/qa/keepalive/api37-settings-keepalive-service-selected.png`
- `build/qa/keepalive/api37-settings-keepalive-manual-check.png`
- `build/qa/keepalive/api37-settings-keepalive-history.png`
- `build/qa/keepalive/api37-settings-keepalive-history-detail.png`

## 手动检查与历史

AVD 未配置 Shizuku，安全设置读取命令返回不可用状态。选择 `无障碍菜单` 作为守护目标后，手动检查结果按预期写入本地历史。

API 36 历史文件：

`build/qa/keepalive/api36-accessibility-guard-history.jsonl`

关键字段：

```json
{
  "reason": "Manual",
  "status": "SkippedMissingPrivilege",
  "triggerAction": "settings_manual_check",
  "selectedCount": 1,
  "restoredCount": 0,
  "skippedCount": 1,
  "failureReason": "Shizuku service unavailable (start Shizuku app first)"
}
```

API 37 历史文件：

`build/qa/keepalive/api37-accessibility-guard-history.jsonl`

关键字段：

```json
{
  "reason": "Manual",
  "status": "SkippedMissingPrivilege",
  "triggerAction": "settings_manual_check",
  "selectedCount": 1,
  "restoredCount": 0,
  "skippedCount": 1,
  "failureReason": "Shizuku service unavailable (start Shizuku app first)"
}
```

API 37 额外验证了 receiver 显式检查：

```bash
adb -s emulator-5556 shell am broadcast \
  -n os.kei.debug/os.kei.feature.keepalive.receiver.AccessibilityGuardEventReceiver \
  -a os.kei.keepalive.action.CHECK_ACCESSIBILITY_GUARD
```

结果：

- `Broadcast completed: result=0`
- `accessibility-guard-history.jsonl` 从 1 条增长到 2 条。
- 新记录 `triggerAction = os.kei.keepalive.action.CHECK_ACCESSIBILITY_GUARD`。
- 未出现 `FATAL` 或 `Exception`。

## 系统设置读取

AVD 当前系统无障碍安全设置：

```text
enabled_accessibility_services = null
accessibility_enabled = 0
```

该状态与 UI 中“已启用 0 / 已选择 1 / 总计 6”和历史中的缺少权限结果一致。

## Receiver 与系统广播

模块 manifest 声明：

- `AccessibilityGuardForegroundService`
  - `foregroundServiceType="specialUse"`
  - `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE = accessibility_guard_keepalive`
- `AccessibilityGuardEventReceiver`
  - `BOOT_COMPLETED`
  - `MY_PACKAGE_REPLACED`

通过 shell 伪造 `MY_PACKAGE_REPLACED` 会被系统拒绝：

```text
java.lang.SecurityException: Permission Denial: not allowed to send broadcast android.intent.action.MY_PACKAGE_REPLACED from uid=2000
```

本轮使用 `adb install -r` 覆盖安装 debug 包，应用启动、设置页保活数据加载、后台恢复摘要均正常。`MY_PACKAGE_REPLACED` 的真实系统分发路径需要在后续安装升级场景继续观察历史记录。

## 单元测试覆盖

以下路径由 `feature-keepalive` 单元测试覆盖：

- `SkippedNoTargets`
- `SkippedMissingPrivilege`
- `Restored`
- `TimedOut`
- history append / latest / trim / export / decode
- secure settings bridge 成功读取、写入命令、timeout、permission denied 映射

AVD 本轮覆盖：

- 设置页 UI 渲染
- 服务列表读取
- 目标选择
- 无 Shizuku 缺少权限历史
- receiver 显式检查历史追加

实体 Shizuku 设备后续验证命令：

```bash
adb shell settings get secure enabled_accessibility_services
adb shell settings get secure accessibility_enabled
adb shell dumpsys activity services | grep -i AccessibilityGuard
adb logcat -d -s KeiOS AccessibilityGuard ShizukuApiUtils
```

## 风险与后续

| 风险 | 当前状态 | 建议 |
| --- | --- | --- |
| Shizuku 写入成功路径 | AVD 无授权环境，本轮未覆盖 | 使用用户实体机授权 Shizuku 后验证恢复成功和 cooldown |
| `MY_PACKAGE_REPLACED` / `BOOT_COMPLETED` | shell 无法伪造 protected broadcast，实际 install-r 无崩溃 | 下次实体机覆盖安装后查看历史是否追加 package replaced |
| FGS specialUse 审核语义 | 源码和 manifest 均已匹配 | 发布前检查 Play / 系统提示文案是否一致 |
| OEM 后台策略 | 原生 AVD 仅覆盖 AOSP 行为 | 后续接入无障碍 / Shizuku 保活时按 OEM 机型补充证据 |

本轮结论：P0 / P1 / P2 的 AOSP 原生基础链路已具备可验收证据，Shizuku 授权成功路径进入实体机专项验收。
