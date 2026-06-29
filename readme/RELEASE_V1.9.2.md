# KeiOS v1.9.2 Release Notes

<!-- markdownlint-disable MD013 -->

## 中文

KeiOS v1.9.2 面向 v1.9.x 用户带来 F-Droid 仓库追踪、同步数据结构升级、Miuix Nav 迁移收尾、MCP 页面稳定性修复，以及构建链路更新。这个版本适合正在使用 GitHub/Git 追踪、WebDAV 同步、MCP/Claw 和第三方 Android 应用更新管理的用户升级。

### F-Droid 仓库追踪

- 新增 F-Droid 来源模式。GitHub 页面现在可以把 F-Droid 仓库作为一类独立追踪来源使用。
- 内置官方 F-Droid 与 IzzyOnDroid 仓库预设，也支持自定义仓库链接。
- 新增按应用名称搜索和包名搜索。用户可以通过常见应用名找到候选包，减少只知道包名才能添加的门槛。
- 新增已安装应用反扫。可以从本机已安装应用出发，在多个 F-Droid 仓库里查找可追踪来源。
- 版本判断使用 F-Droid index 的 versionCode / versionName 数据，并优先使用推荐版本，随后回落到最高兼容版本。
- 详情 sheet 展示仓库、包名、版本、APK、哈希、信任状态、Anti-Features、发行说明等 F-Droid 专属信息。
- 追踪卡片和更多菜单会按来源展示合适动作，F-Droid 来源不会混入 GitHub Actions 专属入口。

### 同步、导入导出与 WebDAV

- 导入导出格式升级到 v4，保留向旧版本数据的兼容读取。
- WebDAV 同步理解 F-Droid 来源数据，可以同步 F-Droid 追踪项、仓库信息、筛选配置和来源摘要。
- 导入预览和 WebDAV 变更预览会展示 F-Droid 条目数、仓库数量、官方仓库数量和信任未知数量。
- MCP tools 的追踪查询支持 `fdroid`、`fdroid_repository`、`izzy`、`izzyondroid` 等来源别名。

### 导航、MCP 与稳定性

- 主导航路径迁移到 Miuix Nav，预测式返回和页面栈作用域更贴近 Miuix sample 的结构。
- MCP 页面适配 Miuix Nav entry 级 ViewModel 作用域，修复打开 MCP 页面时因为 SavedState extras 缺失导致的崩溃。
- MCP 页面保留卡片展开状态、日志状态和 Claw 接入资源，同时避免依赖导航栈未提供的 SavedStateRegistry extras。

### 性能、调度与构建

- F-Droid 刷新按仓库合并请求，同一仓库多个追踪项共享一次仓库快照，再分发到各个包。
- GitHub/F-Droid/Direct APK 的刷新 lane 和并发上限继续收敛，减少高频刷新对 UI 与后台任务的挤压。
- Gradle Wrapper 更新到 9.6.1，Android Gradle Plugin 更新到 9.3.0-rc01，Ktor 更新到 3.5.1。
- Miuix 依赖保持 snapshot 路径，导航、预测式返回和页面动画继续跟进上游实现。
- Gradle 10 兼容警告完成 P0 级收口，后续迁移清单会继续推进。

### 验证

- F-Droid 相关单元测试覆盖模型、仓库 API、index 解析、候选版本选择、缓存 sidecar、导入导出、WebDAV 合并和 MCP 来源过滤。
- Debug 编译、Release/R8 构建、资源优化、lintVital 和签名链路完成验证。
- AVD 覆盖启动、GitHub 页面入口、新增追踪 sheet、F-Droid 来源字段、F-Droid 详情 sheet 和 MCP 页面打开。
- App 崩溃日志使用 `os.kei.debug` 精准过滤，MCP 页面崩溃链路已修复并回归验证。

### 安装信息

- 包名：`os.kei`
- ABI：`arm64-v8a`
- Android：Android 15+（`minSdk 35`）
- APK：`KeiOS_1.9.2.apk`

