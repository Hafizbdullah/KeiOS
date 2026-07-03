# KeiOS v1.10.0 Release Notes

<!-- markdownlint-disable MD013 -->

## 中文

KeiOS v1.10.0 是面向 v1.9.x 用户的一次大版本收束。这个版本把 v1.9.0 之后陆续完成的 F-Droid 来源追踪、历史中心、WebDAV 数据安全、BA 图鉴长期缓存、记忆大厅视频 PiP、Miuix Nav、MCP 服务端性能和构建链路更新整合为一个新的稳定基线。

### GitHub / F-Droid 追踪

- F-Droid 成为 GitHub 页面的一等追踪来源，支持官方 F-Droid、IzzyOnDroid、自定义仓库和常用源管理。
- 新增应用名称搜索、包名搜索和已安装应用反扫。用户知道应用名或已经安装了 App 时，也可以更容易找到可追踪来源。
- F-Droid 详情 sheet 展示仓库、包名、versionCode / versionName、APK、哈希、信任状态、Anti-Features 和发行说明。
- F-Droid 仓库 index 会按仓库缓存并复用，同仓库多个追踪项共享刷新结果，减少流量和后台压力。
- GitHub / Git / Direct APK / F-Droid 刷新调度继续收敛，失败项会正确结束刷新会话，并支持后续重试。
- 刷新按钮、历史入口、BA 活动/卡池入口增加未读角标，让用户更快看到近期变化。

### 历史中心

- Actions 历史升级为历史页，底栏可切换 Actions 历史、刷新历史、追踪历史和 App 历史。
- 刷新历史记录用户主动刷新、自动刷新、后台刷新、成功、失败、超时和摘要，方便排查长期卡在刷新中的问题。
- 追踪历史记录追踪项目的新增、修改和删除。
- App 历史记录已追踪 App 的安装、更新、卸载和降级，并尽量保存安装来源、安装器、版本号和系统可见信息。
- 历史页支持搜索、过滤、导出、未读计数和 MCP 查询，诊断数据保留在本机，不参与 WebDAV 同步。

### WebDAV 与数据安全

- WebDAV 同步会先刷新远端，再展示变更计划。同步或上传前可以看到本地与远端分别会新增、更新、合并或保留哪些数据。
- WebDAV 数据结构支持 F-Droid v4 追踪项、仓库元数据、来源摘要和导入导出预览。
- BA 多账号数据接入 WebDAV 合并，适合同时玩多个服务器或多个同服账号的用户跨设备迁移。
- 自动同步增加远端状态保护，减少使用旧摘要覆盖远端数据的风险。
- 导入导出继续兼容旧数据，并为新追踪源和多账号数据保留迁移入口。

### BA 图鉴与记忆大厅

- 实装学生详情加入长期缓存模型。新实装短期内保持更积极刷新，稳定后优先读取本地缓存，减少重复下载并支持离线查看。
- 学生与 NPC/卫星合并为一个“学生”板块，通过菜单切换类型并记忆用户选择。
- 新增“记忆大厅”板块。学生卡支持收藏、搜索、展开查看、收藏隐藏/显示和顶部紧凑统计。
- 记忆大厅视频播放接入独立 `ui-pip` 模块，支持 PiP 播放、循环记忆、暂停/继续、快退/快进 10 秒、全屏和关闭。
- 学生图鉴、记忆大厅和音乐板块的收藏联动更贴近实际使用场景，已收藏学生可以快速收纳。
- AP 与咖啡厅卡片重新整理信息密度，AP 回满、咖啡厅产出、活动日历和卡池变更提醒更清晰。

### 界面、导航与体验

- 主导航迁移到 Miuix Nav，页面栈、预测式返回和底栏手势链路更接近当前 Miuix 实现。
- 底栏、ActionBar、竖向 dock、搜索 dock、角标和拖动手势做了统一修正，减少切换时的二次位移和误触返回。
- 多个页面支持自定义背景图、裁切、透明度、模糊/景深、颜色强度和动态深度参数。
- Sheet 链路统一按当前设置选择 Miuix sheet 或 Liquid sheet，减少混用导致的透明、阴影和圆角异常。
- Home、GitHub、WebDAV、BA、MCP、Actions/历史、OS Shell Runner、反馈等页面继续统一 action bar、底栏和卡片节奏。

### MCP、性能与构建

- MCP 服务端进一步拆分到 feature/module 边界，常驻后台时减少重复资源读取、日志抖动和不必要的状态发布。
- 图鉴、F-Droid 搜索/刷新、PiP、刷新历史、MCP 服务端和页面预热链路做了缓存复用与异步调度优化。
- CI Actions workflow、测试用例和 GitHub 历史相关覆盖已跟进近期重构。
- 依赖和构建基线更新到 Gradle Wrapper 9.6.1、Kotlin 2.4.0、Compose 1.11.4、Ktor 3.5.1、Media3 1.10.1、Coil 3.5.0，并继续跟进 Miuix snapshot。
- 文档结构整理到 `docs/`，根目录更适合普通用户和贡献者浏览。

### 安装信息

- 包名：`os.kei`
- ABI：`arm64-v8a`
- Android：Android 15+（`minSdk 35`）
- Target SDK：Android 17 / API 37
- APK：`KeiOS_1.10.0.apk`

