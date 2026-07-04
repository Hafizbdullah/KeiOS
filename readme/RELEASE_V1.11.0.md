# KeiOS v1.11.0 Release Notes

<!-- markdownlint-disable MD013 -->

## 中文

KeiOS v1.11.0 是 v1.10.0 之后的一次稳定性与数据安全更新，重点围绕 GitHub 刷新速度、通知 / Live Updates 会话、WebDAV 自动同步、多设备 BA 账号合并和 BA 咖啡厅冷却编辑做收束。

### GitHub / F-Droid 追踪刷新

- 全量刷新和增量刷新做了更明确的调度拆分，70+ 追踪项场景下会复用仓库元数据、APK profile 和可共享的资产解析结果。
- 手动刷新、自动刷新和后台刷新都会写入更完整的刷新历史，包括刷新目的、数量、耗时、慢项、失败原因和摘要。
- 刷新失败、超时或被取消时会结束刷新会话，通知、Live Updates 和超级岛状态可以正确收尾。
- 长列表中的已安装 App 图标加载链路完成修复，靠后的追踪项目滚动进入视野后也能显示真实图标。
- 慢项诊断在页面里展示更完整的原因，定位网络、仓库 API、APK 解析、缓存命中和本地安装信息读取会更直接。

### WebDAV 同步

- WebDAV 自动同步减少启动阶段的抢占式刷新和重复冲突提示，远端摘要会在同步前刷新并进入确认链路。
- 用户确认上传或同步后，链路会按最新远端状态继续执行，降低“已确认仍提示远端变动”的出现概率。
- BA 多账号同步支持按账号合并。设备 A 修改账号 1、设备 B 修改账号 2 后，第三台设备同步时可以保留两边的独立修改。
- WebDAV 新增历史板块，记录自动同步、手动同步、上传、跳过、失败、冲突恢复和合并结果，方便回溯数据变化。
- 数据页继续拆分长卡片，WebDAV 配置、同步状态、历史和维护操作更容易浏览。

### BA 办公室

- 咖啡厅摸摸头、邀请券 1、邀请券 2 支持长按编辑剩余冷却时间。
- 编辑 sheet 支持小时、分钟和秒，适合服务器时间、手动记录和多设备同步后的精确校准。
- 短按行为保持原样：摸摸头会消耗摸摸头动作，邀请券会消耗对应券。
- 摸摸头编辑会参考服务器学生刷新时间，避免设置出超出游戏机制的可用时间。

### 体验与性能

- GitHub 刷新 UI 派发减少重复状态更新，刷新中页面和历史页的响应更稳定。
- WebDAV 请求链路迁移到当前 Ktor dav4jvm API，自动同步和冲突恢复逻辑更清晰。
- BA 咖啡厅冷却 sheet 通过独立状态流承载，减少页面状态分支互相影响。
- 关于页、README、构建指南和发布文档同步更新到 v1.11.0。

### 安装信息

- 包名：`os.kei`
- ABI：`arm64-v8a`
- Android：Android 15+（`minSdk 35`）
- Target SDK：Android 17 / API 37
- APK：`KeiOS_1.11.0.apk`

### 升级建议

建议所有 v1.10.x 用户升级到 v1.11.0。经常使用 GitHub/F-Droid 追踪、WebDAV 自动同步、BA 多账号、咖啡厅提醒、Live Updates / 超级岛刷新提示的用户会获得最明显改善。

## English

KeiOS v1.11.0 is a stability and data-safety update after v1.10.0. It focuses on GitHub refresh speed, notification / Live Updates sessions, WebDAV auto sync, multi-device BA account merging, and BA cafe cooldown calibration.

### GitHub / F-Droid Refresh

- Full refresh and incremental refresh now have clearer scheduling paths, with shared repository metadata, APK profiles, and reusable asset parsing for 70+ tracked items.
- Manual refreshes, automatic refreshes, and background refreshes write richer history records with purpose, item count, duration, slow items, failure reason, and summary.
- Failed, timed-out, or cancelled refreshes now close their refresh sessions so notifications, Live Updates, and Super Island states can finish correctly.
- Installed app icons now keep loading in long tracking lists, including items that appear only after scrolling far down.
- Slow-item diagnostics show more complete reasons for network calls, repository APIs, APK parsing, cache hits, and local installed-app reads.

### WebDAV Sync

- WebDAV auto sync reduces launch-time refresh pressure and repeated conflict prompts. Remote summaries are refreshed before the confirmation flow.
- Confirmed upload or sync actions continue against the latest known remote state, reducing repeated “remote changed” prompts after a valid confirmation.
- BA multi-account sync now merges by account. When device A edits account 1 and device B edits account 2, a third device can sync and keep both independent changes.
- WebDAV has a new history section for auto sync, manual sync, upload, skip, failure, conflict recovery, and merge results.
- The data page splits long WebDAV cards into smaller sections for configuration, sync status, history, and maintenance actions.

### BA Office

- Cafe headpat, invitation ticket 1, and invitation ticket 2 now support long-press remaining cooldown edits.
- The edit sheet supports hours, minutes, and seconds for server-time correction, manual records, and multi-device calibration.
- Short taps keep their original actions: headpat consumes the headpat action, and invitation tickets consume the selected ticket.
- Headpat editing follows the server student refresh window so the result stays within the game mechanism.

### Experience And Performance

- GitHub refresh UI dispatch now avoids repeated state updates, keeping refresh pages and history pages more responsive.
- WebDAV requests migrated to the current Ktor dav4jvm API path, making auto sync and conflict recovery easier to maintain.
- BA cafe cooldown sheets use a focused route state, reducing cross-sheet state interference.
- About, README, build guide, and release documentation are aligned with v1.11.0.

### Package

- Package name: `os.kei`
- ABI: `arm64-v8a`
- Android: Android 15+ (`minSdk 35`)
- Target SDK: Android 17 / API 37
- APK: `KeiOS_1.11.0.apk`

### Upgrade Advice

All v1.10.x users should upgrade to v1.11.0. The biggest gains are for users who rely on GitHub/F-Droid tracking, WebDAV auto sync, BA multi-account data, cafe reminders, and Live Updates / Super Island refresh progress.