### 升级建议

建议所有 v1.9.x 用户升级到 v1.9.2。经常追踪 GitHub/Git/F-Droid 应用、使用 IzzyOnDroid、依赖 WebDAV 备份同步、使用 MCP/Claw 或关注 Miuix Nav 预测式返回体验的用户优先升级。

## English

KeiOS v1.9.2 is an update for v1.9.x users with F-Droid repository tracking, sync schema upgrades, Miuix Nav migration cleanup, MCP page stability fixes, and build-chain updates. It is useful for users who rely on GitHub/Git tracking, WebDAV sync, MCP/Claw, and third-party Android app update tracking.

### F-Droid Repository Tracking

- F-Droid is now a first-class source mode on the GitHub tracking page.
- Official F-Droid and IzzyOnDroid presets are built in, and custom repository URLs are supported.
- App-name search and package-name search are available, making it easier to add apps when the package name is unknown.
- Installed-app reverse scan can search across multiple F-Droid repositories from apps already installed on the device.
- Version checks use F-Droid index versionCode / versionName data, preferring the suggested version and then falling back to the highest compatible version.
- The detail sheet shows F-Droid-specific repository, package, version, APK, hash, trust, Anti-Feature, and release-note information.
- Tracked cards and more menus now show source-appropriate actions. F-Droid tracks keep GitHub Actions-only entries out of the menu.

### Sync, Import/Export, And WebDAV

- Import/export data moves to schema v4 while keeping compatibility with older tracked-item data.
- WebDAV sync understands F-Droid source data, including tracked items, repository metadata, filter options, and source summaries.
- Import previews and WebDAV change previews show F-Droid item count, repository count, official repository count, and unknown-trust count.
- MCP tracking tools support `fdroid`, `fdroid_repository`, `izzy`, and `izzyondroid` source aliases.

### Navigation, MCP, And Stability

- Main navigation moved to the Miuix Nav path, bringing the app closer to the current Miuix sample structure for predictive back and page-stack scoping.
- The MCP page now matches Miuix Nav entry-scoped ViewModel lifetime and opens reliably when SavedState extras are unavailable.
- MCP card expansion state, log state, and Claw resource actions remain available without depending on SavedStateRegistry extras from the navigation entry.

### Performance, Scheduling, And Build

- F-Droid refreshes are grouped by repository. Multiple tracks sharing one repository reuse one repository snapshot before fan-out to packages.
- GitHub, F-Droid, and Direct APK refresh lanes have tighter concurrency limits to reduce pressure on UI and background work.
- Gradle Wrapper moves to 9.6.1, Android Gradle Plugin moves to 9.3.0-rc01, and Ktor moves to 3.5.1.
- Miuix stays on the snapshot path so navigation, predictive back, and page animation improvements can follow upstream quickly.
- P0 Gradle 10 compatibility warnings are cleaned up, with the remaining migration list ready for later work.

### Verification

- F-Droid unit coverage includes models, repository API, index parsing, candidate selection, cache sidecars, import/export, WebDAV merge, and MCP source filtering.
- Debug compile, Release/R8 build, resource optimization, lintVital, and signing paths were verified.
- AVD coverage includes startup, GitHub page entry, add-track sheet, F-Droid fields, F-Droid detail sheet, and MCP page opening.
- App crash logs were filtered precisely for `os.kei.debug`; the MCP page crash path is fixed and regression-tested.

### Package

- Package name: `os.kei`
- ABI: `arm64-v8a`
- Android: Android 15+ (`minSdk 35`)
- APK: `KeiOS_1.9.2.apk`

### Upgrade Advice

All v1.9.x users should upgrade to v1.9.2. This release is especially useful for GitHub/Git/F-Droid tracking, IzzyOnDroid users, WebDAV backup/sync users, MCP/Claw users, and anyone following the Miuix Nav predictive-back improvements.