### 升级建议

建议所有 v1.9.x 用户升级到 v1.10.0。经常使用 GitHub/F-Droid 追踪、WebDAV 同步、BA 多账号与图鉴、记忆大厅视频、MCP/Claw、Actions artifact 和自定义背景的用户会获得最明显的体验变化。

## English

KeiOS v1.10.0 is a new stable baseline for v1.9.x users. It consolidates the work since v1.9.0 into first-class F-Droid tracking, History Hub, safer WebDAV data handling, long-lived BA guide cache, Memorial Lobby video PiP, Miuix Nav, MCP server performance work, and build-chain updates.

### GitHub / F-Droid Tracking

- F-Droid is now a first-class tracking source on the GitHub page, with official F-Droid, IzzyOnDroid, custom repositories, and common-source management.
- App-name search, package-name search, and installed-app reverse scan make it easier to add tracks when only the app name or installed app is known.
- F-Droid detail sheets show repository, package, versionCode / versionName, APK, hash, trust state, Anti-Features, and release notes.
- Repository indexes are cached and reused by repository, so multiple tracks from the same source share refresh work and reduce bandwidth.
- GitHub / Git / Direct APK / F-Droid refresh scheduling was tightened, failed refresh sessions now finish correctly, and failed items can retry cleanly.
- Refresh, History, BA calendar, and BA pool entries now use unread badges so recent changes are easier to notice.

### History Hub

- Actions History is now a History page with bottom tabs for Actions, Refresh, Tracking, and App history.
- Refresh history records manual refreshes, automatic refreshes, background refreshes, success, failure, timeout, and summaries for diagnosing stuck refreshes.
- Tracking history records added, edited, and deleted tracked items.
- App history records installs, updates, uninstalls, and downgrades for tracked apps, including installer/source, version, and other system-visible details when available.
- History supports search, filtering, export, unread counts, and MCP queries. Diagnostic history stays local and is excluded from WebDAV sync.

### WebDAV And Data Safety

- WebDAV sync refreshes the remote side before showing a change plan. Before sync or upload, users can see what will be added, updated, merged, or kept.
- WebDAV data now covers F-Droid v4 tracked items, repository metadata, source summaries, and import/export previews.
- BA multi-account data participates in WebDAV merge, helping users migrate multiple servers or multiple same-server accounts across devices.
- Auto sync has stronger remote-state protection to reduce stale-summary overwrite risk.
- Import/export remains compatible with older data while preserving migration paths for new source and account data.

### BA Guide And Memorial Lobby

- Implemented student detail pages now use a long-lived cache model. Newly implemented students refresh more aggressively for a short period, while stable entries prefer local cache and can be viewed offline.
- Student and NPC/Satellite sections are merged into one Student tab, with remembered type selection in the menu.
- The new Memorial Lobby tab provides student cards with favorites, search, expanded previews, favorite hide/show, and compact top metrics.
- Memorial Lobby videos use the reusable `ui-pip` module, with PiP playback, remembered loop mode, pause/resume, 10-second seek back/forward, fullscreen, and close actions.
- Favorites are coordinated across Student Guide, Memorial Lobby, Music, and playback in a way that better matches daily use.
- AP and cafe cards have denser status pills, clearer AP-full timing, cafe production, and calendar/pool change reminders.

### UI, Navigation, And Experience

- Main navigation moved to Miuix Nav, aligning page stacks, predictive back, and bottom-bar gesture behavior with the current Miuix path.
- Bottom bars, ActionBars, vertical docks, search docks, badges, and drag gestures received shared fixes for settle motion and back-gesture conflicts.
- More pages support custom background images, crop, opacity, blur/depth, color intensity, and dynamic depth.
- Sheet selection now follows the current Miuix/Liquid setting consistently, reducing mixed-mode transparency, shadow, and corner issues.
- Home, GitHub, WebDAV, BA, MCP, Actions/History, OS Shell Runner, and Feedback continue to share more consistent action bars, bottom chrome, and card rhythm.

### MCP, Performance, And Build

- MCP server-side code is split across clearer feature/module boundaries and reduces repeated resource reads, log churn, and unnecessary state publishing while running in the background.
- Catalog, F-Droid search/refresh, PiP, refresh history, MCP server, and prewarm paths received cache reuse and async scheduling work.
- CI Actions workflow versions, regression tests, and GitHub history coverage were refreshed after the recent refactors.
- Build baselines moved to Gradle Wrapper 9.6.1, Kotlin 2.4.0, Compose 1.11.4, Ktor 3.5.1, Media3 1.10.1, Coil 3.5.0, with Miuix kept on the snapshot update path.
- Project documentation is organized under `docs/`, keeping the repository root easier for users and contributors to scan.

### Package

- Package name: `os.kei`
- ABI: `arm64-v8a`
- Android: Android 15+ (`minSdk 35`)
- Target SDK: Android 17 / API 37
- APK: `KeiOS_1.10.0.apk`

### Upgrade Advice

All v1.9.x users should upgrade to v1.10.0. The biggest gains are for users who rely on GitHub/F-Droid tracking, WebDAV sync, BA multi-account and guide features, Memorial Lobby videos, MCP/Claw, Actions artifacts, and custom backgrounds.
